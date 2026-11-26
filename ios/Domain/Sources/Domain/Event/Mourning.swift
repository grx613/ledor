/// Вехи первого года после смерти (B-26). Строковые значения — контракт с Android (идентификаторы `milestone:<…>`).
public enum MourningMilestone: String, CaseIterable, Sendable {
    case shloshim = "SHLOSHIM", elevenMonths = "ELEVEN_MONTHS", twelveMonths = "TWELVE_MONTHS", firstYahrzeit = "FIRST_YAHRZEIT"
}

public struct MilestoneDate: Hashable, Sendable {
    public let milestone: MourningMilestone
    public let date: CivilDate
    public let hebrewDate: HebrewDate
}

/// Правила — `03-calendar-domain.md` §13. Считается от дня смерти; обычаи общин различаются.
public enum MourningCalculator {
    private typealias M = HebrewCalendarMath

    /// Та же дата через `months` еврейских месяцев; если такого дня в месяце нет — последний день месяца.
    public static func addMonths(_ d: HebrewDate, _ months: Int) -> HebrewDate {
        var (year, month) = (d.year, d.month)
        for _ in 0..<months { (year, month) = M.nextMonth(year, month) }
        return HebrewDate(year: year, month: month, day: min(d.day, M.daysInMonth(year, month)))
    }

    /// Вехи по возрастанию даты; последняя — первый йорцайт. Если 12 месяцев совпали с йорцайтом — отдельной вехи нет.
    public static func firstYear(death: HebrewDate, _ policy: YahrzeitAdarPolicy) -> [MilestoneDate] {
        func of(_ m: MourningMilestone, _ h: HebrewDate) -> MilestoneDate {
            MilestoneDate(milestone: m, date: M.toGregorian(h), hebrewDate: h)
        }
        let shloshim = M.toGregorian(death).adding(days: 29)
        let yahrzeit = of(.firstYahrzeit,
                          AnniversaryCalculator.hebrewAnniversary(.yahrzeit, HebrewParts(death), death.year + 1, policy))
        let twelve = of(.twelveMonths, addMonths(death, 12))
        return [
            MilestoneDate(milestone: .shloshim, date: shloshim, hebrewDate: M.fromGregorian(shloshim)),
            of(.elevenMonths, addMonths(death, 11)),
            twelve.date != yahrzeit.date ? twelve : nil,
            yahrzeit,
        ].compactMap { $0 }
    }
}
