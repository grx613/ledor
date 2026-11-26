import Foundation

public enum FailureKind: Sendable { case network, server, notConfigured }

public enum RpcResult<T: Sendable>: Sendable {
    case ok(T)
    /// Бизнес-ошибка из конверта `{"ok":false,"error":"…"}`.
    case error(code: String, attemptsLeft: Int? = nil, retryAfterSec: Int? = nil)
    case failure(FailureKind)

    public var value: T? { if case .ok(let v) = self { v } else { nil } }

    func map<U>(_ f: (T) throws -> U) -> RpcResult<U> {
        switch self {
        case .ok(let v): if let u = try? f(v) { .ok(u) } else { .failure(.server) }
        case .error(let c, let a, let r): .error(code: c, attemptsLeft: a, retryAfterSec: r)
        case .failure(let k): .failure(k)
        }
    }
}

public struct RemoteList: Codable, Hashable, Sendable {
    public let id: String
    public let name: String
    public let revision: Int64
}

public struct JoinResponse: Codable, Sendable {
    public let list: RemoteList
    public let role: String
    public let token: String
    public let readPin: String?
    enum CodingKeys: String, CodingKey { case list, role, token, readPin = "read_pin" }
}

public struct PullResponse: Codable, Sendable {
    public let list: RemoteList
    public let role: String
    public let readPin: String?
    public let events: [RemoteEvent]
    enum CodingKeys: String, CodingKey { case list, role, events, readPin = "read_pin" }

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        list = try c.decode(RemoteList.self, forKey: .list)
        role = try c.decode(String.self, forKey: .role)
        readPin = try c.decodeIfPresent(String.self, forKey: .readPin)
        events = try c.decodeIfPresent([RemoteEvent].self, forKey: .events) ?? []
    }
}

public struct Rejected: Codable, Sendable { public let id: String; public let reason: String }

public struct PushResponse: Codable, Sendable {
    public let revision: Int64
    public let accepted: [String]
    public let rejected: [Rejected]

    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        revision = try c.decode(Int64.self, forKey: .revision)
        accepted = try c.decodeIfPresent([String].self, forKey: .accepted) ?? []
        rejected = try c.decodeIfPresent([Rejected].self, forKey: .rejected) ?? []
    }
    enum CodingKeys: String, CodingKey { case revision, accepted, rejected }
}

/// Транспорт HTTP; в тестах подменяется.
public typealias HTTPTransport = @Sendable (URLRequest) async throws -> (Data, Int)

/// Тонкий клиент к RPC-функциям сервера (`04-backend.md` §3): `POST <адрес>/rest/v1/rpc/<функция>`.
public struct RpcClient: Sendable {
    public let baseURL: String
    public let apiKey: String
    let transport: HTTPTransport

    public init(baseURL: String, apiKey: String, transport: HTTPTransport? = nil) {
        self.baseURL = baseURL
        self.apiKey = apiKey
        self.transport = transport ?? { request in
            let (data, response) = try await URLSession.shared.data(for: request)
            return (data, (response as? HTTPURLResponse)?.statusCode ?? 0)
        }
    }

    public var isConfigured: Bool { !baseURL.isEmpty && !apiKey.isEmpty }

    /// Ответ сервера: разобранный JSON и исходные байты.
    struct Envelope: @unchecked Sendable {
        let object: [String: Any]
        let data: Data
        subscript(_ key: String) -> Any? { object[key] }
    }

    func call(_ function: String, _ body: [String: Any?]) async -> RpcResult<Envelope> {
        guard isConfigured, let url = URL(string: baseURL.trimmingCharacters(in: CharacterSet(charactersIn: "/")) + "/rest/v1/rpc/" + function)
        else { return .failure(.notConfigured) }
        var request = URLRequest(url: url, timeoutInterval: 20)
        request.httpMethod = "POST"
        request.setValue(apiKey, forHTTPHeaderField: "apikey")
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: body.mapValues { $0 ?? NSNull() })
        let data: Data, status: Int
        do { (data, status) = try await transport(request) } catch { return .failure(.network) }
        guard status == 200, let obj = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] else { return .failure(.server) }
        if obj["ok"] as? Bool == true { return .ok(Envelope(object: obj, data: data)) }
        return .error(code: obj["error"] as? String ?? "UNKNOWN", attemptsLeft: obj["attempts_left"] as? Int,
                      retryAfterSec: obj["retry_after_sec"] as? Int)
    }

    private func decode<T: Decodable & Sendable>(_ r: RpcResult<Envelope>, _ type: T.Type) -> RpcResult<T> {
        r.map { try JSONDecoder().decode(T.self, from: $0.data) }
    }

    public func listExists(name: String) async -> RpcResult<Bool> {
        await call("list_exists", ["p_name": name]).map { $0["exists"] as? Bool == true }
    }

    public func createList(name: String, readPin: String, masterPin: String) async -> RpcResult<JoinResponse> {
        decode(await call("create_list", ["p_name": name, "p_read_pin": readPin, "p_master_pin": masterPin]), JoinResponse.self)
    }

    public func joinList(name: String, pin: String) async -> RpcResult<JoinResponse> {
        decode(await call("join_list", ["p_name": name, "p_pin": pin]), JoinResponse.self)
    }

    public func pullChanges(listId: String, token: String, since: Int64) async -> RpcResult<PullResponse> {
        decode(await call("pull_changes", ["p_list_id": listId, "p_token": token, "p_since": since]), PullResponse.self)
    }

    public func pushEvents(listId: String, token: String, events: [RemoteEvent]) async -> RpcResult<PushResponse> {
        guard let data = try? JSONEncoder().encode(events), let arr = try? JSONSerialization.jsonObject(with: data) else {
            return .failure(.server)
        }
        return decode(await call("push_events", ["p_list_id": listId, "p_token": token, "p_events": arr]), PushResponse.self)
    }

    public func leaveList(listId: String, token: String) async -> RpcResult<Bool> {
        await call("leave_list", ["p_list_id": listId, "p_token": token]).map { _ in true }
    }

    public func deleteList(listId: String, token: String, masterPin: String) async -> RpcResult<Bool> {
        await call("delete_list", ["p_list_id": listId, "p_token": token, "p_master_pin": masterPin]).map { _ in true }
    }

    /// B-40: сменить ПИН-коды списка по действующему Мастер-ПИНу (миграция 0003).
    public func changePins(listId: String, token: String, masterPin: String, newReadPin: String, newMasterPin: String) async -> RpcResult<Bool> {
        await call("change_pins", ["p_list_id": listId, "p_token": token, "p_master_pin": masterPin,
                                   "p_new_read_pin": newReadPin, "p_new_master_pin": newMasterPin]).map { _ in true }
    }

    /// Отзыв (B-2). `meta` — техданные, только если пользователь согласился их приложить.
    public func submitFeedback(message: String, contact: String?, meta: [String: String]?) async -> RpcResult<Bool> {
        await call("submit_feedback", ["p_message": message, "p_contact": contact, "p_meta": meta ?? [:]]).map { _ in true }
    }
}
