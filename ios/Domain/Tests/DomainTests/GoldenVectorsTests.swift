import Testing
@testable import Domain

/// Все строки трёх файлов `spec/golden` (веха I1). Не проходит — ошибка в коде, векторы не править.
struct GoldenVectorsTests {
    @Test func conversions() throws {
        let rows = try Golden.rows("conversions.json")
        #expect(rows.count == 1040)
        for (n, r) in rows.enumerated() {
            let g = CivilDate(iso: r[0] as! String)!
            let month = HebrewMonth(rawValue: r[2] as! String)!
            let h = HebrewCalendarMath.fromGregorian(g)
            #expect(h == HebrewDate(year: r[1] as! Int, month: month, day: r[3] as! Int), "row \(n) \(r)")
            #expect(HebrewCalendarMath.kosherJavaMonth(month) == r[4] as! Int, "row \(n)")
            #expect(HebrewCalendarMath.toGregorian(h) == g, "row \(n)")
        }
    }

    @Test func years() throws {
        let rows = try Golden.rows("years.json")
        #expect(rows.count == 201)
        for (n, r) in rows.enumerated() {
            let y = r[0] as! Int
            let leap = r[1] as! Bool
            #expect(HebrewCalendarMath.isLeapYear(y) == leap, "row \(n)")
            #expect(HebrewCalendarMath.daysInYear(y) == r[2] as! Int, "row \(n)")
            #expect(HebrewCalendarMath.daysInMonth(y, .cheshvan) == r[3] as! Int, "row \(n)")
            #expect(HebrewCalendarMath.daysInMonth(y, .kislev) == r[4] as! Int, "row \(n)")
            #expect(HebrewCalendarMath.firstDayOf(y, .tishrei) == CivilDate(iso: r[5] as! String), "row \(n)")
            #expect(HebrewCalendarMath.monthsOfYear(y).count == (leap ? 13 : 12), "row \(n)")
        }
    }

    @Test func anniversaries() throws {
        let rows = try Golden.rows("anniversaries.json")
        #expect(rows.count == 3724)
        for (n, r) in rows.enumerated() {
            let rule = AnniversaryRule(rawValue: r[0] as! String)!
            let origin = HebrewParts(year: r[1] as? Int, month: HebrewMonth(rawValue: r[2] as! String)!, day: r[3] as! Int)
            let res = AnniversaryCalculator.hebrewAnniversary(rule, origin, r[4] as! Int, .adarI)
            #expect(res.month == HebrewMonth(rawValue: r[5] as! String), "row \(n) \(r)")
            #expect(res.day == r[6] as! Int, "row \(n) \(r)")
            #expect(HebrewCalendarMath.toGregorian(res) == CivilDate(iso: r[7] as! String), "row \(n) \(r)")
        }
    }
}
