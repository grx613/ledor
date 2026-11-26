/// Дата события, как её ввёл пользователь; год может быть неизвестен.
public struct GregorianParts: Hashable, Codable, Sendable {
    public let year: Int?
    public let month: Int
    public let day: Int

    public init(year: Int?, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    public var civilDate: CivilDate? { year.flatMap { CivilDate(year: $0, month: month, day: day) } }
}

public struct HebrewParts: Hashable, Codable, Sendable {
    public let year: Int?
    public let month: HebrewMonth
    public let day: Int

    public init(year: Int?, month: HebrewMonth, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    public init(_ date: HebrewDate) { self.init(year: date.year, month: date.month, day: date.day) }

    public var hebrewDate: HebrewDate? {
        guard let year, HebrewCalendarMath.isValid(year, month, day) else { return nil }
        return HebrewDate(year: year, month: month, day: day)
    }
}

public enum AnniversaryRule: String, Codable, Sendable { case birthday = "BIRTHDAY", yahrzeit = "YAHRZEIT" }

/// Обычай для йорцайта по Адару простого года в високосном году.
public enum YahrzeitAdarPolicy: String, Codable, Sendable { case adarI = "ADAR_I", adarII = "ADAR_II" }

/// Ежегодная дата — правила B1–B6 и Y1–Y7 (`03-calendar-domain.md` §5–6).
public enum AnniversaryCalculator {
    private typealias M = HebrewCalendarMath

    public static func hebrewAnniversary(_ rule: AnniversaryRule, _ origin: HebrewParts, _ targetYear: Int,
                                         _ policy: YahrzeitAdarPolicy = .adarI) -> HebrewDate {
        switch rule {
        case .birthday: birthday(origin, targetYear)
        case .yahrzeit: yahrzeit(origin, targetYear, policy)
        }
    }

    private static func lastAdar(_ y: Int) -> HebrewMonth { M.isLeapYear(y) ? .adarII : .adar }

    private static func birthday(_ o: HebrewParts, _ y: Int) -> HebrewDate {
        let leap = M.isLeapYear(y)
        let (m, d) = (o.month, o.day)
        if m == .adar || m == .adarII { return HebrewDate(year: y, month: lastAdar(y), day: d) }
        if m == .adarI && d == 30 && !leap { return HebrewDate(year: y, month: .nisan, day: 1) }
        if m == .adarI { return HebrewDate(year: y, month: leap ? .adarI : .adar, day: d) }
        if m == .cheshvan && d == 30 && !M.isCheshvanLong(y) { return HebrewDate(year: y, month: .kislev, day: 1) }
        if m == .kislev && d == 30 && M.isKislevShort(y) { return HebrewDate(year: y, month: .tevet, day: 1) }
        return HebrewDate(year: y, month: m, day: d)
    }

    private static func yahrzeit(_ o: HebrewParts, _ y: Int, _ policy: YahrzeitAdarPolicy) -> HebrewDate {
        let leap = M.isLeapYear(y)
        let (m, d) = (o.month, o.day)
        if let oy = o.year {
            if m == .cheshvan && d == 30 && !M.isCheshvanLong(oy + 1) {
                return HebrewDate(year: y, month: .cheshvan, day: M.daysInMonth(y, .cheshvan))
            }
            if m == .kislev && d == 30 && M.isKislevShort(oy + 1) {
                return HebrewDate(year: y, month: .kislev, day: M.daysInMonth(y, .kislev))
            }
        }
        let base: HebrewDate
        switch m {
        case .adarII: base = HebrewDate(year: y, month: lastAdar(y), day: d)
        case .adarI where d == 30 && !leap: base = HebrewDate(year: y, month: .shevat, day: 30)
        case .adarI: base = HebrewDate(year: y, month: leap ? .adarI : .adar, day: d)
        case .adar: base = HebrewDate(year: y, month: !leap ? .adar : (policy == .adarI ? .adarI : .adarII), day: d)
        default: base = HebrewDate(year: y, month: m, day: d)
        }
        if base.month == .cheshvan && base.day == 30 && !M.isCheshvanLong(y) { return HebrewDate(year: y, month: .kislev, day: 1) }
        if base.month == .kislev && base.day == 30 && M.isKislevShort(y) { return HebrewDate(year: y, month: .tevet, day: 1) }
        return base
    }

    /// 29 февраля в невисокосный год → 28 февраля (D-11).
    public static func gregorianAnniversary(_ origin: GregorianParts, _ targetYear: Int) -> CivilDate {
        if origin.month == 2 && origin.day == 29 && !CivilDate.isLeapYear(targetYear) {
            return CivilDate(year: targetYear, month: 2, day: 28)!
        }
        return CivilDate(year: targetYear, month: origin.month, day: origin.day)!
    }
}

public struct MitzvahHint: Hashable, Sendable {
    public let type: EventType
    public let hebrewDate: HebrewDate
    public let date: CivilDate
}

/// Подсказка о будущей бар-мицве (13 лет) и бат-мицве (12 лет) — по правилу дня рождения.
public enum MitzvahCalculator {
    public static func mitzvahHints(birth: HebrewParts, today: CivilDate) -> [MitzvahHint] {
        guard let y = birth.year else { return [] }
        return [(EventType.barMitzvah, 13), (EventType.batMitzvah, 12)].compactMap { type, age in
            guard HebrewCalendarMath.isHebrewYearSupported(y + age) else { return nil }
            let hd = AnniversaryCalculator.hebrewAnniversary(.birthday, birth, y + age)
            let date = HebrewCalendarMath.toGregorian(hd)
            return date < today ? nil : MitzvahHint(type: type, hebrewDate: hd, date: date)
        }
    }
}
