import Domain
import Foundation

public let localListId = "local"

public enum ListKind: String, Codable, Sendable { case local = "LOCAL", cloud = "CLOUD" }
public enum ListRole: String, Codable, Sendable { case owner = "OWNER", reader = "READER", editor = "EDITOR" }
public enum ListState: String, Codable, Sendable { case active = "ACTIVE", accessLost = "ACCESS_LOST" }
public enum SyncState: String, Codable, Sendable { case synced = "SYNCED", pendingUpsert = "PENDING_UPSERT", pendingDelete = "PENDING_DELETE" }

/// Список событий на телефоне: личный (`local`) или общий из облака. Поля — как таблица `lists` на Android.
public struct ListRecord: Codable, Hashable, Sendable, Identifiable {
    public var id: String
    public var kind: ListKind
    public var name: String
    public var role: ListRole
    public var state: ListState = .active
    public var readPin: String?
    public var masterPin: String?
    public var accessToken: String?
    public var lastSyncedRev: Int64 = 0
    public var lastSyncAt: Date?
    public var notifyEnabled = true
    public var colorIndex = 0
    public var sortOrder = 0
    public var createdAt = Date()

    public var canEdit: Bool { state == .active && (role == .owner || role == .editor) }
    public var isCloud: Bool { kind == .cloud }

    public static let personal = ListRecord(id: localListId, kind: .local, name: "", role: .owner, createdAt: .distantPast)
}

/// Событие в хранилище: поля сервера (`04-backend.md` §3) плюс состояние синхронизации.
public struct EventRecord: Codable, Hashable, Sendable, Identifiable {
    public var id: String
    public var listId: String
    public var rev: Int64 = 0
    public var syncState: SyncState = .synced
    public var updatedAt = Date()
    public var fields: RemoteEvent
}

/// Событие в формате сервера. `observe_by = "BOTH"` — «по обоим календарям» (B-1), основной — календарь ввода.
public struct RemoteEvent: Codable, Hashable, Sendable {
    public var id: String
    public var deleted = false
    public var rev: Int64 = 0
    public var title = ""
    public var type = "OTHER"
    public var customLabel: String?
    public var recurring = true
    public var observeBy = "HEBREW"
    public var inputCalendar = "GREGORIAN"
    public var gYear: Int?
    public var gMonth: Int?
    public var gDay: Int?
    public var afterSunset = false
    public var hYear: Int?
    public var hMonth: String?
    public var hDay: Int?
    public var note: String?

    enum CodingKeys: String, CodingKey {
        case id, deleted, rev, title, type, recurring, note
        case customLabel = "custom_label", observeBy = "observe_by", inputCalendar = "input_calendar"
        case gYear = "g_year", gMonth = "g_month", gDay = "g_day", afterSunset = "after_sunset"
        case hYear = "h_year", hMonth = "h_month", hDay = "h_day"
    }

    public init(id: String) { self.id = id }

