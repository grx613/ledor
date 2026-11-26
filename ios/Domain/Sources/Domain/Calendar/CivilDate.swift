/// Гражданская (григорианская) дата без времени и пояса — аналог `java.time.LocalDate` на Android.
/// Внутри — номер дня от начала эры (R.D.: 1 января 1 года = 1), поэтому сдвиги часовых поясов невозможны.
public struct CivilDate: Hashable, Comparable, Codable, Sendable, CustomStringConvertible {
    public let year: Int
    public let month: Int
    public let day: Int

    /// Только существующая дата, иначе `nil`.
    public init?(year: Int, month: Int, day: Int) {
        guard (1...12).contains(month), day >= 1, day <= CivilDate.daysInMonth(year: year, month: month) else { return nil }
        self.year = year
        self.month = month
        self.day = day
    }

    /// Дата по номеру дня R.D.
    public init(dayNumber: Int) {
        let d0 = dayNumber - 1
        let n400 = floorDiv(d0, 146_097), d1 = floorMod(d0, 146_097)
        let n100 = d1 / 36_524, d2 = d1 % 36_524
        let n4 = d2 / 1461, d3 = d2 % 1461
        let n1 = d3 / 365
        var y = 400 * n400 + 100 * n100 + 4 * n4 + n1
        if !(n100 == 4 || n1 == 4) { y += 1 }
        let prior = dayNumber - CivilDate.dayNumber(y, 1, 1)
        let correction = dayNumber < CivilDate.dayNumber(y, 3, 1) ? 0 : (CivilDate.isLeapYear(y) ? 1 : 2)
        let m = (12 * (prior + correction) + 373) / 367
        year = y
        month = m
        day = dayNumber - CivilDate.dayNumber(y, m, 1) + 1
    }

    /// Разбор `YYYY-MM-DD`.
    public init?(iso: String) {
        let parts = iso.split(separator: "-")
        guard parts.count == 3, let y = Int(parts[0]), let m = Int(parts[1]), let d = Int(parts[2]) else { return nil }
        self.init(year: y, month: m, day: d)
    }

    public var dayNumber: Int { CivilDate.dayNumber(year, month, day) }

    public func adding(days: Int) -> CivilDate { CivilDate(dayNumber: dayNumber + days) }

    /// Дней от `self` до `other` (положительно, если `other` позже).
    public func days(until other: CivilDate) -> Int { other.dayNumber - dayNumber }

    /// 1 = понедельник … 7 = воскресенье (как `DayOfWeek` в Java).
    public var isoWeekday: Int { floorMod(dayNumber - 1, 7) + 1 }

    public var iso: String {
        func pad(_ n: Int, _ w: Int) -> String { String(repeating: "0", count: max(0, w - String(n).count)) + String(n) }
        return "\(pad(year, 4))-\(pad(month, 2))-\(pad(day, 2))"
    }

    public var description: String { iso }

    public static func < (a: CivilDate, b: CivilDate) -> Bool { a.dayNumber < b.dayNumber }

    public static func isLeapYear(_ y: Int) -> Bool { (y % 4 == 0 && y % 100 != 0) || y % 400 == 0 }

    public static func daysInMonth(year: Int, month: Int) -> Int {
        switch month {
        case 2: isLeapYear(year) ? 29 : 28
        case 4, 6, 9, 11: 30
        default: 31
        }
    }

    static func dayNumber(_ y: Int, _ m: Int, _ d: Int) -> Int {
        let p = y - 1
        return 365 * p + floorDiv(p, 4) - floorDiv(p, 100) + floorDiv(p, 400) + (367 * m - 362) / 12
            + (m <= 2 ? 0 : (isLeapYear(y) ? -1 : -2)) + d
    }
}

func floorDiv(_ a: Int, _ b: Int) -> Int { Int((Double(a) / Double(b)).rounded(.down)) }
func floorMod(_ a: Int, _ b: Int) -> Int { a - b * floorDiv(a, b) }
