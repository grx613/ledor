import AppCore
import Domain
import Foundation

let holidaysListId = "holidays"

/// Встроенный список «Еврейские праздники» (B-25): в хранилище его нет, события вычисляются и подмешиваются как разовые.
/// Идентификаторы: `holiday:<Holiday>:<первый день>`, `holiday+:<Holiday>:<день>` (следующие дни, только для сетки),
/// `shabbat:<суббота>`. В `note` две строки: для списка и для уведомления.
enum HolidayEvents {
    private static let main = "holiday:", nextDay = "holiday+:", shabbatPrefix = "shabbat:"

    static func isBuiltIn(_ id: String) -> Bool { id.hasPrefix(main) || id.hasPrefix(nextDay) || id.hasPrefix(shabbatPrefix) }
    static func isNextDay(_ id: String) -> Bool { id.hasPrefix(nextDay) }
    static func isShabbat(_ id: String) -> Bool { id.hasPrefix(shabbatPrefix) }

    static func holidayOf(_ id: String) -> Holiday? {
        guard id.hasPrefix(main) else { return nil }
        return Holiday(rawValue: String(id.dropFirst(main.count).prefix { $0 != ":" }))
    }

    static func rowLine(_ e: Event) -> String? {
        let line = e.note?.components(separatedBy: "\n").first ?? ""
        return line.trimmingCharacters(in: .whitespaces).isEmpty ? nil : line
    }

    static func notificationLine(_ e: Event) -> String? {
        guard let note = e.note, let i = note.firstIndex(of: "\n") else { return nil }
        let line = String(note[note.index(after: i)...])
        return line.trimmingCharacters(in: .whitespaces).isEmpty ? nil : line
    }

    /// Израиль — по стране города; без города — по часовому поясу телефона.
    static func inIsrael(_ city: City?) -> Bool {
        if let city { return city.inIsrael }
        return ["Asia/Jerusalem", "Asia/Tel_Aviv", "Israel"].contains(TimeZone.current.identifier)
    }

    static func name(_ h: Holiday) -> String { L.t("holiday_" + h.rawValue.lowercased()) }

    static func kindName(_ k: HolidayKind) -> String {
        switch k {
        case .festival: L.t("holiday_kind_festival")
        case .fast: L.t("holiday_kind_fast")
        case .memorial: L.t("holiday_kind_memorial")
        }
    }

    private static func event(_ id: String, _ title: String, _ label: String, _ date: CivilDate, _ note: String?) -> Event {
        Event(id: id, listId: holidaysListId, title: title, type: .other, customLabel: label, recurring: false, observeBy: .hebrew,
              date: .fromHebrew(HebrewParts(HebrewCalendarMath.fromGregorian(date))), note: note)
    }

    static func build(_ s: HolidaySettings, city: City?, from: CivilDate, to: CivilDate, shabbatFrom: CivilDate, shabbatTo: CivilDate,
                      nextDays: Bool, yizkor: Bool) -> [Event] {
        guard s.enabled else { return [] }
        let f = Formatters()
        func time(_ c: CandleLighting, _ city: City) -> String { f.time(c.time, city.timeZone) }
        func candleNote(_ c: CandleLighting?) -> String {
            guard let c, let city else { return "" }
            return L.f(c.afterNightfall ? "notif_candles_after" : "notif_candles", time(c, city))
        }
        var result: [Event] = []
        var candleDays = Set<CivilDate>()
        for o in Holidays.between(from: from, to: to, inIsrael: inIsrael(city)) {
            let h = o.holiday
            let name = name(h), kind = kindName(h.kind)
            let candles = h.candles ? city.flatMap { CandleLightingCalculator.forDay(o.start, city: $0) } : nil
            if candles != nil { candleDays.insert(o.start) }
            let eve = f.gregorianDayMonthWeekday(o.start.adding(days: -1))
            let starts = h.dawn ? L.t("holiday_fast_dawn")
                : h == .chanukah ? L.f("holiday_first_candle", eve) : L.f("holiday_from_eve", eve)
            var row = starts
            if let candles, let city {
                row += " · " + L.f(candles.afterNightfall ? "holiday_candles_after" : "holiday_candles_eve", time(candles, city))
            }
            let yizkorDay = yizkor ? Holidays.yizkorDay(o) : nil
            let notification = [h.dawn ? L.t("notif_fast_dawn") : candleNote(candles),
                                yizkorDay.map { L.f("notif_yizkor", f.gregorianDayMonthWeekday($0)) } ?? ""]
                .filter { !$0.isEmpty }.joined(separator: " ")
            if yizkorDay != nil { row += " · " + L.t("yizkor") }
            result.append(event("\(main)\(h.rawValue):\(o.start.iso)", name, kind, o.start, row + "\n" + notification))
            // Седьмой день Песаха — отдельная строка, поэтому дни Песаха дальше шестого не дублируются.
            let lastDay = h == .pesach ? min(o.days, 6) : o.days
            if nextDays, lastDay >= 2 {
                for n in 2...lastDay {
                    let day = o.start.adding(days: n - 1)
                    result.append(event("\(nextDay)\(h.rawValue):\(day.iso)", L.f("holiday_day_n", name, n), kind, day, nil))
                }
            }
        }
        if s.shabbat, let city {
            for sat in Holidays.shabbats(from: shabbatFrom, to: shabbatTo) where !candleDays.contains(sat) {
                guard let candles = CandleLightingCalculator.forDay(sat, city: city) else { continue }
                result.append(event("\(shabbatPrefix)\(sat.iso)", L.t("shabbat"), L.f("shabbat_candles_label", time(candles, city)), sat,
                                    "\n" + candleNote(candles)))
            }
        }
        return result
    }
}

/// Вехи первого года (B-26): шлошим, 11 и 12 месяцев — разовые события того же списка `milestone:<веха>:<id йорцайта>`.
enum MilestoneEvents {
    private static let prefix = "milestone:"
    static func isMilestone(_ id: String) -> Bool { id.hasPrefix(prefix) }
    static func parentId(_ id: String) -> String {
        let rest = id.dropFirst(prefix.count)
        return rest.firstIndex(of: ":").map { String(rest[rest.index(after: $0)...]) } ?? String(rest)
    }

    static func label(_ m: MourningMilestone) -> String {
        switch m {
        case .shloshim: L.t("milestone_shloshim")
        case .elevenMonths: L.t("milestone_11_months")
        case .twelveMonths: L.t("milestone_12_months")
        case .firstYahrzeit: L.t("milestone_first_yahrzeit")
        }
    }

    /// nil — не йорцайт, год неизвестен или первый йорцайт уже прошёл.
    static func firstYear(_ e: Event, today: CivilDate, _ policy: YahrzeitAdarPolicy) -> [MilestoneDate]? {
        guard e.type == .yahrzeit, e.recurring, let death = e.date.hebrew?.hebrewDate else { return nil }
        let list = MourningCalculator.firstYear(death: death, policy)
        return list.last!.date >= today ? list : nil
    }

    static func build(_ events: [Event], today: CivilDate, _ policy: YahrzeitAdarPolicy) -> [Event] {
        events.flatMap { e in
            (firstYear(e, today: today, policy) ?? []).filter { $0.milestone != .firstYahrzeit }.map { m in
                Event(id: "\(prefix)\(m.milestone.rawValue):\(e.id)", listId: e.listId, title: e.title, type: .other,
                      customLabel: label(m.milestone), recurring: false, observeBy: .hebrew,
                      date: .fromHebrew(HebrewParts(m.hebrewDate)), note: nil)
            }
        }
    }
}
