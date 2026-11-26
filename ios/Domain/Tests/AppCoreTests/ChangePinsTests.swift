import Domain
import Foundation
import Testing
@testable import AppCore

/// B-40: смена ПИН-кодов — как `ChangePinsTest`, `ChangePinsRpcTest` и `SyncDemotionTest` на Android.
struct ChangePinsTests {
    let server = FakeServer()
    func service() -> DataService {
        DataService(store: LocalStore(fileURL: nil), rpc: RpcClient(baseURL: "https://example.test", apiKey: "key", transport: server.transport))
    }

    /// Простой код для общины («0770») принимается; Мастер-ПИН остаётся длинным.
    @Test func simpleReadPinIsAccepted() {
        var f = ChangePinsForm(readPin: "493817", masterPin: "61803398", masterStored: true)
        #expect(f.canSave && f.current == "61803398")
        f.read = "0770"
        #expect(f.canSave)
    }

    @Test func rulesForNewCodes() {
        var f = ChangePinsForm(readPin: "0770", masterPin: "61803398", masterStored: true)
        f.read = "077"; #expect(!f.canSave)
        f.read = "0770"; f.master = "12345"; #expect(!f.canSave)
        f.master = "0770"; #expect(!f.canSave)
        f.master = "27182818"; #expect(f.canSave)
    }

    /// Мастер-ПИН на телефоне не сохранён — без текущего сохранить нельзя.
    @Test func asksCurrentMasterWhenUnknown() {
        var f = ChangePinsForm(readPin: "493817", masterPin: "", masterStored: false)
        f.master = "27182818"
        #expect(!f.canSave)
        f.current = "61803398"
        #expect(f.canSave)
    }

    @Test func changePinsSendsCodesAndKeepsThem() async {
        server.handlers["change_pins"] = { _ in ["ok": true, "read_pin": "0770", "demoted": 1] }
        let s = service()
        await s.store.mutate { $0.upsert(ListRecord(id: "L", kind: .cloud, name: "Община", role: .editor,
                                                   readPin: "493817", masterPin: "61803398", accessToken: "T")) }
        guard case .ok = await s.changePins(listId: "L", currentMasterPin: "61803398", newReadPin: "0770", newMasterPin: "27182818") else {
            Issue.record("change_pins failed"); return
        }
        let body = server.calls.last { $0.0 == "change_pins" }!.1
        #expect(body["p_list_id"] as? String == "L" && body["p_token"] as? String == "T")
        #expect(body["p_master_pin"] as? String == "61803398")
        #expect(body["p_new_read_pin"] as? String == "0770" && body["p_new_master_pin"] as? String == "27182818")
        let l = await s.store.state.list("L")
        #expect(l?.readPin == "0770" && l?.masterPin == "27182818")
    }

    @Test func wrongMasterPinKeepsOldCodes() async {
        server.handlers["change_pins"] = { _ in ["ok": false, "error": "WRONG_PIN", "attempts_left": 4] }
        let s = service()
        await s.store.mutate { $0.upsert(ListRecord(id: "L", kind: .cloud, name: "Община", role: .editor,
                                                   readPin: "493817", masterPin: nil, accessToken: "T")) }
        guard case .error("WRONG_PIN", 4, _) = await s.changePins(listId: "L", currentMasterPin: "11111111", newReadPin: "0770",
                                                                  newMasterPin: "27182818") else {
            Issue.record("expected WRONG_PIN"); return
        }
        #expect(await s.store.state.list("L")?.readPin == "493817")
    }

    /// Мастер-ПИН сменили на другом телефоне — этот стал читателем, забыл старый Мастер-ПИН и сказал об этом.
    @Test func editorBecomesReader() async {
        server.handlers["pull_changes"] = { _ in
            ["ok": true, "list": ["id": "L", "name": "Община", "revision": 3], "role": "READER", "read_pin": "0770", "events": []]
        }
        let s = service()
        await s.store.mutate { $0.upsert(ListRecord(id: "L", kind: .cloud, name: "Община", role: .editor,
                                                   readPin: "493817", masterPin: "12345678", accessToken: "t")) }
        _ = await s.syncList("L")
        let l = await s.store.state.list("L")
        #expect(l?.role == .reader && l?.readPin == "0770" && l?.masterPin == nil)
        #expect(await s.store.state.demotedListNotices == ["Община"])
        await s.clearNotices()
        #expect(await s.store.state.demotedListNotices.isEmpty)
    }

    /// Файл старой версии (без новых полей) открывается без потерь.
    @Test func oldFileStillLoads() throws {
        let old = #"{"lists":[{"id":"local","kind":"LOCAL","name":"","role":"OWNER","state":"ACTIVE","colorIndex":0,"sortOrder":0,"createdAt":0,"notifyEnabled":true,"lastSyncedRev":0}],"events":[],"deletedListNotices":["X"]}"#
        let state = try JSONDecoder().decode(DatabaseState.self, from: Data(old.utf8))
        #expect(state.deletedListNotices == ["X"] && state.demotedListNotices.isEmpty)
    }
}
