import Foundation
import Testing
@testable import Domain

/// B-41: «каждый месяц» — в тот же день каждого месяца; если такого дня нет — в последний день месяца (`MonthlyTest.kt`).
struct MonthlyTests {
    let today = CivilDate(year: 2026, month: 11, day: 27)!
    let policy = YahrzeitAdarPolicy.adarI

    func date(_ y: Int, _ m: Int, _ d: Int) -> CivilDate { CivilDate(year: y, month: m, day: d)! }

    func monthly(_ year: Int?, _ month: Int, _ day: Int, _ type: EventType = .other) -> Event {
        Event(id: "m1", listId: "local", title: "Квартира", type: type, customLabel: type == .other ? "Квартплата" : nil, recurring: true,
              observeBy: .gregorian, date: .fromGregorian(GregorianParts(year: year, month: month, day: day), afterSunset: false),
              note: nil, monthly: true)
    }

    @Test func monthlyDateClampsToMonthLength() {
        #expect(MonthlyRule.date(31, year: 2027, month: 2) == date(2027, 2, 28))
        #expect(MonthlyRule.date(31, year: 2028, month: 2) == date(2028, 2, 29))
        #expect(MonthlyRule.date(31, year: 2027, month: 4) == date(2027, 4, 30))
        #expect(MonthlyRule.date(31, year: 2027, month: 3) == date(2027, 3, 31))
        #expect(MonthlyRule.date(10, year: 2027, month: 2) == date(2027, 2, 10))
    }

    @Test func nextIsThisMonthOrTheNext() {
        let e = monthly(2025, 1, 10)
        let next = OccurrenceCalculator.next(e, today: today, policy)!
        #expect(next.date == date(2026, 12, 10))
        #expect(next.calendar == .gregorian)
        #expect(next.monthly)
        #expect(next.yearsSince == nil)
        #expect(!next.startsEveBefore)
        // Сегодня — день платежа: ближайшее — сегодня.
        #expect(OccurrenceCalculator.next(e, today: date(2026, 12, 10), policy)?.date == date(2026, 12, 10))
        #expect(OccurrenceCalculator.next(e, today: date(2026, 12, 11), policy)?.date == date(2027, 1, 10))
        // Одна дата, как у события по одному календарю.
        #expect(OccurrenceCalculator.nextEach(e, today: today, policy) == [next])
    }

    @Test func day31InShortMonths() {
        let e = monthly(2026, 1, 31)
        let dates = OccurrenceCalculator.between(e, from: date(2027, 1, 1), to: date(2027, 12, 31), policy).map(\.date)
        #expect(dates.count == 12)
        #expect(dates[1] == date(2027, 2, 28))
        #expect(dates[3] == date(2027, 4, 30))
        #expect(dates[11] == date(2027, 12, 31))
        #expect(OccurrenceCalculator.next(e, today: date(2027, 2, 1), policy)?.date == date(2027, 2, 28))
        #expect(OccurrenceCalculator.next(e, today: date(2027, 3, 1), policy)?.date == date(2027, 3, 31))
    }

    @Test func startsFromTheFirstDate() {
        // Первый раз — 15 февраля 2027: раньше наступлений нет.
        let e = monthly(2027, 2, 15)
        #expect(OccurrenceCalculator.next(e, today: today, policy)?.date == date(2027, 2, 15))
        let dates = OccurrenceCalculator.between(e, from: date(2027, 1, 1), to: date(2027, 4, 30), policy).map(\.date)
        #expect(dates == [date(2027, 2, 15), date(2027, 3, 15), date(2027, 4, 15)])
    }

    @Test func withoutYearEveryMonth() {
        let e = monthly(nil, 6, 5)
        #expect(OccurrenceCalculator.next(e, today: today, policy)?.date == date(2026, 12, 5))
        let dates = OccurrenceCalculator.between(e, from: date(2026, 1, 1), to: date(2026, 3, 31), policy).map(\.date)
        #expect(dates == [date(2026, 1, 5), date(2026, 2, 5), date(2026, 3, 5)])
    }

    @Test func monthlyOnlyForOtherRecurringGregorian() {
        #expect(monthly(2026, 1, 10).isMonthly)
        // Не «Другое», одноразовое или без григорианской даты — ежегодное/одноразовое, как раньше.
        let birthday = monthly(2026, 1, 10, .birthday)
        #expect(!birthday.isMonthly)
        #expect(OccurrenceCalculator.next(birthday, today: today, policy)?.date == date(2027, 1, 10))
        var oneOff = monthly(2027, 1, 10)
        oneOff.recurring = false
        #expect(!oneOff.isMonthly)
        #expect(OccurrenceCalculator.next(oneOff, today: today, policy)?.date == date(2027, 1, 10))
        #expect(OccurrenceCalculator.next(oneOff, today: date(2027, 1, 11), policy) == nil)
        var noGregorian = monthly(nil, 1, 10)
        noGregorian.date = .fromHebrew(HebrewParts(year: nil, month: .nisan, day: 10))
        #expect(!noGregorian.isMonthly)
    }

    @Test func remindersEveryMonth() {
        let zone = TimeZone(identifier: "Europe/Moscow")!
        let e = monthly(2025, 1, 10)
        let from = Zoned.instant(today, TimeOfDay(hour: 0, minute: 0), zone)
        let plan = ReminderPlanner.plan([e], ReminderSettings(), DefaultSunsetProvider(zone: zone), zone, policy,
                                        from: from, to: from.addingTimeInterval(80 * 86400))
        // Декабрь, январь, февраль — накануне и в день.
        #expect(plan.count == 6)
        var seen = Set<CivilDate>()
        #expect(plan.map(\.occurrenceDate).filter { seen.insert($0).inserted } == [date(2026, 12, 10), date(2027, 1, 10), date(2027, 2, 10)])
        #expect(plan.allSatisfy { $0.calendar == .gregorian && $0.yearsSince == nil })
    }
}
