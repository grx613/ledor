import Domain
import Foundation
import Testing
@testable import AppCore

/// Поддельный сервер: отвечает по имени функции; запоминает запросы.
final class FakeServer: @unchecked Sendable {
    var handlers: [String: ([String: Any]) -> Any] = [:]
    var calls: [(String, [String: Any])] = []
    var offline = false

    var transport: HTTPTransport {
        { [self] request in
            if offline { throw URLError(.notConnectedToInternet) }
            let fn = request.url!.lastPathComponent
            let body = (try? JSONSerialization.jsonObject(with: request.httpBody!)) as? [String: Any] ?? [:]
            calls.append((fn, body))
            #expect(request.value(forHTTPHeaderField: "apikey") == "key")
            let reply = handlers[fn]?(body) ?? ["ok": false, "error": "NOT_FOUND"]
            return (try JSONSerialization.data(withJSONObject: reply), 200)
        }
    }
}

struct DataServiceTests {
    let server = FakeServer()
    func service() -> DataService {
        DataService(store: LocalStore(fileURL: nil), rpc: RpcClient(baseURL: "https://example.test", apiKey: "key", transport: server.transport))
    }

    func birthday(_ id: String, list: String = localListId) -> Event {
        Event(id: id, listId: list, title: " Сара ", type: .birthday, customLabel: nil, recurring: true, observeBy: .hebrew,
              date: .fromGregorian(GregorianParts(year: 1990, month: 4, day: 23), afterSunset: false), note: "  ")
    }

    @Test func recordRoundTrip() {
        var e = birthday("a")
        e.observeBoth = true
        let r = EventRecord(e, syncState: .synced)
        #expect(r.fields.observeBy == "BOTH")
        #expect(r.fields.title == "Сара")
        #expect(r.fields.note == nil)
        var expected = e
        expected.title = "Сара"
        expected.note = nil
        expected.observeBy = .gregorian  // при «по обоим» основной календарь — календарь ввода (как на Android)
        #expect(r.event == expected)
    }

