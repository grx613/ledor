/// Ежемесячный повтор (B-41): тот же день каждого месяца; если такого дня в месяце нет (31-е, 30 февраля) — последний
/// день месяца. Дата первого раза с известным годом — начало: раньше неё наступлений нет. Без года — каждый месяц.
public enum MonthlyRule {
    public static func date(_ day: Int, year: Int, month: Int) -> CivilDate {
        CivilDate(year: year, month: month, day: min(day, CivilDate.daysInMonth(year: year, month: month)))!
    }

    /// Первое наступление или `nil`, если год не указан.
    public static func start(_ g: GregorianParts) -> CivilDate? { g.year.map { date(g.day, year: $0, month: g.month) } }

    public static func between(_ g: GregorianParts, from: CivilDate, to: CivilDate) -> [CivilDate] {
        let lo = start(g).map { max($0, from) } ?? from
        guard lo <= to else { return [] }
        var result: [CivilDate] = []
        var (y, m) = (lo.year, lo.month)
        while (y, m) <= (to.year, to.month) {
            let d = date(g.day, year: y, month: m)
            if d >= lo && d <= to { result.append(d) }
            (y, m) = m == 12 ? (y + 1, 1) : (y, m + 1)
        }
        return result
    }

    public static func next(_ g: GregorianParts, today: CivilDate) -> CivilDate {
        let lo = start(g).map { max($0, today) } ?? today
        let d = date(g.day, year: lo.year, month: lo.month)
        if d >= lo { return d }
        return lo.month == 12 ? date(g.day, year: lo.year + 1, month: 1) : date(g.day, year: lo.year, month: lo.month + 1)
    }
}

/// Наступление события в конкретный день (`03` §7).
public struct Occurrence: Hashable, Sendable {
    public let eventId: String
    public let date: CivilDate
    public let hebrewDate: HebrewDate
    public let yearsSince: Int?
    /// Еврейский день начинается накануне вечером.
    public let startsEveBefore: Bool
    /// Наступление ежемесячного события (B-41): показывается только григорианская дата.
    public let monthly: Bool

    public init(eventId: String, date: CivilDate, hebrewDate: HebrewDate, yearsSince: Int?, startsEveBefore: Bool, monthly: Bool = false) {
        self.eventId = eventId
        self.date = date
        self.hebrewDate = hebrewDate
        self.yearsSince = yearsSince
        self.startsEveBefore = startsEveBefore
        self.monthly = monthly
    }

    /// По какому календарю это наступление.
    public var calendar: CalendarKind { startsEveBefore ? .hebrew : .gregorian }
}

public enum OccurrenceCalculator {
    private typealias M = HebrewCalendarMath

    public static func inHebrewYear(_ e: Event, _ year: Int, _ policy: YahrzeitAdarPolicy) -> Occurrence? {
        if !e.recurring { return oneOff(e).flatMap { M.hebrewYearOf($0.date) == year ? $0 : nil } }
        guard let h = e.date.hebrew, M.isHebrewYearSupported(year) else { return nil }
        if let origin = h.year, year < origin + EventDefaults.firstOccurrenceOffset(e.type) { return nil }
        let hd = AnniversaryCalculator.hebrewAnniversary(EventDefaults.rule(e.type), h, year, policy)
        guard M.isValid(hd.year, hd.month, hd.day) else { return nil }
        return Occurrence(eventId: e.id, date: M.toGregorian(hd), hebrewDate: hd, yearsSince: h.year.map { year - $0 },
                          startsEveBefore: true)
    }

    public static func inGregorianYear(_ e: Event, _ year: Int) -> Occurrence? {
        if !e.recurring { return oneOff(e).flatMap { $0.date.year == year ? $0 : nil } }
        guard let g = e.date.gregorian, M.isGregorianYearSupported(year) else { return nil }
        if let origin = g.year, year < origin + EventDefaults.firstOccurrenceOffset(e.type) { return nil }
        let date = AnniversaryCalculator.gregorianAnniversary(g, year)
        return Occurrence(eventId: e.id, date: date, hebrewDate: M.fromGregorian(date), yearsSince: g.year.map { year - $0 },
                          startsEveBefore: false)
    }

