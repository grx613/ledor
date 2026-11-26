import AppCore
import Domain
import SwiftUI

private typealias M = HebrewCalendarMath

/// Конвертер дат; из результата — создать событие; подсказки «если это дата рождения / смерти».
struct ConverterScreen: View {
    @Environment(AppModel.self) private var model
    @State private var toHebrew = true
    @State private var gd = 1
    @State private var gm = 1
    @State private var gy = ""
    @State private var after = false
    @State private var hd = 1
    @State private var hm = HebrewMonth.tishrei
    @State private var hy = ""
    @State private var loaded = false

    var body: some View {
        let f = Formatters()
        let today = model.today
        let gDate = Int(gy).flatMap { M.isGregorianYearSupported($0) ? CivilDate(year: $0, month: gm, day: gd) : nil }
        let hYear = Int(hy).flatMap { M.isHebrewYearSupported($0) ? $0 : nil }
        let hDate = hYear.flatMap { M.isValid($0, hm, hd) ? HebrewDate(year: $0, month: hm, day: hd) : nil }
        let resultHebrew = toHebrew ? gDate.map { M.fromGregorian($0, afterSunset: after) } : hDate
        let resultDate = toHebrew ? gDate : hDate.map(M.toGregorian)

        Form {
            Section {
                Picker("", selection: $toHebrew) {
                    Text(L.t("conv_g2h")).tag(true)
                    Text(L.t("conv_h2g")).tag(false)
                }
                .pickerStyle(.segmented)
                if toHebrew {
                    HStack {
                        Picker(L.t("day"), selection: $gd) { ForEach(1...31, id: \.self) { Text(String($0)).tag($0) } }.labelsHidden().frame(maxWidth: 70)
                        Picker(L.t("month"), selection: $gm) { ForEach(1...12, id: \.self) { Text(f.gregorianMonthName($0)).tag($0) } }.labelsHidden()
                        yearField($gy)
                    }
                    SunsetToggle(afterSunset: $after)
                    SunsetHintView(date: gDate) { after = $0 }
                } else {
                    let months = hYear.map(M.monthsOfYear) ?? HebrewMonth.orderWithoutYear
                    HStack {
                        Picker(L.t("day"), selection: $hd) { ForEach(1...30, id: \.self) { Text(String($0)).tag($0) } }.labelsHidden().frame(maxWidth: 70)
                        Picker(L.t("month"), selection: $hm) { ForEach(months, id: \.self) { Text(f.monthNom($0)).tag($0) } }.labelsHidden()
                        yearField(Binding(get: { hy }, set: { v in
                            hy = v
                            if let y = Int(v), M.isHebrewYearSupported(y), !M.isMonthInYear(hm, y) { hm = hm == .adar ? .adarII : .adar }
                        }))
                    }
                }
            }
            if let rh = resultHebrew, let rd = resultDate {
                Section {
                    HighlightCard {
                        if toHebrew {
                            Text(L.t("conv_hebrew_label")).font(Typography.rubik(12, .medium))
                            Text(f.hebrewDate(rh)).font(Typography.rubik(24, .medium))
                            Text(f.gregorianWeekday(after ? rd.adding(days: 1) : rd).components(separatedBy: ",").first ?? "").font(Typography.rubik(14))
                        } else {
                            Text(L.t("conv_gregorian_label")).font(Typography.rubik(12, .medium))
                            Text(f.gregorianWeekday(rd)).font(Typography.rubik(22, .medium))
                            Text(L.f("conv_eve_note", f.gregorianDayMonth(rd.adding(days: -1)))).font(Typography.rubik(12))
                        }
                    }
                    .listRowInsets(EdgeInsets())
                    NavigationLink(value: Route.editor(toHebrew
                        ? EditorSeed(calendar: .gregorian, year: rd.year, month: rd.month, day: rd.day, afterSunset: after)
                        : EditorSeed(calendar: .hebrew, year: rh.year, hebrewMonth: rh.month, day: rh.day))) {
                        Label(L.t("conv_create_event"), systemImage: "plus").foregroundStyle(Palette.primary)
                    }
                }
                let parts = HebrewParts(rh)
                let hints = MitzvahCalculator.mitzvahHints(birth: parts, today: today)
                Section(L.t("conv_if_birth")) {
                    if hints.isEmpty { Text(L.t("conv_mitzvah_passed")) }
                    ForEach(hints, id: \.type) { h in
                        Text(L.f(h.type == .barMitzvah ? "hint_bar" : "hint_bat", f.hebrewDate(h.hebrewDate), f.gregorianShortWeekday(h.date)))
                    }
                }
                let ty = M.hebrewYearOf(today)
                if let next = (ty...ty + 1).lazy.compactMap({ y -> (HebrewDate, CivilDate)? in
                    guard y > rh.year else { return nil }
                    let h = AnniversaryCalculator.hebrewAnniversary(.yahrzeit, parts, y)
                    let d = M.toGregorian(h)
                    return d >= today ? (h, d) : nil
                }).first {
                    Section(L.t("conv_if_death")) {
                        Text(L.f("conv_next_yahrzeit", f.hebrewDate(next.0), f.gregorianShortWeekday(next.1)))
                    }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("tab_converter"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            guard !loaded else { return }
            loaded = true
            let t = model.today, h = M.fromGregorian(t)
            gd = t.day; gm = t.month; gy = String(t.year)
            hd = h.day; hm = h.month; hy = String(h.year)
        }
        .ledorScreen()
    }

    private func yearField(_ text: Binding<String>) -> some View {
        TextField(L.t("year"), text: Binding(get: { text.wrappedValue }, set: { text.wrappedValue = String($0.filter(\.isNumber).prefix(4)) }))
            .keyboardType(.numberPad).multilineTextAlignment(.trailing).frame(maxWidth: 80)
    }
}

/// Встроенный список «Еврейские праздники» (B-25): три переключателя и ближайшие даты на год вперёд.
struct HolidaysScreen: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        let h = model.settings.holidays
        let city = model.city
        let items = model.upcoming.filter { $0.event.listId == holidaysListId }
        List {
            Section {
                Toggle(L.t("holidays_show"), isOn: $m.settings.holidays.enabled)
                Toggle(isOn: Binding(get: { h.enabled && h.notify }, set: { m.settings.holidays.notify = $0 })) {
                    subtitled(L.t("holidays_notify"), L.t("holidays_notify_hint"))
                }
                .disabled(!h.enabled)
                Toggle(isOn: Binding(get: { h.enabled && h.shabbat && city != nil }, set: { m.settings.holidays.shabbat = $0 })) {
                    subtitled(L.t("holidays_shabbat"), L.t("holidays_shabbat_hint"))
                }
                .disabled(!h.enabled || city == nil)
            } footer: {
                if let city {
                    Text(L.t(HolidayEvents.inIsrael(city) ? "holidays_region_israel" : "holidays_region_diaspora") + " "
                        + L.f("holidays_candles_rule", CandleLightingCalculator.minutesBeforeSunset(city), cityName(city)))
                } else {
                    VStack(alignment: .leading) {
                        Text(L.t("holidays_city_needed"))
                        NavigationLink(value: Route.cityPicker) { Text(L.t("holidays_choose_city")).foregroundStyle(Palette.primary) }
                    }
                }
            }
            if h.enabled && !items.isEmpty {
                Section(L.t("holidays_upcoming")) {
                    ForEach(items) { u in EventRow(event: u.event, occurrence: u.occurrence, daysUntil: u.daysUntil, showList: false) }
                }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("holidays_title"))
        .navigationBarTitleDisplayMode(.inline)
        .ledorScreen()
    }
}

func subtitled(_ title: String, _ sub: String) -> some View {
    VStack(alignment: .leading, spacing: 2) {
        Text(title)
        Text(sub).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
    }
}