    /// Отсутствующие поля — значения по умолчанию, как `ignoreUnknownKeys`/умолчания на Android.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(String.self, forKey: .id)
        deleted = try c.decodeIfPresent(Bool.self, forKey: .deleted) ?? false
        rev = try c.decodeIfPresent(Int64.self, forKey: .rev) ?? 0
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        type = try c.decodeIfPresent(String.self, forKey: .type) ?? "OTHER"
        customLabel = try c.decodeIfPresent(String.self, forKey: .customLabel)
        recurring = try c.decodeIfPresent(Bool.self, forKey: .recurring) ?? true
        observeBy = try c.decodeIfPresent(String.self, forKey: .observeBy) ?? "HEBREW"
        inputCalendar = try c.decodeIfPresent(String.self, forKey: .inputCalendar) ?? "GREGORIAN"
        gYear = try c.decodeIfPresent(Int.self, forKey: .gYear)
        gMonth = try c.decodeIfPresent(Int.self, forKey: .gMonth)
        gDay = try c.decodeIfPresent(Int.self, forKey: .gDay)
        afterSunset = try c.decodeIfPresent(Bool.self, forKey: .afterSunset) ?? false
        hYear = try c.decodeIfPresent(Int.self, forKey: .hYear)
        hMonth = try c.decodeIfPresent(String.self, forKey: .hMonth)
        hDay = try c.decodeIfPresent(Int.self, forKey: .hDay)
        note = try c.decodeIfPresent(String.self, forKey: .note)
    }

    /// Пустые поля отправляются явным `null`, как на Android (`explicitNulls = true`).
    public func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(id, forKey: .id)
        try c.encode(deleted, forKey: .deleted)
        try c.encode(rev, forKey: .rev)
        try c.encode(title, forKey: .title)
        try c.encode(type, forKey: .type)
        try c.encode(customLabel, forKey: .customLabel)
        try c.encode(recurring, forKey: .recurring)
        try c.encode(observeBy, forKey: .observeBy)
        try c.encode(inputCalendar, forKey: .inputCalendar)
        try c.encode(gYear, forKey: .gYear)
        try c.encode(gMonth, forKey: .gMonth)
        try c.encode(gDay, forKey: .gDay)
        try c.encode(afterSunset, forKey: .afterSunset)
        try c.encode(hYear, forKey: .hYear)
        try c.encode(hMonth, forKey: .hMonth)
        try c.encode(hDay, forKey: .hDay)
        try c.encode(note, forKey: .note)
    }
}

public let observeBoth = "BOTH"

extension EventRecord {
    public var event: Event {
        let f = fields
        let g = f.gMonth.flatMap { m in f.gDay.map { GregorianParts(year: f.gYear, month: m, day: $0) } }
        let h = f.hMonth.flatMap(HebrewMonth.init(rawValue:)).flatMap { m in f.hDay.map { HebrewParts(year: f.hYear, month: m, day: $0) } }
        let input: CalendarKind = f.inputCalendar == "HEBREW" ? .hebrew : .gregorian
        let observe: CalendarKind = switch f.observeBy {
        case "GREGORIAN": .gregorian
        case observeBoth: input
        default: .hebrew
        }
        return Event(id: id, listId: listId, title: f.title, type: EventType.parse(f.type), customLabel: f.customLabel,
                     recurring: f.recurring, observeBy: observe,
                     date: EventDate(inputCalendar: input, gregorian: g, afterSunset: f.afterSunset, hebrew: h),
                     note: f.note, observeBoth: f.observeBy == observeBoth)
    }

    public init(_ e: Event, syncState: SyncState, rev: Int64 = 0, now: Date = Date()) {
        func clean(_ s: String?) -> String? {
            let t = s?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            return t.isEmpty ? nil : t
        }
        var f = RemoteEvent(id: e.id)
        f.rev = rev
        f.title = e.title.trimmingCharacters(in: .whitespacesAndNewlines)
        f.type = e.type.rawValue
        f.customLabel = clean(e.customLabel)
        f.recurring = e.recurring
        f.observeBy = e.observeBoth ? observeBoth : e.observeBy.rawValue
        f.inputCalendar = e.date.inputCalendar.rawValue
        f.gYear = e.date.gregorian?.year
        f.gMonth = e.date.gregorian?.month
        f.gDay = e.date.gregorian?.day
        f.afterSunset = e.date.afterSunset
        f.hYear = e.date.hebrew?.year
        f.hMonth = e.date.hebrew?.month.rawValue
        f.hDay = e.date.hebrew?.day
        f.note = clean(e.note)
        self.init(id: e.id, listId: e.listId, rev: rev, syncState: syncState, updatedAt: now, fields: f)
    }

    var remote: RemoteEvent {
        var f = fields
        f.id = id
        f.rev = rev
        f.deleted = syncState == .pendingDelete
        return f
    }

    init(remote: RemoteEvent, listId: String) {
        self.init(id: remote.id, listId: listId, rev: remote.rev, syncState: .synced, updatedAt: Date(), fields: remote)
    }
}
