/// Модель календаря — `03-calendar-domain.md` §3. Строковые значения — контракт: так месяц хранится в БД,
/// на сервере, в Android-приложении и на сайте.
public enum HebrewMonth: String, CaseIterable, Codable, Sendable {
    case nisan = "NISAN"
    case iyar = "IYAR"
    case sivan = "SIVAN"
    case tammuz = "TAMMUZ"
    case av = "AV"
    case elul = "ELUL"
    case tishrei = "TISHREI"
    case cheshvan = "CHESHVAN"
    case kislev = "KISLEV"
    case tevet = "TEVET"
    case shevat = "SHEVAT"
    /// Адар простого (невисокосного) года.
    case adar = "ADAR"
    /// Адар I високосного года.
    case adarI = "ADAR_I"
    /// Адар II високосного года.
    case adarII = "ADAR_II"

    /// Месяцы года по порядку от Тишрея: 12 в простом году, 13 в високосном.
    public static func yearOrder(leapYear: Bool) -> [HebrewMonth] {
        let adars: [HebrewMonth] = leapYear ? [.adarI, .adarII] : [.adar]
        return [.tishrei, .cheshvan, .kislev, .tevet, .shevat] + adars + [.nisan, .iyar, .sivan, .tammuz, .av, .elul]
    }

    /// Все 14 значений в порядке года от Тишрея — для выбора месяца, когда год неизвестен.
    public static let orderWithoutYear: [HebrewMonth] = [
        .tishrei, .cheshvan, .kislev, .tevet, .shevat, .adar, .adarI, .adarII, .nisan, .iyar, .sivan, .tammuz, .av, .elul,
    ]

    /// Наибольший возможный день месяца, когда год неизвестен.
    public var maxDayWithoutYear: Int {
        switch self {
        case .tishrei, .cheshvan, .kislev, .shevat, .adarI, .nisan, .sivan, .av: 30
        case .tevet, .adar, .adarII, .iyar, .tammuz, .elul: 29
        }
    }

    /// Допустим ли месяц в году данного вида.
    public func exists(inLeapYear leapYear: Bool) -> Bool {
        switch self {
        case .adar: !leapYear
        case .adarI, .adarII: leapYear
        default: true
        }
    }
}

/// Полная еврейская дата; создаётся только валидной (см. `HebrewCalendarMath.isValid`).
public struct HebrewDate: Hashable, Codable, Sendable, CustomStringConvertible {
    public let year: Int
    public let month: HebrewMonth
    public let day: Int

    public init(year: Int, month: HebrewMonth, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    public var description: String { "\(day) \(month.rawValue) \(year)" }
}

public enum CalendarKind: String, CaseIterable, Codable, Sendable {
    case hebrew = "HEBREW"
    case gregorian = "GREGORIAN"
}

public enum EventType: String, CaseIterable, Codable, Sendable {
    case birthday = "BIRTHDAY"
    case yahrzeit = "YAHRZEIT"
    case barMitzvah = "BAR_MITZVAH"
    case batMitzvah = "BAT_MITZVAH"
    case wedding = "WEDDING"
    case other = "OTHER"

    /// Неизвестное или пустое значение с сервера — «Другое», как на Android (`EventType.parse`).
    public static func parse(_ raw: String?) -> EventType {
        raw.flatMap(EventType.init(rawValue:)) ?? .other
    }
}
