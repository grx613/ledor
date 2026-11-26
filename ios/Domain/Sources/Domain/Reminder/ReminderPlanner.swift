import Foundation

public enum ReminderKind: String, Sendable, Comparable {
    case eve = "EVE", day = "DAY"
    public static func < (a: ReminderKind, b: ReminderKind) -> Bool { a == .eve && b == .day }
}

public enum EveMode: String, Codable, Sendable { case beforeSunset = "BEFORE_SUNSET", fixedTime = "FIXED_TIME" }

/// Настройки напоминаний; значения по умолчанию — как на Android.
public struct ReminderSettings: Hashable, Codable, Sendable {
    public var eveEnabled = true
    public var eveMode = EveMode.beforeSunset
    public var eveMinutesBeforeSunset = 180
    public var eveFixedTime = TimeOfDay(hour: 15, minute: 0)
    public var dayEnabled = true
    public var dayTime = TimeOfDay.noon

    public init() {}
    public var anyEnabled: Bool { eveEnabled || dayEnabled }
}

public struct PlannedReminder: Hashable, Sendable {
    public let eventId: String
    public let occurrenceDate: CivilDate
    public let kind: ReminderKind
    public let triggerAt: Date
    public let yearsSince: Int?
    /// Календарь наступления — для текста уведомления (при «по обоим» бывают оба).
    public let calendar: CalendarKind

    /// Идентификатор локального уведомления на iOS (`09-ios-notes.md` §7).
    public var notificationId: String { "\(eventId)|\(occurrenceDate.iso)|\(kind.rawValue)" }
}

/// Планировщик — чистая функция (`03` §9).
public enum ReminderPlanner {
    public static func eveTrigger(_ date: CivilDate, _ s: ReminderSettings, _ sunset: any SunsetProvider, _ zone: TimeZone) -> Date {
        let eve = date.adding(days: -1)
        switch s.eveMode {
        case .beforeSunset: return sunset.sunset(eve).addingTimeInterval(Double(-s.eveMinutesBeforeSunset * 60))
        case .fixedTime: return Zoned.instant(eve, s.eveFixedTime, zone)
        }
    }

    public static func dayTrigger(_ date: CivilDate, _ s: ReminderSettings, _ zone: TimeZone) -> Date {
        Zoned.instant(date, s.dayTime, zone)
    }

    /// Все напоминания с моментом в (from, to], по возрастанию.
    public static func plan(_ events: [Event], _ settings: ReminderSettings, _ sunset: any SunsetProvider, _ zone: TimeZone,
                            _ policy: YahrzeitAdarPolicy, from: Date, to: Date) -> [PlannedReminder] {
        guard settings.anyEnabled, to > from else { return [] }
        let fromDate = Zoned.civilDate(from, zone).adding(days: -1)
        let toDate = Zoned.civilDate(to, zone).adding(days: 2)
        var result: [PlannedReminder] = []
        for e in events {
            for occ in OccurrenceCalculator.between(e, from: fromDate, to: toDate, policy) {
                func add(_ kind: ReminderKind, _ t: Date) {
                    if t > from && t <= to {
                        result.append(PlannedReminder(eventId: e.id, occurrenceDate: occ.date, kind: kind, triggerAt: t,
                                                      yearsSince: occ.yearsSince, calendar: occ.calendar))
                    }
                }
                if settings.eveEnabled { add(.eve, eveTrigger(occ.date, settings, sunset, zone)) }
                if settings.dayEnabled { add(.day, dayTrigger(occ.date, settings, zone)) }
            }
        }
        return result.sorted { ($0.triggerAt, $0.eventId, $0.kind) < ($1.triggerAt, $1.eventId, $1.kind) }
    }
}
