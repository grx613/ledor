import AppCore
import Domain
import SwiftUI

/// Все события по месяцам в выбранном календаре; фильтр по спискам; прошедшие разовые — отдельно.
struct EventsScreen: View {
    @Environment(AppModel.self) private var model
    @Binding var path: [Route]
    @State private var hebrew = true
    @State private var filter: String?
    @State private var showPast = false

    var body: some View {
        let f = Formatters()
        let kind: CalendarKind = hebrew ? .hebrew : .gregorian
        let items = model.upcoming.filter { filter == nil || $0.event.listId == filter }
        let upcomingIds = Set(model.upcoming.map(\.event.id))
        let past = model.events
            .filter { !upcomingIds.contains($0.id) && !HolidayEvents.isBuiltIn($0.id) && !MilestoneEvents.isMilestone($0.id)
                && (filter == nil || $0.listId == filter) }
            .compactMap { e in OccurrenceCalculator.oneOff(e).map { (e, $0) } }
            .sorted { $0.1.date > $1.1.date }
        var groups: [(String, [UpcomingItem])] = []
        for u in items {
            let title = kind == .hebrew ? f.hebrewMonthTitle(u.occurrence.hebrewDate.year, u.occurrence.hebrewDate.month)
                : f.gregorianMonthTitle(u.occurrence.date.year, u.occurrence.date.month)
            if groups.last?.0 == title { groups[groups.count - 1].1.append(u) } else { groups.append((title, [u])) }
        }
        let hasHolidays = model.upcoming.contains { $0.event.listId == holidaysListId }
        let empty = model.events.allSatisfy { $0.listId == holidaysListId } && items.isEmpty

        return List {
            Section {
                Picker("", selection: $hebrew) {
                    Text(L.t("tab_hebrew_calendar")).tag(true)
                    Text(L.t("tab_gregorian_calendar")).tag(false)
                }
                .pickerStyle(.segmented)
                if model.lists.count > 1 || hasHolidays {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip(L.t("all_lists"), nil)
                            ForEach(model.lists) { chip(f.listName($0), $0.id) }
                            if hasHolidays { chip(L.t("holidays_title"), holidaysListId) }
                        }
                    }
                }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 4, leading: 12, bottom: 4, trailing: 12))

            if empty {
                Section {
                    EmptyState(text: L.t("empty_events")) {
                        Button(L.t("add_event")) { path.append(.editor(EditorSeed())) }.buttonStyle(.borderedProminent)
                        Button(L.t("join_list")) { path.append(.listJoin(nil)) }.buttonStyle(.bordered)
                    }
                }
                .listRowBackground(Color.clear)
            }
            ForEach(groups, id: \.0) { title, group in
                Section {
                    ForEach(group) { u in
                        NavigationLink(value: Route.event(u.event.id)) {
                            EventRow(event: u.event, occurrence: u.occurrence, daysUntil: u.daysUntil, primary: kind, showList: filter == nil)
                        }
                    }
                } header: {
                    Text(title).font(Typography.rubik(14, .semibold)).foregroundStyle(Palette.primary).textCase(nil)
                }
            }
            if !past.isEmpty {
                Section {
                    Button { withAnimation { showPast.toggle() } } label: {
                        Label(L.f("past_events", past.count), systemImage: showPast ? "chevron.up" : "chevron.down")
                    }
                    if showPast {
                        ForEach(past, id: \.0.id) { e, o in
                            NavigationLink(value: Route.event(e.id)) { EventRow(event: e, occurrence: o, daysUntil: nil, primary: kind) }
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("tab_events"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                NavigationLink(value: Route.lists) { Label(L.t("lists"), systemImage: "person.2").labelStyle(.titleAndIcon) }
            }
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.editor(EditorSeed())) { Image(systemName: "plus") }.accessibilityLabel(L.t("add_event"))
            }
        }
        .ledorScreen()
    }

    private func chip(_ title: String, _ id: String?) -> some View {
        let on = filter == id
        return Button { filter = id } label: {
            Text(title).font(Typography.rubik(14, .medium)).padding(.horizontal, 12).padding(.vertical, 6)
                .background(on ? Palette.primaryContainer : Palette.surface, in: Capsule())
                .overlay(Capsule().stroke(on ? Color.clear : Palette.outline))
                .foregroundStyle(on ? Palette.onPrimaryContainer : Palette.onSurface)
        }
        .buttonStyle(.plain)
    }
}

