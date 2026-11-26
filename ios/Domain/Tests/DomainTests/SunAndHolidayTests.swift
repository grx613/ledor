import Foundation
import Testing
@testable import Domain

/// Заход солнца, подсказка, планировщик (`DomainTest.kt`) и праздники со свечами (`HolidaysTest.kt`) — как на Android.
struct SunAndHolidayTests {
    let catalog = CityCatalog.builtIn
    let today = CivilDate(year: 2026, month: 11, day: 24)!
    func date(_ y: Int, _ m: Int, _ d: Int) -> CivilDate { CivilDate(year: y, month: m, day: d)! }
    func t(_ h: Int, _ m: Int) -> TimeOfDay { TimeOfDay(hour: h, minute: m) }
    func minutesApart(_ a: TimeOfDay, _ b: TimeOfDay) -> Int { abs(a.minutesOfDay - b.minutesOfDay) }

    @Test func citiesMatchAndroid() throws {
        #expect(catalog.cities.count > 100)
        #expect(Set(catalog.cities.map(\.id)).count == catalog.cities.count)
        for c in catalog.cities {
            #expect(TimeZone(identifier: c.zoneId) != nil, "\(c.id)")
            #expect((-90...90).contains(c.lat) && (-180...180).contains(c.lon))
        }
        #expect(catalog.search("иерус").first?.id == "il-jerusalem")
        let android = Golden.directory.deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("android/domain/src/main/resources/cities.json")
        let packaged = try Data(contentsOf: URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().appendingPathComponent("Sources/Domain/Resources/cities.json"))
        #expect(packaged == (try Data(contentsOf: android)), "cities.json разошёлся с Android — скопировать заново")
    }

    func sunset(_ id: String, _ d: CivilDate, _ h: Int, _ m: Int) {
        let city = catalog.byId(id)!
        let got = Zoned.timeOfDay(CitySunsetProvider(city: city).sunset(d), city.timeZone)
        #expect(minutesApart(got, t(h, m)) <= 6, "\(id) \(d): \(got)")
    }

    @Test func sunsets() {
        sunset("il-jerusalem", date(2026, 6, 21), 19, 47)
        sunset("il-jerusalem", date(2026, 12, 21), 16, 40)
        sunset("ru-moscow", date(2026, 6, 21), 21, 17)
        sunset("ru-moscow", date(2026, 12, 21), 15, 58)
        sunset("us-new-york", date(2026, 6, 21), 20, 30)
        sunset("us-new-york", date(2026, 12, 21), 16, 32)
        let polar = City(id: "x", nameRu: "x", nameEn: "x", nameHe: "x", countryCode: "NO", lat: 78.2, lon: 15.6,
                         zoneId: "Arctic/Longyearbyen")
        #expect(Zoned.timeOfDay(CitySunsetProvider(city: polar).sunset(date(2026, 6, 21)), polar.timeZone) == t(18, 0))
    }

