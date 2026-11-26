/// Арифметика еврейского календаря (`03-calendar-domain.md` §3). Своя реализация без Foundation —
/// перенос `website/assets/js/hebrew-calendar.js`; проверяется векторами `spec/golden`.
public enum HebrewCalendarMath {
    public static let minGregorianYear = 1600
    public static let maxGregorianYear = 2200
    public static let minHebrewYear = 5360
    public static let maxHebrewYear = 5960

    public static func isHebrewYearSupported(_ year: Int) -> Bool { (minHebrewYear...maxHebrewYear).contains(year) }
    public static func isGregorianYearSupported(_ year: Int) -> Bool { (minGregorianYear...maxGregorianYear).contains(year) }

    public static func isLeapYear(_ year: Int) -> Bool { floorMod(1 + year * 7, 19) < 7 }
    public static func daysInYear(_ year: Int) -> Int { elapsedDays(year + 1) - elapsedDays(year) }
    public static func isCheshvanLong(_ year: Int) -> Bool { daysInYear(year) % 10 == 5 }
    public static func isKislevShort(_ year: Int) -> Bool { daysInYear(year) % 10 == 3 }

    public static func isMonthInYear(_ month: HebrewMonth, _ year: Int) -> Bool { month.exists(inLeapYear: isLeapYear(year)) }

    /// Число дней месяца; месяц должен существовать в этом году.
    public static func daysInMonth(_ year: Int, _ month: HebrewMonth) -> Int {
        precondition(isMonthInYear(month, year), "\(month) is not in year \(year)")
        switch month {
        case .iyar, .tammuz, .elul, .tevet, .adar, .adarII: return 29
        case .cheshvan: return isCheshvanLong(year) ? 30 : 29
        case .kislev: return isKislevShort(year) ? 29 : 30
        default: return 30
        }
    }

    public static func monthsOfYear(_ year: Int) -> [HebrewMonth] { HebrewMonth.yearOrder(leapYear: isLeapYear(year)) }

    public static func isValid(_ year: Int, _ month: HebrewMonth, _ day: Int) -> Bool {
        isHebrewYearSupported(year) && isMonthInYear(month, year) && (1...daysInMonth(year, month)).contains(day)
    }

    /// Номер месяца, как в KosherJava: Нисан = 1 … Адар / Адар I = 12, Адар II = 13.
    public static func kosherJavaMonth(_ month: HebrewMonth) -> Int {
        switch month {
        case .nisan: 1
        case .iyar: 2
        case .sivan: 3
        case .tammuz: 4
        case .av: 5
        case .elul: 6
        case .tishrei: 7
        case .cheshvan: 8
        case .kislev: 9
        case .tevet: 10
        case .shevat: 11
        case .adar, .adarI: 12
        case .adarII: 13
        }
    }

    public static func fromKosherJavaMonth(_ year: Int, _ month: Int) -> HebrewMonth {
        switch month {
        case 1: .nisan
        case 2: .iyar
        case 3: .sivan
        case 4: .tammuz
        case 5: .av
        case 6: .elul
        case 7: .tishrei
        case 8: .cheshvan
        case 9: .kislev
        case 10: .tevet
        case 11: .shevat
        case 12: isLeapYear(year) ? .adarI : .adar
        case 13: .adarII
        default: preconditionFailure("month \(month)")
        }
    }

    /// По дневной гражданской дате.
    public static func fromGregorian(_ date: CivilDate) -> HebrewDate { fromDayNumber(date.dayNumber) }

    /// После захода солнца еврейская дата — уже следующего дня.
    public static func fromGregorian(_ date: CivilDate, afterSunset: Bool) -> HebrewDate {
        fromGregorian(afterSunset ? date.adding(days: 1) : date)
    }

    /// Дневная гражданская дата; еврейская дата должна быть валидной.
    public static func toGregorian(_ date: HebrewDate) -> CivilDate {
        precondition(isValid(date.year, date.month, date.day), "Invalid hebrew date \(date)")
        return CivilDate(dayNumber: dayNumber(date.year, kosherJavaMonth(date.month), date.day))
    }

