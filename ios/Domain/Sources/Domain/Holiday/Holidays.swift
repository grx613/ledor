import Foundation

public enum HolidayKind: Sendable { case festival, fast, memorial }

/// Праздники встроенного списка. Строковые значения — контракт (эталонные векторы, идентификаторы `holiday:<…>`).
public enum Holiday: String, CaseIterable, Sendable {
    case roshHashana = "ROSH_HASHANA", tzomGedaliah = "TZOM_GEDALIAH", yomKippur = "YOM_KIPPUR", sukkot = "SUKKOT"
    case shminiAtzeret = "SHMINI_ATZERET", chanukah = "CHANUKAH", asaraBetevet = "ASARA_BETEVET", tuBishvat = "TU_BISHVAT"
    case taanitEsther = "TAANIT_ESTHER", purim = "PURIM", shushanPurim = "SHUSHAN_PURIM", pesach = "PESACH"
    case pesachVII = "PESACH_VII", yomHashoah = "YOM_HASHOAH", yomHazikaron = "YOM_HAZIKARON", yomHaatzmaut = "YOM_HAATZMAUT"
    case lagBaomer = "LAG_BAOMER", yomYerushalayim = "YOM_YERUSHALAYIM", shavuot = "SHAVUOT", tzomTammuz = "TZOM_TAMMUZ"
    case tishaBav = "TISHA_BAV", tuBav = "TU_BAV"

    public var kind: HolidayKind {
        switch self {
        case .tzomGedaliah, .asaraBetevet, .taanitEsther, .tzomTammuz, .tishaBav: .fast
        case .yomHashoah, .yomHazikaron: .memorial
        default: .festival
        }
    }

    /// Накануне первого дня зажигают свечи.
    public var candles: Bool {
        [.roshHashana, .yomKippur, .sukkot, .shminiAtzeret, .pesach, .pesachVII, .shavuot].contains(self)
    }

    /// Пост начинается на рассвете (а не накануне вечером) и длится до выхода звёзд.
    public var dawn: Bool { [.tzomGedaliah, .asaraBetevet, .taanitEsther, .tzomTammuz].contains(self) }
}

/// `start` — первый день (гражданская дата); праздник начинается накануне вечером.
public struct HolidayOccurrence: Hashable, Sendable {
    public let holiday: Holiday
    public let start: CivilDate
    public let days: Int

    public var end: CivilDate { start.adding(days: days - 1) }
}

/// Правила — `03-calendar-domain.md` §12; проверены на `spec/golden/holidays.json`.
public enum Holidays {
    private typealias M = HebrewCalendarMath
    private static let friday = 5, saturday = 6, sunday = 7, monday = 1

    public static func inHebrewYear(_ year: Int, inIsrael: Bool) -> [HolidayOccurrence] {
        func day(_ m: HebrewMonth, _ d: Int) -> CivilDate { M.toGregorian(HebrewDate(year: year, month: m, day: d)) }
        /// Пост, выпавший на субботу, переносится на воскресенье.
        func fast(_ m: HebrewMonth, _ d: Int) -> CivilDate { let x = day(m, d); return x.isoWeekday == saturday ? x.adding(days: 1) : x }
        let adar: HebrewMonth = M.isLeapYear(year) ? .adarII : .adar
        let yomTovDays = inIsrael ? 1 : 2

        let estherDay = day(adar, 13)
        let esther = estherDay.isoWeekday == saturday ? estherDay.adding(days: -2) : estherDay
        let shoahDay = day(.nisan, 27)
        let shoah = switch shoahDay.isoWeekday {
        case friday: shoahDay.adding(days: -1)
        case sunday: shoahDay.adding(days: 1)
        default: shoahDay
        }
        let atzmautDay = day(.iyar, 5)
        let atzmaut = switch atzmautDay.isoWeekday {
        case friday: atzmautDay.adding(days: -1)
        case saturday: atzmautDay.adding(days: -2)
        case monday: atzmautDay.adding(days: 1)
        default: atzmautDay
        }
        func o(_ h: Holiday, _ start: CivilDate, _ days: Int = 1) -> HolidayOccurrence { HolidayOccurrence(holiday: h, start: start, days: days) }
        return [
            o(.roshHashana, day(.tishrei, 1), 2),
            o(.tzomGedaliah, fast(.tishrei, 3)),
            o(.yomKippur, day(.tishrei, 10)),
            o(.sukkot, day(.tishrei, 15), 7),
            o(.shminiAtzeret, day(.tishrei, 22), yomTovDays),
            o(.chanukah, day(.kislev, 25), 8),
            o(.asaraBetevet, day(.tevet, 10)),
            o(.tuBishvat, day(.shevat, 15)),
            o(.taanitEsther, esther),
            o(.purim, day(adar, 14)),
            o(.shushanPurim, day(adar, 15)),
            o(.pesach, day(.nisan, 15), inIsrael ? 7 : 8),
            o(.pesachVII, day(.nisan, 21), yomTovDays),
            o(.yomHashoah, shoah),
            o(.yomHazikaron, atzmaut.adding(days: -1)),
            o(.yomHaatzmaut, atzmaut),
            o(.lagBaomer, day(.iyar, 18)),
            o(.yomYerushalayim, day(.iyar, 28)),
            o(.shavuot, day(.sivan, 6), yomTovDays),
            o(.tzomTammuz, fast(.tammuz, 17)),
            o(.tishaBav, fast(.av, 9)),
            o(.tuBav, day(.av, 15)),
        ]
    }

