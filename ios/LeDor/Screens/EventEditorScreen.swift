import AppCore
import Domain
import SwiftUI

private typealias M = HebrewCalendarMath

/// Редактор события (`01` §3.4): тип, повтор, дата в любом календаре, «год неизвестен», до/после захода, «отмечать по…», заметка.
struct EventEditorScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let seed: EditorSeed
    @State private var s: EditorState?
    @State private var initial: EditorState?
    @State private var attempted = false
    @State private var confirmDelete = false
    @State private var confirmLeave = false
    @State private var toast: String?

    var body: some View {
        Group {
            if let s { form(s) } else { Color.clear }
        }
        .onAppear { if s == nil { load() } }
        .ledorScreen()
    }

    private func load() {
        let today = model.today
        if let id = seed.id, let e = model.event(id) {
            var st = EditorState.from(e, today: today)
            st.readOnly = model.list(e.listId)?.canEdit != true
            s = st; initial = st
            return
        }
        var st = EditorState(today: today)
        st.listId = seed.listId ?? defaultListId(model.lists, lastListId: model.settings.lastListId)
        if let t = seed.type { st.type = t; st.recurring = EventDefaults.recurring(t) }
        if let title = seed.title { st.title = title }
        switch seed.calendar {
        case .gregorian:
            st.input = .gregorian
            st.gDay = seed.day ?? st.gDay; st.gMonth = seed.month ?? st.gMonth
            st.gYear = seed.year.map(String.init) ?? st.gYear; st.afterSunset = seed.afterSunset
        case .hebrew:
            st.input = .hebrew
            st.hDay = seed.day ?? 1; st.hMonth = seed.hebrewMonth ?? .tishrei
            st.hYear = seed.year.map(String.init) ?? ""; st.hYearUnknown = seed.year == nil
        case nil: break
        }
        st.syncOtherCalendar()
        s = st; initial = st
    }

    private func binding<T>(_ kp: WritableKeyPath<EditorState, T>) -> Binding<T> {
        Binding(get: { s![keyPath: kp] }, set: { v in s![keyPath: kp] = v; s!.syncOtherCalendar() })
    }

    @ViewBuilder
    private func form(_ st: EditorState) -> some View {
        let f = Formatters()
        let errors = st.errors
        let ed = st.eventDate()
        let dateErr = attempted && (errors.contains(.dateInvalid) || errors.contains(.yearOutOfRange))
        let editable = model.lists.filter(\.canEdit)
        Form {
            if st.readOnly { Section { Text(L.t("read_only_list")) }.listRowBackground(Palette.secondaryContainer) }
            Section {
                TextField(L.t("field_title"), text: Binding(get: { st.title }, set: { s!.title = String($0.prefix(EventValidator.titleMax)) }))
                    .textInputAutocapitalization(.words)
                if attempted && errors.contains(.titleEmpty) { errorText(L.t("err_title_empty")) }
                Picker(L.t("field_list"), selection: binding(\.listId)) {
                    ForEach(editable) { Text(f.listName($0)).tag($0.id) }
                }
            }
            Section(L.t("field_type")) {
                Picker(L.t("field_type"), selection: Binding(get: { st.type }, set: { s!.setType($0) })) {
                    ForEach(EventType.allCases, id: \.self) { t in
                        Label { Text(f.typeName(t)) } icon: { EventTypeGlyph(type: t, color: Palette.forType(t)).frame(width: 20, height: 20) }.tag(t)
                    }
                }
                .pickerStyle(.navigationLink)
                if st.type == .other {
                    TextField(L.t("field_custom_label"), text: Binding(get: { st.customLabel },
                                                                        set: { s!.customLabel = String($0.prefix(EventValidator.labelMax)) }))
                    if attempted && errors.contains(.customLabelRequired) { errorText(L.t("field_custom_label")) }
                } else {
                    // У «Другое» вместо переключателя — «Повтор» ниже (B-41).
                    Toggle(isOn: Binding(get: { st.recurring }, set: { s!.recurring = $0; s!.recurringManual = true })) {
                        VStack(alignment: .leading) {
                            Text(L.t("field_recurring"))
                            Text(L.t(st.recurring ? "recurring_yes" : "recurring_no")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                        }
                    }
                }
            }
            if st.type == .other {
                // У «Другое» — три варианта повтора, в том числе раз в месяц (B-41).
                // Строками, а не сегментами: «Раз в месяц» и переводы не помещаются в треть ширины на узком экране.
                Section(L.t("field_repeat")) {
                    ForEach(Repeat.allCases, id: \.self) { r in
                        radio(L.t(r == .once ? "repeat_once" : r == .yearly ? "repeat_yearly" : "repeat_monthly"),
                              on: st.repeatMode == r, enabled: true, sub: nil) { s!.setRepeat(r, today: model.today) }
                    }
                }
            }
            Section(L.t("field_date")) {
                // Раз в месяц — только по григорианскому календарю (B-41).
                if !st.isMonthly {
                    Picker("", selection: Binding(get: { st.input }, set: { s!.syncOtherCalendar(); s!.input = $0 })) {
                        Text(L.t("gregorian_date")).tag(CalendarKind.gregorian)
                        Text(L.t("hebrew_date")).tag(CalendarKind.hebrew)
                    }
                    .pickerStyle(.segmented)
                }
                if st.input == .gregorian || st.isMonthly {
                    HStack {
                        Picker(L.t("day"), selection: binding(\.gDay)) { ForEach(1...31, id: \.self) { Text(String($0)).tag($0) } }
                            .labelsHidden().frame(maxWidth: 70)
                        Picker(L.t("month"), selection: binding(\.gMonth)) { ForEach(1...12, id: \.self) { Text(f.gregorianMonthName($0)).tag($0) } }
                            .labelsHidden()
                        yearField(binding(\.gYear), disabled: st.gYearUnknown)
                    }
                    if st.isMonthly {
                        Text(L.t("monthly_hint")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                    } else {
                        Toggle(L.t("year_unknown"), isOn: binding(\.gYearUnknown))
                    }
                    if let y = st.gYearInt, !st.isMonthly {
                        SunsetToggle(afterSunset: binding(\.afterSunset))
                        if !st.readOnly {
                            SunsetHintView(date: CivilDate(year: y, month: st.gMonth, day: st.gDay)) { v in s!.afterSunset = v; s!.syncOtherCalendar() }
                        }
                    }
                } else {
                    let hy = st.hYearInt.flatMap { M.isHebrewYearSupported($0) ? $0 : nil }
                    let months = hy.map(M.monthsOfYear) ?? HebrewMonth.orderWithoutYear
                    HStack {
                        Picker(L.t("day"), selection: binding(\.hDay)) { ForEach(1...30, id: \.self) { Text(String($0)).tag($0) } }
                            .labelsHidden().frame(maxWidth: 70)
                        Picker(L.t("month"), selection: binding(\.hMonth)) { ForEach(months, id: \.self) { Text(f.monthNom($0)).tag($0) } }
                            .labelsHidden()
                        yearField(Binding(get: { st.hYear }, set: { s!.setHYear($0); s!.syncOtherCalendar() }), disabled: st.hYearUnknown)
                    }
                    Toggle(L.t("year_unknown"), isOn: binding(\.hYearUnknown))
                    if let hy, M.isMonthInYear(st.hMonth, hy), st.hDay > M.daysInMonth(hy, st.hMonth) {
                        errorText(L.f("err_month_days", M.daysInMonth(hy, st.hMonth)))
                    }
                }
                if dateErr { errorText(L.t("err_date_invalid")) }
                if attempted && errors.contains(.oneOffNeedsYear) { errorText(L.t("err_one_off_year")) }
                if !st.isMonthly {
                    HighlightCard {
                        if st.input == .gregorian, let h = ed.hebrew, h.year != nil {
                            Text(L.t("conv_hebrew_label")).font(Typography.rubik(12, .medium))
                            Text(f.hebrewParts(h)).font(Typography.rubik(20, .medium))
                        } else if st.input == .hebrew, let g = ed.gregorian?.civilDate {
                            Text(L.t("conv_gregorian_label")).font(Typography.rubik(12, .medium))
                            Text(f.gregorianWeekday(g)).font(Typography.rubik(20, .medium))
                            Text(L.f("conv_eve_note", f.gregorianDayMonth(g.adding(days: -1)))).font(Typography.rubik(12))
                        } else {
                            Text(L.t("conv_need_year"))
                        }
                    }
                    .listRowInsets(EdgeInsets())
                }
            }
            if st.recurring && !st.isMonthly { observeSection(st, ed, f) }
            let previews = previewOccurrences(st, ed)
            if !previews.isEmpty {
                Section {
                    ForEach(previews, id: \.date) { preview in
                        let age = f.agePhrase(st.type, preview.yearsSince).map { ", " + $0.prefix(1).lowercased() + $0.dropFirst() } ?? ""
                        Text(L.f("next_occurrence", f.occurrenceDatesInline(preview), f.relativeDays(model.today.days(until: preview.date))) + age)
                            .font(Typography.rubik(14)).foregroundStyle(Palette.onSurfaceVariant)
                    }
                }
                .listRowBackground(Color.clear)
            }
            if st.type == .birthday, let h = ed.hebrew, h.year != nil, !st.readOnly {
                ForEach(MitzvahCalculator.mitzvahHints(birth: h, today: model.today), id: \.type) { hint in
                    Section {
                        Text(L.f(hint.type == .barMitzvah ? "hint_bar" : "hint_bat", f.hebrewDate(hint.hebrewDate), f.gregorianShortWeekday(hint.date)))
                            .font(Typography.rubik(14, .medium))
                        Button(L.t("add_as_event")) { addMitzvah(hint, st) }
                    }
                    .listRowBackground(Palette.secondaryContainer)
                }
            }
            Section {
                TextField(L.t("field_note"), text: Binding(get: { st.note }, set: { s!.note = String($0.prefix(EventValidator.noteMax)) }), axis: .vertical)
                    .lineLimit(2...6)
            }
        }
        .disabled(st.readOnly)
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t(st.id == nil ? "new_event" : "edit_event"))
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(st != initial)
        .toolbar {
            if st != initial {
                ToolbarItem(placement: .topBarLeading) {
                    Button { confirmLeave = true } label: { Image(systemName: "chevron.backward") }.accessibilityLabel(L.t("back"))
                }
            }
            if !st.readOnly {
                ToolbarItem(placement: .topBarTrailing) { Button(L.t("save")) { save() }.fontWeight(.semibold) }
                if st.id != nil {
                    ToolbarItem(placement: .bottomBar) {
                        Button(L.t("delete"), role: .destructive) { confirmDelete = true }.foregroundStyle(Palette.error)
                    }
                }
            }
        }
        .confirmationDialog(L.f("delete_event_q", st.title), isPresented: $confirmDelete, titleVisibility: .visible) {
            Button(L.t("delete"), role: .destructive) {
                Task { if let id = st.id { await model.data.delete(eventId: id) }; dismiss() }
            }
        }
        .confirmationDialog(L.t("save_changes_q"), isPresented: $confirmLeave, titleVisibility: .visible) {
            Button(L.t("save")) { save() }
            Button(L.t("dont_save"), role: .destructive) { dismiss() }
            Button(L.t("cancel"), role: .cancel) {}
        }
        .toast($toast)
    }

    private func observeSection(_ st: EditorState, _ ed: EventDate, _ f: Formatters) -> some View {
        let known = st.inputYearKnown
        let heb = ed.hebrew.map { f.hebrewDayMonth($0.day, $0.month) }
        let greg = ed.gregorian.flatMap { CivilDate(year: 2000, month: $0.month, day: $0.day) }.map(f.gregorianDayMonth)
        let both = st.effectiveBoth
        let eff = st.effectiveObserve
        return Section(L.t("observe_title")) {
            radio(L.t("observe_hebrew") + (heb.map { " — " + $0 } ?? ""), on: !both && eff == .hebrew,
                  enabled: known || st.input == .hebrew, sub: !known && st.input != .hebrew ? L.t("observe_needs_year") : nil) {
                s!.observeBy = .hebrew; s!.observeBoth = false
            }
            radio(L.t("observe_gregorian") + (greg.map { " — " + $0 } ?? ""), on: !both && eff == .gregorian,
                  enabled: known || st.input == .gregorian, sub: !known && st.input != .gregorian ? L.t("observe_needs_year") : nil) {
                s!.observeBy = .gregorian; s!.observeBoth = false
            }
            radio(L.t("observe_both"), on: both, enabled: known, sub: L.t(known ? "observe_both_hint" : "observe_needs_year")) {
                s!.observeBoth = true
            }
        }
    }

    private func radio(_ title: String, on: Bool, enabled: Bool, sub: String?, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Image(systemName: on ? "largecircle.fill.circle" : "circle").foregroundStyle(on ? Palette.primary : Palette.outline)
                VStack(alignment: .leading) {
                    Text(title).foregroundStyle(Palette.onSurface)
                    if let sub { Text(sub).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant) }
                }
            }
        }
        .disabled(!enabled)
        .opacity(enabled ? 1 : 0.5)
    }

    private func yearField(_ text: Binding<String>, disabled: Bool) -> some View {
        TextField(L.t("year"), text: Binding(get: { disabled ? "" : text.wrappedValue },
                                            set: { text.wrappedValue = String($0.filter(\.isNumber).prefix(4)) }))
            .keyboardType(.numberPad).multilineTextAlignment(.trailing).frame(maxWidth: 80).disabled(disabled)
    }

    private func errorText(_ t: String) -> some View { Text(t).font(Typography.rubik(12)).foregroundStyle(Palette.error) }

    /// «Ближайшее» — по строке на каждую дату («по обоим» — две).
    private func previewOccurrences(_ st: EditorState, _ ed: EventDate) -> [Occurrence] {
        guard EventValidator.validateDate(ed).isEmpty, !st.yearMissingForOneOff else { return [] }
        var e = st.toEvent()
        e.id = "preview"
        return OccurrenceCalculator.nextEach(e, today: model.today, model.settings.adarPolicy)
    }

    private func save() {
        guard let st = s else { return }
        if !st.errors.isEmpty { attempted = true; return }
        var e = st.toEvent()
        if e.id.isEmpty { e.id = UUID().uuidString.lowercased() }
        Task {
            try? await model.data.save(e)
            model.settings.lastListId = e.listId
            _ = await model.reminders.requestPermission()
            dismiss()
        }
    }

    private func addMitzvah(_ hint: MitzvahHint, _ st: EditorState) {
        let title = st.title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !title.isEmpty else { toast = L.t("err_title_empty"); return }
        let e = Event(id: UUID().uuidString.lowercased(), listId: st.listId, title: title, type: hint.type, customLabel: nil,
                      recurring: false, observeBy: .hebrew, date: .fromHebrew(HebrewParts(hint.hebrewDate)), note: nil)
        Task { try? await model.data.save(e); toast = L.t("event_added") }
    }
}
