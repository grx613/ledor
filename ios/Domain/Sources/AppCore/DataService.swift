import Domain
import Foundation

public enum DataError: Error, Sendable { case listMissing, readOnly }

public enum JoinOutcome: Sendable { case joined(ListRecord, imported: Int) }

public struct SyncReport: Sendable { public let ok: Int; public let failed: Int; public let networkError: Bool }

/// Работа с событиями и списками — как `EventRepository`, `ListRepository` и `SyncEngine` на Android.
/// `onChanged` вызывается после правок: перестроить уведомления и (для облачного списка) отправить изменения.
public final class DataService: Sendable {
    public let store: LocalStore
    public let rpc: RpcClient
    private let sync: SyncSerializer
    private let onChanged: @Sendable (_ cloudListId: String?) -> Void

    public init(store: LocalStore, rpc: RpcClient, onChanged: @escaping @Sendable (String?) -> Void = { _ in }) {
        self.store = store
        self.rpc = rpc
        self.onChanged = onChanged
        sync = SyncSerializer()
    }

    public var cloudConfigured: Bool { rpc.isConfigured }

    // MARK: - События

    public func save(_ event: Event) async throws {
        let movedFrom: String? = try await store.mutate { db in
            guard let list = db.list(event.listId) else { throw DataError.listMissing }
            guard list.canEdit else { throw DataError.readOnly }
            let state: SyncState = list.isCloud ? .pendingUpsert : .synced
            let existing = db.event(event.id)
            if let existing, existing.listId != event.listId {
                // Перенос в другой список = удаление в старом + создание с новым id.
                Self.deleteInternal(existing, &db)
                var moved = event
                moved.id = UUID().uuidString.lowercased()
                db.upsert(EventRecord(moved, syncState: state))
                return existing.listId
            }
            db.upsert(EventRecord(event, syncState: state, rev: existing?.rev ?? 0))
            return nil
        }
        if let movedFrom { await notify(movedFrom) }
        await notify(event.listId)
    }

    public func delete(eventId: String) async {
        let listId: String? = await store.mutate { db in
            guard let e = db.event(eventId) else { return nil }
            Self.deleteInternal(e, &db)
            return e.listId
        }
        if let listId { await notify(listId) }
    }

    private static func deleteInternal(_ e: EventRecord, _ db: inout DatabaseState) {
        let list = db.list(e.listId)
        if list == nil || list?.isCloud == false || (e.rev == 0 && e.syncState == .pendingUpsert) {
            db.deleteEvent(e.id)
        } else {
            var m = e
            m.syncState = .pendingDelete
            m.updatedAt = Date()
            db.upsert(m)
        }
    }

    private func notify(_ listId: String) async {
        let list = await store.state.list(listId)
        onChanged(list?.isCloud == true ? listId : nil)
    }

    // MARK: - Списки

    public func checkExists(name: String) async -> RpcResult<Bool> { await rpc.listExists(name: ListNameRules.clean(name)) }

    public func create(name: String, readPin: String, masterPin: String, moveLocalEvents: Bool) async -> RpcResult<ListRecord> {
        let r = await rpc.createList(name: ListNameRules.clean(name), readPin: readPin, masterPin: masterPin)
        guard case .ok(let resp) = r else { return r.map { _ in ListRecord.personal } }
        let list = await saveJoined(resp, masterPin: masterPin)
        if moveLocalEvents {
            await store.mutate { db in Self.move(from: localListId, to: list, &db) }
            _ = await syncList(list.id)
        }
        onChanged(nil)
        return .ok(list)
    }

    public func join(name: String, pin: String) async -> RpcResult<JoinOutcome> {
        let r = await rpc.joinList(name: ListNameRules.clean(name), pin: pin)
        guard case .ok(let resp) = r else { return r.map { _ in .joined(.personal, imported: 0) } }
        let list = await saveJoined(resp, masterPin: resp.role == ListRole.editor.rawValue ? pin : nil)
        _ = await syncList(list.id, full: true)
        onChanged(nil)
        let state = await store.state
        return .ok(.joined(state.list(list.id) ?? list, imported: state.count(in: list.id)))
    }

    private func saveJoined(_ resp: JoinResponse, masterPin: String?) async -> ListRecord {
        await store.mutate { db in
            let existing = db.list(resp.list.id)
            var list = existing ?? ListRecord(id: resp.list.id, kind: .cloud, name: resp.list.name, role: .reader,
                                             colorIndex: db.lists.count % 8,
                                             sortOrder: (db.lists.map(\.sortOrder).max() ?? 0) + 1)
            list.name = resp.list.name
            list.role = ListRole(rawValue: resp.role) ?? .reader
            list.state = .active
            list.accessToken = resp.token
            list.readPin = resp.readPin ?? existing?.readPin
            list.masterPin = masterPin ?? existing?.masterPin
            db.upsert(list)
            return list
        }
    }

    public func disconnect(listId: String) async {
        guard let list = await store.state.list(listId), list.isCloud else { return }
        if let token = list.accessToken { _ = await rpc.leaveList(listId: listId, token: token) }
        await store.mutate { $0.deleteList(listId) }
        onChanged(nil)
    }

    public func deleteFromCloud(listId: String, masterPin: String) async -> RpcResult<Bool> {
        guard let list = await store.state.list(listId) else { return .ok(true) }
        guard let token = list.accessToken else { return .error(code: "UNAUTHORIZED") }
        let r = await rpc.deleteList(listId: listId, token: token, masterPin: masterPin)
        var gone = false
        if case .ok = r { gone = true }
        if case .error(let code, _, _) = r, code == "LIST_DELETED" { gone = true }
        guard gone else { return r }
        await store.mutate { $0.deleteList(listId) }
        onChanged(nil)
        return .ok(true)
    }