    public static func hebrewYearOf(_ date: CivilDate) -> Int { fromGregorian(date).year }

    /// Первый день еврейского месяца как гражданская дата.
    public static func firstDayOf(_ year: Int, _ month: HebrewMonth) -> CivilDate {
        toGregorian(HebrewDate(year: year, month: month, day: 1))
    }

    /// Следующий месяц (после Элула — Тишрей следующего года).
    public static func nextMonth(_ year: Int, _ month: HebrewMonth) -> (year: Int, month: HebrewMonth) {
        if month == .elul { return (year + 1, .tishrei) }
        let months = monthsOfYear(year)
        return (year, months[months.firstIndex(of: month)! + 1])
    }

    public static func previousMonth(_ year: Int, _ month: HebrewMonth) -> (year: Int, month: HebrewMonth) {
        if month == .tishrei { return (year - 1, .elul) }
        let months = monthsOfYear(year)
        return (year, months[months.firstIndex(of: month)! - 1])
    }

    // MARK: - Дни от начала эры

    private static let epoch = -1_373_428  // R.D. начала еврейской эры

    /// Дней от эпохи до 1 Тишрея года — по правилам молада и отсрочек (дхийот).
    static func elapsedDays(_ year: Int) -> Int {
        let prev = year - 1
        let monthsElapsed = 235 * floorDiv(prev, 19) + 12 * floorMod(prev, 19) + floorDiv(floorMod(prev, 19) * 7 + 1, 19)
        let partsElapsed = 204 + 793 * (monthsElapsed % 1080)
        let hoursElapsed = 5 + 12 * monthsElapsed + 793 * (monthsElapsed / 1080) + partsElapsed / 1080
        let parts = partsElapsed % 1080 + 1080 * (hoursElapsed % 24)
        let day = 1 + 29 * monthsElapsed + hoursElapsed / 24
        let postponed = parts >= 19440
            || (day % 7 == 2 && parts >= 9924 && !isLeapYear(year))
            || (day % 7 == 1 && parts >= 16789 && isLeapYear(prev))
        let alt = day + (postponed ? 1 : 0)
        return alt + ([0, 3, 5].contains(alt % 7) ? 1 : 0)
    }

    private static func monthsInYear(_ year: Int) -> Int { isLeapYear(year) ? 13 : 12 }

    private static func daysInMonthNumber(_ month: Int, _ year: Int) -> Int {
        switch month {
        case 2, 4, 6, 10, 13: return 29
        case 12: return isLeapYear(year) ? 30 : 29
        case 8: return isCheshvanLong(year) ? 30 : 29
        case 9: return isKislevShort(year) ? 29 : 30
        default: return 30
        }
    }

    private static func dayNumber(_ year: Int, _ month: Int, _ day: Int) -> Int {
        var total = day
        if month < 7 {
            for m in 7...monthsInYear(year) { total += daysInMonthNumber(m, year) }
            for m in stride(from: 1, to: month, by: 1) { total += daysInMonthNumber(m, year) }
        } else {
            for m in stride(from: 7, to: month, by: 1) { total += daysInMonthNumber(m, year) }
        }
        return epoch + elapsedDays(year) + total - 1
    }

    private static func fromDayNumber(_ abs: Int) -> HebrewDate {
        var year = Int((Double(abs - epoch) / 365.24682220597794).rounded(.down))
        while epoch + elapsedDays(year + 1) <= abs { year += 1 }
        var month = abs < dayNumber(year, 1, 1) ? 7 : 1
        while abs > dayNumber(year, month, daysInMonthNumber(month, year)) { month = month % monthsInYear(year) + 1 }
        return HebrewDate(year: year, month: fromKosherJavaMonth(year, month), day: 1 + abs - dayNumber(year, month, 1))
    }
}