    @Test func sunsetHint() {
        let jerusalem = City(id: "j", nameRu: "j", nameEn: "Jerusalem", nameHe: "j", countryCode: "IL", lat: 31.778, lon: 35.235,
                             zoneId: "Asia/Jerusalem")
        let d = date(2026, 12, 21)
        let before = SunsetHints.evaluate(date: d, time: t(15, 0), city: jerusalem)!
        #expect(before.verdict == .before)
        #expect(minutesApart(before.sunset, t(16, 40)) <= 2)
        #expect(minutesApart(before.nightfall, t(17, 18)) <= 8)
        #expect(SunsetHints.evaluate(date: d, time: t(16, 50), city: jerusalem)?.verdict == .twilight)
        #expect(SunsetHints.evaluate(date: d, time: t(18, 0), city: jerusalem)?.verdict == .after)
        #expect(SunsetHints.evaluate(date: d, time: t(1, 30), city: jerusalem)?.verdict == .before)

        // Исторический пояс: в апреле 1990 в Москве UTC+4, заход около 20:50.
        let moscow = City(id: "m", nameRu: "m", nameEn: "Moscow", nameHe: "m", countryCode: "RU", lat: 55.7558, lon: 37.6173,
                          zoneId: "Europe/Moscow")
        let m = SunsetHints.evaluate(date: date(1990, 4, 23), time: t(19, 40), city: moscow)!
        #expect(m.verdict == .before)
        #expect(minutesApart(m.sunset, t(20, 50)) <= 6)

        // Белые ночи: сумерки 72 минуты, после полуночи уже «до захода».
        let spb = City(id: "s", nameRu: "s", nameEn: "Saint Petersburg", nameHe: "s", countryCode: "RU", lat: 59.9343,
                       lon: 30.3351, zoneId: "Europe/Moscow")
        let june = date(2026, 6, 21)
        let white = SunsetHints.evaluate(date: june, time: t(23, 0), city: spb)!
        #expect(white.verdict == .twilight)
        #expect(white.nightfall.minutesOfDay - white.sunset.minutesOfDay == 72)
        #expect(SunsetHints.evaluate(date: june.adding(days: 1), time: t(0, 30), city: spb)?.verdict == .before)

        let polar = City(id: "x", nameRu: "x", nameEn: "x", nameHe: "x", countryCode: "NO", lat: 78.2, lon: 15.6,
                         zoneId: "Arctic/Longyearbyen")
        #expect(SunsetHints.evaluate(date: june, time: t(20, 0), city: polar) == nil)
    }

    func birthday(_ h: HebrewParts) -> Event {
        Event(id: "e1", listId: "local", title: "Сара", type: .birthday, customLabel: nil, recurring: true, observeBy: .hebrew,
              date: .fromHebrew(h), note: nil)
    }

    @Test func plannerDefaults() {
        let zone = TimeZone(identifier: "Asia/Jerusalem")!
        let e = birthday(HebrewParts(year: nil, month: .kislev, day: 20))
        let from = Zoned.instant(today, t(0, 0), zone)
        let to = from.addingTimeInterval(30 * 86400)
        let plan = ReminderPlanner.plan([e], ReminderSettings(), DefaultSunsetProvider(zone: zone), zone, .adarI, from: from, to: to)
        let d = HebrewCalendarMath.toGregorian(HebrewDate(year: 5787, month: .kislev, day: 20))
        #expect(plan.count == 2)
        #expect(plan[0].triggerAt == Zoned.instant(d.adding(days: -1), t(15, 0), zone))
        #expect(plan[0].kind == .eve)
        #expect(plan[1].triggerAt == Zoned.instant(d, .noon, zone))
        #expect(plan[0].notificationId == "e1|\(d.iso)|EVE")
        var noEve = ReminderSettings(); noEve.eveEnabled = false
        let off = ReminderPlanner.plan([e], noEve, DefaultSunsetProvider(zone: zone), zone, .adarI, from: plan[0].triggerAt, to: to)
        #expect(off.map(\.kind) == [.day])
        var fixed = ReminderSettings(); fixed.eveMode = .fixedTime; fixed.eveFixedTime = t(19, 30); fixed.dayEnabled = false
        let f = ReminderPlanner.plan([e], fixed, DefaultSunsetProvider(zone: zone), zone, .adarI, from: from, to: to)
        #expect(f.map(\.triggerAt) == [Zoned.instant(d.adding(days: -1), t(19, 30), zone)])
    }

    @Test func observeBothReminders() {
        var e = Event(id: "e1", listId: "local", title: "Сара", type: .birthday, customLabel: nil, recurring: true,
                      observeBy: .hebrew, date: .fromGregorian(GregorianParts(year: 1990, month: 4, day: 23), afterSunset: false),
                      note: nil)
        e.observeBoth = true
        let zone = TimeZone(identifier: "Europe/Moscow")!
        let from = Zoned.instant(date(2027, 4, 1), t(0, 0), zone)
        let plan = ReminderPlanner.plan([e], ReminderSettings(), DefaultSunsetProvider(zone: zone), zone, .adarI,
                                        from: from, to: from.addingTimeInterval(60 * 86400))
        #expect(plan.count == 4)
        #expect(Set(plan.map(\.calendar)) == [.gregorian, .hebrew])
    }