/// Карточка события.
struct EventDetailScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let id: String
    @State private var confirmDelete = false

    var body: some View {
        let f = Formatters()
        if let e = model.event(id) {
            let list = model.list(e.listId)
            let canEdit = list?.canEdit == true
            let policy = model.settings.adarPolicy
            let today = model.today
            // «По обоим календарям» — две ближайшие даты, по блоку на каждую.
            let nexts = OccurrenceCalculator.nextEach(e, today: today, policy)
            let next = nexts.first
            List {
                Section {
                    HStack(spacing: 14) {
                        EventTypeIcon(type: e.type, size: 56)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(e.title).font(Typography.rubik(24, .medium))
                            Text(f.typeLabel(e, next?.yearsSince)).font(Typography.rubik(14, .medium)).foregroundStyle(Palette.forType(e.type))
                            if let list { ListBadge(name: f.listName(list), colorIndex: list.colorIndex) }
                        }
                    }
                    if model.isPending(e.id) {
                        Text(L.t("pending_upload")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                    }
                    ForEach(nexts, id: \.date) { o in
                        HighlightCard {
                            Text(L.t(!e.recurring ? "date_label" : nexts.count == 1 ? "next_label"
                                     : o.calendar == .hebrew ? "observe_hebrew" : "observe_gregorian"))
                                .font(Typography.rubik(12, .medium))
                            Text(f.relativeDays(today.days(until: o.date)).capitalizedFirst(L.locale)).font(Typography.rubik(24, .medium))
                            Text(f.occurrenceDates(o, first: .gregorian).joined(separator: " · ")).font(Typography.rubik(14))
                            if o.startsEveBefore {
                                Text(L.f("conv_eve_note", f.gregorianDayMonth(o.date.adding(days: -1)))).font(Typography.rubik(12))
                            }
                            if let age = f.agePhrase(e.type, o.yearsSince) { Text(age).font(Typography.rubik(14, .semibold)) }
                        }
                    }
                    if nexts.isEmpty && !e.recurring {
                        HighlightCard { Text(L.t("event_passed")) }
                    }
                }
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
                .listRowInsets(EdgeInsets(top: 6, leading: 0, bottom: 6, trailing: 0))

                Section(L.t("original_date")) {
                    if let g = e.date.gregorian {
                        InfoLine(label: L.t("gregorian_date"),
                                 value: f.gregorianParts(g) + (e.date.afterSunset ? " · " + L.t("after_sunset").lowercased() : ""))
                    }
                    if let h = e.date.hebrew { InfoLine(label: L.t("hebrew_date"), value: f.hebrewParts(h)) }
                    if e.recurring {
                        InfoLine(label: L.t("observe_title"),
                                 value: L.t(e.observeBoth ? "observe_both" : e.observeBy == .hebrew ? "observe_hebrew" : "observe_gregorian"))
                    } else {
                        InfoLine(label: L.t("field_recurring"), value: L.t("recurring_no"))
                    }
                }

                if e.type == .birthday, let h = e.date.hebrew, h.year != nil {
                    ForEach(MitzvahCalculator.mitzvahHints(birth: h, today: today), id: \.type) { hint in
                        Section {
                            Text(L.f(hint.type == .barMitzvah ? "hint_bar" : "hint_bat", f.hebrewDate(hint.hebrewDate), f.gregorianShortWeekday(hint.date)))
                            if canEdit {
                                NavigationLink(value: Route.editor(EditorSeed(calendar: .hebrew, year: hint.hebrewDate.year,
                                    hebrewMonth: hint.hebrewDate.month, day: hint.hebrewDate.day, type: hint.type, title: e.title, listId: e.listId))) {
                                    Text(L.t("add_as_event")).foregroundStyle(Palette.primary)
                                }
                            }
                        }
                        .listRowBackground(Palette.secondaryContainer)
                    }
                }

                // Первый год после смерти (B-26).
                if let milestones = MilestoneEvents.firstYear(e, today: today, policy) {
                    Section {
                        ForEach(milestones, id: \.milestone) { m in
                            let days = today.days(until: m.date)
                            HStack {
                                VStack(alignment: .leading) {
                                    Text(MilestoneEvents.label(m.milestone))
                                    Text("\(f.hebrewDate(m.hebrewDate)) · \(f.gregorianShortWeekday(m.date))")
                                        .font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                                }
                                Spacer()
                                Text(days < 0 ? L.t("milestone_passed") : f.relativeDays(days)).font(Typography.rubik(12, .medium))
                                    .foregroundStyle(days < 0 ? Palette.onSurfaceVariant : Palette.primary)
                            }
                        }
                    } header: { Text(L.t("first_year_title")) } footer: { Text(L.t("first_year_note")) }
                }

                if let note = e.note { Section(L.t("field_note")) { Text(note) } }
                if !canEdit {
                    Section { Text(L.t("read_only_list")).font(Typography.rubik(13)).foregroundStyle(Palette.onSurfaceVariant) }
                        .listRowBackground(Color.clear)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Palette.background)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if canEdit {
                    ToolbarItem(placement: .topBarTrailing) {
                        NavigationLink(value: Route.editor(EditorSeed(id: e.id))) { Image(systemName: "pencil") }.accessibilityLabel(L.t("edit"))
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(role: .destructive) { confirmDelete = true } label: { Image(systemName: "trash") }.accessibilityLabel(L.t("delete"))
                    }
                }
            }
            .confirmationDialog(L.f("delete_event_q", e.title), isPresented: $confirmDelete, titleVisibility: .visible) {
                Button(L.t("delete"), role: .destructive) { Task { await model.data.delete(eventId: e.id); dismiss() } }
                Button(L.t("cancel"), role: .cancel) {}
            }
            .ledorScreen()
        } else {
            Color.clear.onAppear { dismiss() }
        }
    }
}

struct InfoLine: View {
    let label: String
    let value: String
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(Typography.rubik(12, .medium)).foregroundStyle(Palette.onSurfaceVariant)
            Text(value).font(Typography.rubik(16))
        }
    }
}
