import Foundation

/// Всё, что хранится на телефоне: списки, события, сообщения об удалённых списках.
public struct DatabaseState: Codable, Sendable, Equatable {
    public var lists: [ListRecord] = [ListRecord.personal]
    public var events: [EventRecord] = []
    /// Названия общих списков, удалённых владельцем, — показываются пользователю один раз.
    public var deletedListNotices: [String] = []
    /// Названия списков, где сменили Мастер-ПИН и этот телефон стал читателем (B-40), — тоже один раз.
    public var demotedListNotices: [String] = []
    public var lastSyncAttempt: Date?

    public init() {}

    /// Поля, которых нет в файле старой версии, берутся по умолчанию — файл не теряется при обновлении приложения.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        lists = try c.decodeIfPresent([ListRecord].self, forKey: .lists) ?? [ListRecord.personal]
        events = try c.decodeIfPresent([EventRecord].self, forKey: .events) ?? []
        deletedListNotices = try c.decodeIfPresent([String].self, forKey: .deletedListNotices) ?? []
        demotedListNotices = try c.decodeIfPresent([String].self, forKey: .demotedListNotices) ?? []
        lastSyncAttempt = try c.decodeIfPresent(Date.self, forKey: .lastSyncAttempt)
    }

    public var sortedLists: [ListRecord] { lists.sorted { ($0.sortOrder, $0.createdAt) < ($1.sortOrder, $1.createdAt) } }
    public func list(_ id: String) -> ListRecord? { lists.first { $0.id == id } }
    public func event(_ id: String) -> EventRecord? { events.first { $0.id == id } }
    public var visibleEvents: [EventRecord] { events.filter { $0.syncState != .pendingDelete } }
    public func visible(in listId: String) -> [EventRecord] { visibleEvents.filter { $0.listId == listId } }
    public func pending(in listId: String) -> [EventRecord] { events.filter { $0.listId == listId && $0.syncState != .synced } }
    public func count(in listId: String) -> Int { visible(in: listId).count }

    public mutating func upsert(_ list: ListRecord) {
        if let i = lists.firstIndex(where: { $0.id == list.id }) { lists[i] = list } else { lists.append(list) }
    }

    public mutating func upsert(_ event: EventRecord) {
        if let i = events.firstIndex(where: { $0.id == event.id }) { events[i] = event } else { events.append(event) }
    }

    /// Удаление списка удаляет и его события (как ON DELETE CASCADE).
    public mutating func deleteList(_ id: String) {
        lists.removeAll { $0.id == id }
        events.removeAll { $0.listId == id }
    }

    public mutating func deleteEvent(_ id: String) { events.removeAll { $0.id == id } }
}

/// Хранилище — один JSON-файл в Application Support (данных немного: сотни событий). Без файла — только в памяти (тесты).
public actor LocalStore {
    public private(set) var state: DatabaseState
    private let fileURL: URL?
    private var observers: [UUID: AsyncStream<DatabaseState>.Continuation] = [:]

    public init(fileURL: URL?) {
        self.fileURL = fileURL
        if let url = fileURL, let data = try? Data(contentsOf: url),
           let saved = try? JSONDecoder().decode(DatabaseState.self, from: data) {
            state = saved
            if saved.list(localListId) == nil { state.lists.insert(.personal, at: 0) }
        } else {
            state = DatabaseState()
        }
    }

    public static var defaultURL: URL {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        return dir.appendingPathComponent("ledor.json")
    }

    /// Изменение — атомарно: целиком под актором, затем запись файла и оповещение наблюдателей.
    @discardableResult
    public func mutate<T: Sendable>(_ body: @Sendable (inout DatabaseState) throws -> T) rethrows -> T {
        var copy = state
        let result = try body(&copy)
        if copy != state {
            state = copy
            persist()
            for o in observers.values { o.yield(state) }
        }
        return result
    }

    /// Поток состояний: текущее сразу, затем каждое изменение.
    public func changes() -> AsyncStream<DatabaseState> {
        let id = UUID()
        let (stream, continuation) = AsyncStream<DatabaseState>.makeStream(bufferingPolicy: .bufferingNewest(1))
        continuation.yield(state)
        observers[id] = continuation
        continuation.onTermination = { [weak self] _ in Task { await self?.removeObserver(id) } }
        return stream
    }

    private func removeObserver(_ id: UUID) { observers[id] = nil }

    private func persist() {
        guard let url = fileURL, let data = try? JSONEncoder().encode(state) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }
}