    @Test func holidaysMatchHebcal() throws {
        let data = try Data(contentsOf: Golden.directory.appendingPathComponent("holidays.json"))
        let rows = (try JSONSerialization.jsonObject(with: data) as! [String: Any])["rows"] as! [[String: Any]]
        #expect(rows.count > 600)
        var cache: [String: [Holiday: HolidayOccurrence]] = [:]
        for r in rows {
            let year = r["year"] as! Int, il = r["il"] as! Bool
            let key = "\(year)\(il)"
            if cache[key] == nil {
                cache[key] = Dictionary(uniqueKeysWithValues: Holidays.inHebrewYear(year, inIsrael: il).map { ($0.holiday, $0) })
            }
            let h = Holiday(rawValue: r["holiday"] as! String)!
            let o = cache[key]![h]!
            #expect(o.start == CivilDate(iso: r["start"] as! String), "\(year) il=\(il) \(h)")
            #expect(o.days == r["days"] as! Int, "\(year) il=\(il) \(h)")
        }
        #expect(Set(Holiday.allCases.filter(\.dawn)) == [.tzomGedaliah, .asaraBetevet, .taanitEsther, .tzomTammuz])
        #expect(Set(Holiday.allCases) == Set(rows.map { Holiday(rawValue: $0["holiday"] as! String)! }))
    }

    @Test func betweenAndShabbats() {
        let list = Holidays.between(from: date(2026, 12, 8), to: date(2026, 12, 20), inIsrael: false)
        #expect(list.map(\.holiday) == [.chanukah, .asaraBetevet])
        #expect(list[0].end == date(2026, 12, 12))
        #expect(Holidays.shabbats(from: date(2026, 11, 25), to: date(2026, 12, 5)) == [date(2026, 11, 28), date(2026, 12, 5)])
    }

    @Test func yizkorDays() {
        func day(_ y: Int, _ il: Bool, _ h: Holiday) -> CivilDate? {
            Holidays.yizkorDay(Holidays.inHebrewYear(y, inIsrael: il).first { $0.holiday == h }!)
        }
        #expect(day(5787, true, .shavuot) == date(2027, 6, 11))
        #expect(day(5787, false, .shavuot) == date(2027, 6, 12))
        #expect(day(5787, true, .pesachVII) == date(2027, 4, 28))
        #expect(day(5787, false, .pesachVII) == date(2027, 4, 29))
        #expect(day(5787, false, .yomKippur) == date(2026, 9, 21))
        #expect(day(5787, false, .shminiAtzeret) == date(2026, 10, 3))
        #expect(Holidays.inHebrewYear(5787, inIsrael: false).filter { Holidays.yizkorDay($0) != nil }.count == 4)
    }

    @Test func candleLightingMatchesHebcal() throws {
        let data = try Data(contentsOf: Golden.directory.appendingPathComponent("candles.json"))
        let rows = (try JSONSerialization.jsonObject(with: data) as! [String: Any])["rows"] as! [[String: Any]]
        #expect(rows.count > 300)
        var saturdayNights = 0
        for r in rows {
            let city = catalog.byId(r["city"] as? String)!
            let eve = CivilDate(iso: r["eve"] as! String)!
            #expect(CandleLightingCalculator.minutesBeforeSunset(city) == r["minutes"] as! Int)
            let c = CandleLightingCalculator.forDay(eve.adding(days: 1), city: city)!
            #expect(c.afterNightfall == (eve.isoWeekday == 6))
            if c.afterNightfall { saturdayNights += 1 }
            let got = Zoned.timeOfDay(c.time, city.timeZone)
            let p = (r["time"] as! String).split(separator: ":").map { Int($0)! }
            let diff = minutesApart(got, t(p[0], p[1]))
            #expect(diff <= 2, "\(city.id) \(eve): \(got), Hebcal \(r["time"]!)")
        }
        #expect(saturdayNights > 0)
    }
}
