import Foundation

/// Местное время суток без секунд — аналог `LocalTime` с точностью до минуты.
public struct TimeOfDay: Hashable, Comparable, Codable, Sendable, CustomStringConvertible {
    public let hour: Int
    public let minute: Int

    public init(hour: Int, minute: Int) {
        precondition((0...23).contains(hour) && (0...59).contains(minute), "time \(hour):\(minute)")
        self.hour = hour
        self.minute = minute
    }

    public var minutesOfDay: Int { hour * 60 + minute }
    public static func < (a: TimeOfDay, b: TimeOfDay) -> Bool { a.minutesOfDay < b.minutesOfDay }
    public var description: String { String(format: "%02d:%02d", hour, minute) }
    public static let noon = TimeOfDay(hour: 12, minute: 0)
}

/// Перевод между гражданской датой, временем пояса и моментом (`Date`) — с историческими правилами пояса.
public enum Zoned {
    static func calendar(_ zone: TimeZone) -> Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = zone
        return c
    }

    /// Момент начала минуты `time` в день `date` пояса `zone`. Несуществующее время (переход на летнее) сдвигается вперёд.
    public static func instant(_ date: CivilDate, _ time: TimeOfDay, _ zone: TimeZone) -> Date {
        let comps = DateComponents(year: date.year, month: date.month, day: date.day, hour: time.hour, minute: time.minute)
        return calendar(zone).date(from: comps)!
    }

    public static func civilDate(_ instant: Date, _ zone: TimeZone) -> CivilDate {
        let c = calendar(zone).dateComponents([.year, .month, .day], from: instant)
        return CivilDate(year: c.year!, month: c.month!, day: c.day!)!
    }

    /// Местное время (минуты отбрасываются вниз).
    public static func timeOfDay(_ instant: Date, _ zone: TimeZone) -> TimeOfDay {
        let c = calendar(zone).dateComponents([.hour, .minute], from: instant)
        return TimeOfDay(hour: c.hour!, minute: c.minute!)
    }
}