    /// Праздники, хотя бы один день которых попадает в [from, to], по возрастанию даты.
    public static func between(from: CivilDate, to: CivilDate, inIsrael: Bool) -> [HolidayOccurrence] {
        if to < from { return [] }
        let first = max(M.hebrewYearOf(from), M.minHebrewYear)
        let last = min(M.hebrewYearOf(to), M.maxHebrewYear)
        guard first <= last else { return [] }
        return (first...last).flatMap { inHebrewYear($0, inIsrael: inIsrael) }
            .filter { $0.end >= from && $0.start <= to }
            .sorted { $0.start < $1.start }
    }

    /// День Изкора: Йом Кипур и Шмини Ацерет — первый день; Песах VII и Шавуот — последний. nil — Изкор не читают.
    public static func yizkorDay(_ o: HolidayOccurrence) -> CivilDate? {
        switch o.holiday {
        case .yomKippur, .shminiAtzeret: o.start
        case .pesachVII, .shavuot: o.end
        default: nil
        }
    }

    /// Субботы в [from, to].
    public static func shabbats(from: CivilDate, to: CivilDate) -> [CivilDate] {
        var d = from.adding(days: (saturday - from.isoWeekday + 7) % 7)
        var result: [CivilDate] = []
        while d <= to { result.append(d); d = d.adding(days: 7) }
        return result
    }
}

/// `afterNightfall` — канун выпал на субботу: свечи зажигают после выхода звёзд, не раньше `time`.
public struct CandleLighting: Hashable, Sendable {
    public let time: Date
    public let afterNightfall: Bool
}

public enum CandleLightingCalculator {
    /// За сколько минут до захода зажигают свечи: Иерусалим — 40, Хайфа — 30, остальные — 18.
    public static func minutesBeforeSunset(_ city: City) -> Int {
        switch city.id {
        case "il-jerusalem": 40
        case "il-haifa": 30
        default: 18
        }
    }

    /// Зажигание накануне дня `day`. До захода — округление вниз до минуты, после выхода звёзд — вверх. nil — нет захода.
    public static func forDay(_ day: CivilDate, city: City) -> CandleLighting? {
        let eve = day.adding(days: -1)
        let p = CitySunsetProvider(city: city)
        guard let sunset = p.sunsetOrNil(eve) else { return nil }
        if eve.isoWeekday != 6 {
            let t = sunset.addingTimeInterval(Double(-minutesBeforeSunset(city) * 60))
            return CandleLighting(time: floorToMinute(t), afterNightfall: false)
        }
        let nightfall = p.nightfallOrNil(eve).flatMap { $0 > sunset ? $0 : nil }
            ?? sunset.addingTimeInterval(Double(SunsetHints.nightfallFallbackMinutes * 60))
        return CandleLighting(time: floorToMinute(nightfall.addingTimeInterval(59)), afterNightfall: true)
    }

    private static func floorToMinute(_ d: Date) -> Date {
        Date(timeIntervalSince1970: (d.timeIntervalSince1970 / 60).rounded(.down) * 60)
    }
}