    @Test func remoteEventEncodesExplicitNulls() throws {
        let json = try JSONSerialization.jsonObject(with: JSONEncoder().encode(RemoteEvent(id: "x"))) as! [String: Any]
        #expect(json["custom_label"] is NSNull)
        #expect(json["h_month"] is NSNull)
        #expect(json.count == 17)
        let decoded = try JSONDecoder().decode(RemoteEvent.self, from: Data(#"{"id":"y"}"#.utf8))
        #expect(decoded.observeBy == "HEBREW" && decoded.recurring && decoded.type == "OTHER")
    }

    @Test func personalListEditing() async throws {
        let s = service()
        try await s.save(birthday("a"))
        #expect(await s.store.state.visibleEvents.count == 1)
        #expect(await s.store.state.event("a")?.syncState == .synced)
        await s.delete(eventId: "a")
        #expect(await s.store.state.events.isEmpty)
    }

    @Test func persistsToFile() async throws {
        let url = FileManager.default.temporaryDirectory.appendingPathComponent("ledor-test-\(UUID()).json")
        defer { try? FileManager.default.removeItem(at: url) }
        let s = DataService(store: LocalStore(fileURL: url), rpc: RpcClient(baseURL: "", apiKey: ""))
        try await s.save(birthday("a"))
        let reopened = LocalStore(fileURL: url)
        #expect(await reopened.state.event("a")?.fields.title == "Сара")
        #expect(await reopened.state.list(localListId) != nil)
    }

    @Test func joinPullsEventsAndPushesEdits() async throws {
        let remote = try JSONSerialization.jsonObject(with: JSONEncoder().encode(EventRecord(birthday("r1"), syncState: .synced).remote))
        server.handlers["join_list"] = { _ in
            ["ok": true, "list": ["id": "L1", "name": "Семья", "revision": 5], "role": "EDITOR", "token": "T", "read_pin": "1234"]
        }
        server.handlers["pull_changes"] = { body in
            ["ok": true, "list": ["id": "L1", "name": "Семья", "revision": 5], "role": "EDITOR",
             "events": (body["p_since"] as? Int ?? 0) == 0 ? [remote] : []]
        }
        server.handlers["push_events"] = { body in
            let ids = (body["p_events"] as! [[String: Any]]).map { $0["id"] as! String }
            return ["ok": true, "revision": 6, "accepted": ids]
        }
        let s = service()
        guard case .ok(.joined(let list, let imported)) = await s.join(name: " Семья ", pin: "123456") else {
            Issue.record("join failed"); return
        }
        #expect(imported == 1)
        #expect(list.role == .editor && list.masterPin == "123456" && list.readPin == "1234")
        #expect(server.calls.first { $0.0 == "join_list" }?.1["p_name"] as? String == "Семья")

        try await s.save(birthday("n1", list: "L1"))
        #expect(await s.store.state.event("n1")?.syncState == .pendingUpsert)
        _ = await s.syncList("L1")
        #expect(await s.store.state.event("n1")?.syncState == .synced)
        #expect(await s.store.state.event("n1")?.rev == 6)

        await s.delete(eventId: "r1")
        #expect(await s.store.state.event("r1")?.syncState == .pendingDelete)
        _ = await s.syncList("L1")
        #expect(await s.store.state.event("r1") == nil)
        let pushed = server.calls.last { $0.0 == "push_events" }!.1["p_events"] as! [[String: Any]]
        #expect(pushed.first?["deleted"] as? Bool == true)
    }

    @Test func deletedListLeavesNotice() async {
        server.handlers["join_list"] = { _ in ["ok": true, "list": ["id": "L2", "name": "Община", "revision": 0], "role": "READER", "token": "T"] }
        server.handlers["pull_changes"] = { _ in ["ok": false, "error": "LIST_DELETED"] }
        let s = service()
        _ = await s.join(name: "Община", pin: "1234")
        #expect(await s.store.state.list("L2") == nil)
        #expect(await s.store.state.deletedListNotices == ["Община"])
    }

    @Test func offlineIsNetworkFailure() async {
        server.offline = true
        let r = await service().join(name: "Семья", pin: "1234")
        guard case .failure(.network) = r else { Issue.record("expected network failure"); return }
        let wrong = FakeServer()
        wrong.handlers["join_list"] = { _ in ["ok": false, "error": "WRONG_PIN", "attempts_left": 3] }
        let s = DataService(store: LocalStore(fileURL: nil), rpc: RpcClient(baseURL: "https://e.test", apiKey: "key", transport: wrong.transport))
        guard case .error("WRONG_PIN", 3, _) = await s.join(name: "x y z", pin: "1111") else { Issue.record("expected WRONG_PIN"); return }
    }

    @Test func readOnlyListRejectsEdits() async {
        let s = service()
        await s.store.mutate { $0.upsert(ListRecord(id: "R", kind: .cloud, name: "R", role: .reader)) }
        await #expect(throws: DataError.self) { try await s.save(birthday("x", list: "R")) }
    }
}

struct SettingsTests {
    @Test func defaultsAndRoundTrip() {
        let store = SettingsStore(suite: "ledor-test-\(UUID())")
        var s = store.load()
        #expect(s.gridMode == .hebrew && s.weekStartsOnSunday && s.holidays.enabled && !s.holidays.shabbat)
        #expect(s.reminders.eveMinutesBeforeSunset == 180 && s.reminders.dayTime == .noon)
        s.cityId = "il-jerusalem"
        s.reminders.eveMode = .fixedTime
        store.save(s)
        #expect(store.load() == s)
        let old = try! JSONDecoder().decode(AppSettings.self, from: Data(#"{"cityId":"ru-moscow"}"#.utf8))
        #expect(old.cityId == "ru-moscow" && old.adarPolicy == .adarI)
    }
}
