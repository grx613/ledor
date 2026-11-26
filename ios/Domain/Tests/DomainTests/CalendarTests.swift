import Testing
@testable import Domain

/// Контрольные примеры из `DomainTest.kt` (Android) для календаря, бар/бат-мицвы и гематрии.
struct CalendarTests {
    let today = CivilDate(year: 2026, month: 11, day: 24)!

    @Test func controlConversions() {
        let d = CivilDate(year: 1990, month: 4, day: 23)!
        #expect(HebrewCalendarMath.fromGregorian(d) == HebrewDate(year: 5750, month: .nisan, day: 28))
        #expect(HebrewCalendarMath.fromGregorian(d, afterSunset: true) == HebrewDate(year: 5750, month: .nisan, day: 29))
        #expect(HebrewCalendarMath.fromGregorian(today) == HebrewDate(year: 5787, month: .kislev, day: 14))
    }

    @Test func civilDate() {
        #expect(CivilDate(year: 2027, month: 2, day: 29) == nil)
        #expect(CivilDate(year: 2028, month: 2, day: 29) != nil)
        #expect(CivilDate(iso: "1600-01-01")!.adding(days: -1).iso == "1599-12-31")
        #expect(today.isoWeekday == 2)  // вторник
        #expect(today.days(until: CivilDate(year: 2027, month: 5, day: 5)!) == 162)
        #expect(CivilDate(dayNumber: 1).iso == "0001-01-01")
    }

    @Test func mitzvah() {
        let birth = HebrewParts(HebrewCalendarMath.fromGregorian(CivilDate(year: 2015, month: 6, day: 2)!))
        let hints = MitzvahCalculator.mitzvahHints(birth: birth, today: today)
        #expect(hints.first { $0.type == .barMitzvah }?.date == CivilDate(year: 2028, month: 6, day: 9))
        #expect(hints.first { $0.type == .batMitzvah }?.date == CivilDate(year: 2027, month: 6, day: 20))
        let old = HebrewParts(HebrewCalendarMath.fromGregorian(CivilDate(year: 2013, month: 3, day: 10)!))
        #expect(MitzvahCalculator.mitzvahHints(birth: old, today: today).isEmpty)
    }

    @Test func yahrzeitAdarIIPolicy() {
        let r = AnniversaryCalculator.hebrewAnniversary(.yahrzeit, HebrewParts(year: 5785, month: .adar, day: 10), 5787, .adarII)
        #expect(r.month == .adarII)
    }

    @Test func gregorianLeapDay() {
        #expect(AnniversaryCalculator.gregorianAnniversary(GregorianParts(year: nil, month: 2, day: 29), 2027)
            == CivilDate(year: 2027, month: 2, day: 28))
    }

    @Test func monthNavigation() {
        #expect(HebrewCalendarMath.nextMonth(5786, .elul) == (5787, .tishrei))
        #expect(HebrewCalendarMath.nextMonth(5787, .shevat) == (5787, .adarI))  // 5787 — високосный
        #expect(HebrewCalendarMath.previousMonth(5787, .tishrei) == (5786, .elul))
        #expect(HebrewCalendarMath.previousMonth(5787, .nisan) == (5787, .adarII))
    }

    @Test func gematriya() {
        #expect(Gematriya.format(5750) == "תש״ן")
        #expect(Gematriya.format(5787) == "תשפ״ז")
        #expect(Gematriya.format(28) == "כ״ח")
        #expect(Gematriya.format(15) == "ט״ו")
        #expect(Gematriya.format(16) == "ט״ז")
        #expect(Gematriya.format(1) == "א׳")
        #expect(Gematriya.format(14) == "י״ד")
        #expect(Gematriya.format(30) == "ל׳")
    }
}