    public func setNotifyEnabled(listId: String, _ enabled: Bool) async {
        await store.mutate { db in
            guard var l = db.list(listId) else { return }
            l.notifyEnabled = enabled
            db.upsert(l)
        }
        onChanged(nil)
    }

    public func moveLocalEvents(to targetId: String) async {
        let moved: Bool = await store.mutate { db in
            guard let target = db.list(targetId), target.canEdit else { return false }
            Self.move(from: localListId, to: target, &db)
            return true
        }
        if moved { onChanged(targetId) }
    }

    private static func move(from sourceId: String, to target: ListRecord, _ db: inout DatabaseState) {
        for e in db.visible(in: sourceId) {
            db.deleteEvent(e.id)
            var m = e
            m.id = UUID().uuidString.lowercased()
            m.fields.id = m.id
            m.listId = target.id
            m.rev = 0
            m.syncState = target.isCloud ? .pendingUpsert : .synced
            m.updatedAt = Date()
            db.upsert(m)
        }
    }

    public func clearNotices() async { await store.mutate { $0.deletedListNotices = [] } }

    // MARK: - Синхронизация (`04-backend.md` §5)

    public func syncAll() async -> SyncReport {
        await sync.run { [self] in
            var ok = 0, failed = 0, net = false
            for l in await store.state.lists where l.isCloud {
                switch await syncInternal(l.id, full: false) {
                case .ok: ok += 1
                case .network: failed += 1; net = true
                case .other: failed += 1
                }
            }
            await store.mutate { $0.lastSyncAttempt = Date() }
            onChanged(nil)
            return SyncReport(ok: ok, failed: failed, networkError: net)
        }
    }

    public func syncList(_ listId: String, full: Bool = false) async -> SyncReport {
        await sync.run { [self] in
            let r = await syncInternal(listId, full: full)
            onChanged(nil)
            return SyncReport(ok: r == .ok ? 1 : 0, failed: r == .ok ? 0 : 1, networkError: r == .network)
        }
    }

    private enum Outcome { case ok, network, other }

    private func syncInternal(_ listId: String, full: Bool) async -> Outcome {
        guard rpc.isConfigured, let list = await store.state.list(listId), let token = list.accessToken,
              list.state != .accessLost else { return .other }

        // 1. Отправка
        if list.role == .editor {
            let pending = await store.state.pending(in: listId)
            for start in stride(from: 0, to: pending.count, by: 200) {
                let chunk = Array(pending[start..<min(start + 200, pending.count)])
                switch await rpc.pushEvents(listId: listId, token: token, events: chunk.map(\.remote)) {
                case .ok(let resp):
                    let accepted = Set(resp.accepted)
                    let rejected = Set(resp.rejected.map(\.id))
                    await store.mutate { db in
                        for e in chunk where accepted.contains(e.id) || rejected.contains(e.id) {
                            if e.syncState == .pendingDelete || !accepted.contains(e.id) {
                                db.deleteEvent(e.id)
                            } else {
                                var m = e
                                m.syncState = .synced
                                m.rev = resp.revision
                                m.fields.rev = resp.revision
                                db.upsert(m)
                            }
                        }
                    }
                case .error(let code, _, _): return await handleError(list, code)
                case .failure(let kind): return kind == .network ? .network : .other
                }
            }
        }

        // 2. Получение
        let since = full ? 0 : list.lastSyncedRev
        switch await rpc.pullChanges(listId: listId, token: token, since: since) {
        case .ok(let resp):
            await store.mutate { db in
                if since == 0 { db.events.removeAll { $0.listId == listId && $0.syncState == .synced } }
                let pendingIds = Set(db.pending(in: listId).map(\.id))
                for re in resp.events where !pendingIds.contains(re.id) {
                    if re.deleted { db.deleteEvent(re.id) } else { db.upsert(EventRecord(remote: re, listId: listId)) }
                }
                guard var l = db.list(listId) else { return }
                l.name = resp.list.name
                l.role = ListRole(rawValue: resp.role) ?? l.role
                l.readPin = resp.readPin ?? l.readPin
                l.lastSyncedRev = resp.list.revision
                l.lastSyncAt = Date()
                db.upsert(l)
            }
            return .ok
        case .error(let code, _, _): return await handleError(list, code)
        case .failure(let kind): return kind == .network ? .network : .other
        }
    }

    private func handleError(_ list: ListRecord, _ code: String) async -> Outcome {
        await store.mutate { db in
            switch code {
            case "LIST_DELETED":
                db.deleteList(list.id)
                db.deletedListNotices.append(list.name)
            case "UNAUTHORIZED":
                guard var l = db.list(list.id) else { return }
                l.state = .accessLost
                db.upsert(l)
            case "FORBIDDEN":
                db.events.removeAll { $0.listId == list.id && $0.syncState != .synced }
                guard var l = db.list(list.id) else { return }
                l.role = .reader
                l.lastSyncedRev = 0
                db.upsert(l)
            default: break
            }
        }
        return .other
    }
}

/// Синхронизации идут строго по одной (как `Mutex` на Android).
actor SyncSerializer {
    private var tail: Task<Void, Never>?

    func run<T: Sendable>(_ op: @escaping @Sendable () async -> T) async -> T {
        let previous = tail
        let task = Task { _ = await previous?.value; return await op() }
        tail = Task { _ = await task.value }
        return await task.value
    }
}
