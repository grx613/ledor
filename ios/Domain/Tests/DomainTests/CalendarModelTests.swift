import Testing
@testable import Domain

struct CalendarModelTests {
    @Test func monthCodesMatchContract() {
        #expect(HebrewMonth.allCases.map(\.rawValue) == [
            "NISAN", "IYAR", "SIVAN", "TAMMUZ", "AV", "ELUL", "TISHREI", "CHESHVAN", "KISLEV", "TEVET", "SHEVAT",
            "ADAR", "ADAR_I", "ADAR_II",
        ])
    }

    @Test func yearOrderStartsAtTishrei() {
        #expect(HebrewMonth.yearOrder(leapYear: false) == [
            .tishrei, .cheshvan, .kislev, .tevet, .shevat, .adar, .nisan, .iyar, .sivan, .tammuz, .av, .elul,
        ])
        #expect(HebrewMonth.yearOrder(leapYear: true) == [
            .tishrei, .cheshvan, .kislev, .tevet, .shevat, .adarI, .adarII, .nisan, .iyar, .sivan, .tammuz, .av, .elul,
        ])
    }

    @Test func monthsExistOnlyInTheirYears() {
        for leap in [false, true] {
            #expect(HebrewMonth.allCases.filter { $0.exists(inLeapYear: leap) } .sorted { $0.rawValue < $1.rawValue }
                == HebrewMonth.yearOrder(leapYear: leap).sorted { $0.rawValue < $1.rawValue })
        }
    }

    @Test func maxDaysWithoutYear() {
        let expected: [HebrewMonth: Int] = [
            .cheshvan: 30, .kislev: 30, .adar: 29, .adarI: 30, .adarII: 29, .nisan: 30, .iyar: 29,
            .sivan: 30, .tammuz: 29, .av: 30, .elul: 29, .tishrei: 30, .tevet: 29, .shevat: 30,
        ]
        for month in HebrewMonth.allCases { #expect(month.maxDayWithoutYear == expected[month]) }
    }

    @Test func eventTypeParse() {
        #expect(EventType.parse("YAHRZEIT") == .yahrzeit)
        #expect(EventType.parse("BAT_MITZVAH") == .batMitzvah)
        #expect(EventType.parse("SOMETHING_NEW") == .other)
        #expect(EventType.parse(nil) == .other)
        #expect(CalendarKind(rawValue: "HEBREW") == .hebrew)
    }
}