    public static func oneOff(_ e: Event) -> Occurrence? {
        guard let date = e.date.gregorian?.civilDate else { return nil }
        let heb = e.date.hebrew?.hebrewDate ?? M.fromGregorian(date)
        return Occurrence(eventId: e.id, date: date, hebrewDate: heb, yearsSince: 0, startsEveBefore: e.observeBy == .hebrew)
    }

    /// Календари, по которым отмечается повторяющееся событие (основной — первым).
    private static func calendars(_ e: Event) -> [CalendarKind] {
        if e.observeBoth && e.date.hebrew?.year != nil && e.date.gregorian?.year != nil {
            return [e.observeBy, e.observeBy == .hebrew ? .gregorian : .hebrew]
        }
        return [e.observeBy]
    }

    /// Если обе даты совпали — одно наступление (по основному календарю).
    private static func merge(_ list: [Occurrence]) -> [Occurrence] {
        var seen = Set<CivilDate>()
        return list.filter { seen.insert($0.date).inserted }.sorted { $0.date < $1.date }
    }

    private static func monthlyOccurrence(_ e: Event, _ date: CivilDate) -> Occurrence {
        Occurrence(eventId: e.id, date: date, hebrewDate: M.fromGregorian(date), yearsSince: nil, startsEveBefore: false, monthly: true)
    }

    public static func between(_ e: Event, from: CivilDate, to: CivilDate, _ policy: YahrzeitAdarPolicy) -> [Occurrence] {
        if to < from { return [] }
        if e.isMonthly, let g = e.date.gregorian { return MonthlyRule.between(g, from: from, to: to).map { monthlyOccurrence(e, $0) } }
        if !e.recurring { return oneOff(e).map { $0.date >= from && $0.date <= to ? [$0] : [] } ?? [] }
        let list = calendars(e).flatMap { cal -> [Occurrence] in
            cal == .hebrew
                ? (M.hebrewYearOf(from)...M.hebrewYearOf(to)).compactMap { inHebrewYear(e, $0, policy) }
                : (from.year...to.year).compactMap { inGregorianYear(e, $0) }
        }
        return merge(list.filter { $0.date >= from && $0.date <= to })
    }

    public static func next(_ e: Event, today: CivilDate, _ policy: YahrzeitAdarPolicy) -> Occurrence? {
        if !e.recurring { return oneOff(e).flatMap { $0.date >= today ? $0 : nil } }
        if e.isMonthly, let g = e.date.gregorian { return monthlyOccurrence(e, MonthlyRule.next(g, today: today)) }
        return merge(calendars(e).compactMap { nextBy(e, $0, today, policy) }).first
    }

    /// Ближайшая дата по каждому календарю события (B-28, D-46): «по обоим» — две даты (совпавшие — одна), иначе — одна.
    public static func nextEach(_ e: Event, today: CivilDate, _ policy: YahrzeitAdarPolicy) -> [Occurrence] {
        if !e.recurring || e.isMonthly { return next(e, today: today, policy).map { [$0] } ?? [] }
        return merge(calendars(e).compactMap { nextBy(e, $0, today, policy) })
    }

    private static func nextBy(_ e: Event, _ cal: CalendarKind, _ today: CivilDate, _ policy: YahrzeitAdarPolicy) -> Occurrence? {
        let candidates: [Occurrence]
        if cal == .hebrew {
            let y = M.hebrewYearOf(today)
            candidates = (y...y + 2).compactMap { inHebrewYear(e, $0, policy) }
        } else {
            candidates = (today.year...today.year + 1).compactMap { inGregorianYear(e, $0) }
        }
        if let upcoming = candidates.filter({ $0.date >= today }).min(by: { $0.date < $1.date }) { return upcoming }
        // Событие начинается в будущем (например, свадьба через 3 года) — ищем дальше.
        guard let origin = e.date.originYear(cal) else { return nil }
        let first = origin + EventDefaults.firstOccurrenceOffset(e.type)
        let occ = cal == .hebrew ? inHebrewYear(e, first, policy) : inGregorianYear(e, first)
        return occ.flatMap { $0.date >= today ? $0 : nil }
    }
}
