import Testing
@testable import Domain

/// Тесты событий из `DomainTest.kt` и `HolidaysTest.mourningFirstYear` (Android).
struct EventTests {
    let today = CivilDate(year: 2026, month: 11, day: 24)!
    let policy = YahrzeitAdarPolicy.adarI

    func date(_ y: Int, _ m: Int, _ d: Int) -> CivilDate { CivilDate(year: y, month: m, day: d)! }

    func event(_ date: EventDate, _ type: EventType = .birthday, observe: CalendarKind = .hebrew, recurring: Bool? = nil) -> Event {
        Event(id: "e1", listId: "local", title: "Сара", type: type, customLabel: type == .other ? "X" : nil,
              recurring: recurring ?? EventDefaults.recurring(type), observeBy: observe, date: date, note: nil)
    }

    @Test func nextBirthdayFromSpec() {
        let e = event(.fromGregorian(GregorianParts(year: 1990, month: 4, day: 23), afterSunset: false))
        let occ = OccurrenceCalculator.next(e, today: today, policy)!
        #expect(occ.date == date(2027, 5, 5))
        #expect(occ.yearsSince == 37)
        #expect(today.days(until: occ.date) == 162)
    }

    @Test func gregorianObserve() {
        let e = event(.fromGregorian(GregorianParts(year: 1990, month: 4, day: 30), afterSunset: false), observe: .gregorian)
        #expect(OccurrenceCalculator.next(e, today: today, policy)?.date == date(2027, 4, 30))
        let leap = event(.fromGregorian(GregorianParts(year: nil, month: 2, day: 29), afterSunset: false), observe: .gregorian)
        #expect(OccurrenceCalculator.next(leap, today: today, policy)?.date == date(2027, 2, 28))
    }

    @Test func todayIsOccurrence() {
        let e = event(.fromHebrew(HebrewParts(year: nil, month: .kislev, day: 14)))
        let occ = OccurrenceCalculator.next(e, today: today, policy)!
        #expect(occ.date == today)
        #expect(occ.yearsSince == nil)
    }

    @Test func oneOffPastIsNull() {
        let e = event(.fromGregorian(GregorianParts(year: 2020, month: 1, day: 1), afterSunset: false), .barMitzvah)
        #expect(OccurrenceCalculator.next(e, today: today, policy) == nil)
    }

    @Test func birthdayStartsNextYear() {
        let d = EventDate.fromGregorian(GregorianParts(year: 2026, month: 12, day: 1), afterSunset: false)
        #expect(OccurrenceCalculator.next(event(d, observe: .gregorian), today: today, policy)?.date == date(2027, 12, 1))
        #expect(OccurrenceCalculator.next(event(d, .wedding, observe: .gregorian), today: today, policy)?.date == date(2026, 12, 1))
        let far = event(.fromGregorian(GregorianParts(year: 2030, month: 6, day: 1), afterSunset: false), .wedding)
        #expect(OccurrenceCalculator.next(far, today: today, policy)?.date == date(2030, 6, 1))
    }

    @Test func validation() {
        let ok = event(.fromGregorian(GregorianParts(year: 1990, month: 4, day: 23), afterSunset: false))
        #expect(EventValidator.validate(ok).isEmpty)
        var blank = ok; blank.title = " "
        #expect(EventValidator.validate(blank).contains(.titleEmpty))
        var badHeb = ok; badHeb.date = .fromHebrew(HebrewParts(year: 5786, month: .cheshvan, day: 30))
        #expect(EventValidator.validate(badHeb).contains(.dateInvalid))
        var noYear = ok; noYear.date = .fromGregorian(GregorianParts(year: nil, month: 4, day: 23), afterSunset: false)
        #expect(EventValidator.validate(noYear).contains(.observeNeedsYear))
        var oneOff = noYear; oneOff.recurring = false; oneOff.observeBy = .gregorian
        #expect(EventValidator.validate(oneOff).contains(.oneOffNeedsYear))
        var other = ok; other.type = .other
        #expect(EventValidator.validate(other).contains(.customLabelRequired))
    }

    @Test func observeBoth() {
        var e = event(.fromGregorian(GregorianParts(year: 1990, month: 4, day: 23), afterSunset: false))
        e.observeBoth = true
        let next = OccurrenceCalculator.next(e, today: today, policy)!
        #expect(next.date == date(2027, 4, 23))
        #expect(next.calendar == .gregorian)
        let year = OccurrenceCalculator.between(e, from: date(2027, 1, 1), to: date(2027, 12, 31), policy)
        #expect(year.map(\.date) == [date(2027, 4, 23), date(2027, 5, 5)])
        #expect(year.map(\.calendar) == [.gregorian, .hebrew])
        let all = OccurrenceCalculator.between(e, from: date(1991, 1, 1), to: date(2150, 12, 31), policy)
        #expect(all.count == Set(all.map(\.date)).count)
        var noYear = event(.fromGregorian(GregorianParts(year: nil, month: 4, day: 23), afterSunset: false), observe: .gregorian)
        noYear.observeBoth = true
        #expect(EventValidator.validate(noYear).contains(.observeNeedsYear))
    }

    @Test func listNames() {
        #expect(ListNameRules.key("  Семья   Коэн ") == "семья коэн")
        #expect(!ListNameRules.isValid("ab"))
        #expect(PinRules.isValidReadPin("1234") && !PinRules.isValidReadPin("12a4") && !PinRules.isValidMasterPin("12345"))
        let pin = PinGenerator.generate(length: 6)
        #expect(PinRules.isValidMasterPin(pin))
    }

    @Test func mourningFirstYear() {
        let a = MourningCalculator.firstYear(death: HebrewDate(year: 5786, month: .nisan, day: 10), policy)
        #expect(a.map(\.milestone) == [.shloshim, .elevenMonths, .twelveMonths, .firstYahrzeit])
        #expect(a[1].hebrewDate == HebrewDate(year: 5787, month: .adarI, day: 10))
        #expect(a[2].hebrewDate == HebrewDate(year: 5787, month: .adarII, day: 10))
        #expect(a[3].hebrewDate == HebrewDate(year: 5787, month: .nisan, day: 10))
        #expect(a.map(\.date) == a.map(\.date).sorted())
        let b = MourningCalculator.firstYear(death: HebrewDate(year: 5785, month: .tishrei, day: 10), policy)
        #expect(b.map(\.milestone) == [.shloshim, .elevenMonths, .firstYahrzeit])
        #expect(b[1].hebrewDate == HebrewDate(year: 5785, month: .elul, day: 10))
        #expect(b[2].hebrewDate == HebrewDate(year: 5786, month: .tishrei, day: 10))
        let c = MourningCalculator.firstYear(death: HebrewDate(year: 5787, month: .tishrei, day: 1), policy)[0]
        #expect(c.date == date(2026, 10, 11))
        #expect(c.hebrewDate == HebrewDate(year: 5787, month: .tishrei, day: 30))
        #expect(MourningCalculator.addMonths(HebrewDate(year: 5786, month: .shevat, day: 30), 1)
            == HebrewDate(year: 5786, month: .adar, day: 29))
    }
}
