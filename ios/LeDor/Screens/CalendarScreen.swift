import AppCore
import Domain
import SwiftUI

private typealias M = HebrewCalendarMath

/// Месяц сетки: заголовки и дни (с хвостами соседних месяцев до полных недель).
struct MonthGridModel {
    let titlePrimary: String
    let titleSecondary: String
    let cells: [CivilDate]
    let first: CivilDate
    let last: CivilDate
    func inMonth(_ d: CivilDate) -> Bool { d >= first && d <= last }
}

func gridCells(first: CivilDate, last: CivilDate, weekStartsOnSunday: Bool) -> [CivilDate] {
    let firstDow = weekStartsOnSunday ? 7 : 1
    let lead = (first.isoWeekday - firstDow + 7) % 7
    let start = first.adding(days: -lead)
    let weeks = (start.days(until: last) + 1 + 6) / 7
    return (0..<weeks * 7).map { start.adding(days: $0) }
}

/// Наступления событий в окне — по дням (Шабат в сетке не отмечается: суббота и так выделена).
func occurrencesIn(_ events: [Event], from: CivilDate, to: CivilDate, _ policy: YahrzeitAdarPolicy) -> [CivilDate: [(Event, Occurrence)]] {
    let pairs = events.filter { !HolidayEvents.isShabbat($0.id) }
        .flatMap { e in OccurrenceCalculator.between(e, from: from, to: to, policy).map { (e, $0) } }
        .sorted { $0.0.title < $1.0.title }
    return Dictionary(grouping: pairs, by: { $0.1.date })
}

struct CalendarScreen: View {
    @Environment(AppModel.self) private var model
    @Binding var path: [Route]
    @State private var offset = 0
    @State private var selected: CivilDate?
    @State private var onboarding = false

    private func monthModel(_ off: Int) -> MonthGridModel {
        let f = Formatters()
        let today = model.today
        let sunday = model.settings.weekStartsOnSunday
        if model.settings.gridMode == .gregorian {
            let total = today.year * 12 + today.month - 1 + off
            let y = total / 12, m = total % 12 + 1
            let first = CivilDate(year: y, month: m, day: 1)!
            let last = CivilDate(year: y, month: m, day: CivilDate.daysInMonth(year: y, month: m))!
            let hf = M.fromGregorian(first), hl = M.fromGregorian(last)
            let sec = hf.month == hl.month ? "\(f.monthNom(hf.month)) \(f.hebrewYear(hf.year))"
                : hf.year == hl.year ? "\(f.monthNom(hf.month)) – \(f.monthNom(hl.month)) \(f.hebrewYear(hl.year))"
                : "\(f.monthNom(hf.month)) \(f.hebrewYear(hf.year)) – \(f.monthNom(hl.month)) \(f.hebrewYear(hl.year))"
            return MonthGridModel(titlePrimary: f.gregorianMonthTitle(y, m), titleSecondary: sec,
                                  cells: gridCells(first: first, last: last, weekStartsOnSunday: sunday), first: first, last: last)
        }
        let h = M.fromGregorian(today)
        var (y, m) = (h.year, h.month)
        for _ in 0..<abs(off) { (y, m) = off > 0 ? M.nextMonth(y, m) : M.previousMonth(y, m) }
        let first = M.firstDayOf(y, m)
        let last = first.adding(days: M.daysInMonth(y, m) - 1)
        let sec = first.month == last.month ? f.gregorianMonthTitle(first.year, first.month)
            : "\(f.gregorianMonthName(first.month)) – \(f.gregorianMonthTitle(last.year, last.month))"
        return MonthGridModel(titlePrimary: f.hebrewMonthTitle(y, m), titleSecondary: sec,
                              cells: gridCells(first: first, last: last, weekStartsOnSunday: sunday), first: first, last: last)
    }

    var body: some View {
        let current = monthModel(offset)
        let occ = occurrencesIn(model.events, from: current.cells.first!, to: current.cells.last!, model.settings.adarPolicy)
        let f = Formatters()
        List {
            Section {
                if let notice = model.db.deletedListNotices.first {
                    InfoBanner(text: L.f("notice_list_deleted", notice)) { model.dismissNotices() }
                }
                if model.health.hasIssues && model.settings.healthWarningsEnabled {
                    HealthBanner()
                }
                MonthHeader(model: current, mode: model.settings.gridMode,
                            onPrev: { withAnimation { offset -= 1 } }, onNext: { withAnimation { offset += 1 } },
                            onMode: { mode in model.settings.gridMode = mode; offset = 0 })
                MonthGrid(model: current, mode: model.settings.gridMode, today: model.today, selected: selected,
                          weekStartsOnSunday: model.settings.weekStartsOnSunday, occ: occ) { d in
                    selected = selected == d ? nil : d
                }
                .gesture(DragGesture(minimumDistance: 30).onEnded { g in
                    guard abs(g.translation.width) > abs(g.translation.height) else { return }
                    let rtl = L.hebrewLetters
                    withAnimation { offset += (g.translation.width < 0) != rtl ? 1 : -1 }
                })
            }
            .listRowSeparator(.hidden)
            .listRowInsets(EdgeInsets(top: 4, leading: 8, bottom: 4, trailing: 8))
            .listRowBackground(Color.clear)

            if let day = selected {
                let items = occ[day] ?? []
                Section {
                    if items.isEmpty {
                        Text(L.t("no_events_day")).foregroundStyle(Palette.onSurfaceVariant)
                    }
                    ForEach(items, id: \.0.id) { e, o in
                        NavigationLink(value: Route.event(e.id)) {
                            EventRow(event: e, occurrence: o, daysUntil: model.today.days(until: o.date))
                        }
                    }
                    NavigationLink(value: Route.editor(EditorSeed(calendar: .gregorian, year: day.year, month: day.month, day: day.day))) {
                        Label(L.t("add_on_this_date"), systemImage: "plus")
                    }
                } header: {
                    HStack {
                        Text(L.f("events_of_day", f.gregorianDayMonthWeekday(day)) + " · " + f.hebrewDate(M.fromGregorian(day)))
                        Spacer()
                        Button(L.t("show_upcoming")) { selected = nil }.font(Typography.rubik(13, .medium))
                    }
                }
            } else {
                if model.events.allSatisfy({ $0.listId == holidaysListId }) {
                    Section {
                        EmptyState(text: L.t("empty_events")) {
                            Button(L.t("add_event")) { path.append(.editor(EditorSeed())) }.buttonStyle(.borderedProminent)
                            Button(L.t("join_list")) { path.append(.listJoin(nil)) }.buttonStyle(.bordered)
                        }
                    }
                    .listRowBackground(Color.clear)
                }
                let all = model.upcoming
                if !all.isEmpty {
                    let within = all.filter { $0.daysUntil <= 30 }
                    let shown = within.count >= 5 ? within : Array(all.prefix(5))
                    Section(L.t("upcoming_events")) {
                        ForEach(shown) { u in
                            NavigationLink(value: Route.event(u.event.id)) {
                                EventRow(event: u.event, occurrence: u.occurrence, daysUntil: u.daysUntil)
                            }
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .contentMargins(.bottom, 80, for: .scrollContent)
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(Text(verbatim: "LeDor"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button { withAnimation { offset = 0; selected = nil } } label: {
                    Image(systemName: "calendar.badge.clock")
                }.accessibilityLabel(L.t("today"))
            }
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: Route.lists) { Label(L.t("lists"), systemImage: "person.2").labelStyle(.titleAndIcon) }
            }
        }
        .overlay(alignment: .bottomTrailing) {
            Button {
                let d = selected
                path.append(.editor(d.map { EditorSeed(calendar: .gregorian, year: $0.year, month: $0.month, day: $0.day) } ?? EditorSeed()))
            } label: {
                Image(systemName: "plus").font(.system(size: 22, weight: .semibold)).foregroundStyle(.white)
                    .frame(width: 56, height: 56).background(Palette.Brand.tchelet, in: RoundedRectangle(cornerRadius: 16))
                    .shadow(radius: 3, y: 2)
            }
            .accessibilityLabel(L.t("add_event"))
            .padding(16)
        }
        .task {
            try? await Task.sleep(for: .milliseconds(1700))
            if !model.settings.onboardingImportShown { onboarding = true }
        }
        .alert(L.t("onboarding_title"), isPresented: $onboarding) {
            Button(L.t("yes")) { model.settings.onboardingImportShown = true; path.append(.listJoin(nil)) }
            Button(L.t("no"), role: .cancel) { model.settings.onboardingImportShown = true }
        } message: { Text(L.t("onboarding_text")) }
        .ledorScreen()
    }
}

private struct MonthHeader: View {
    let model: MonthGridModel
    let mode: GridMode
    let onPrev: () -> Void
    let onNext: () -> Void
    let onMode: @MainActor @Sendable (GridMode) -> Void

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Button(action: onPrev) { Image(systemName: "chevron.backward").frame(width: 44, height: 44) }
                    .accessibilityLabel(L.t("prev_month"))
                VStack(spacing: 2) {
                    Text(model.titlePrimary).font(Typography.rubik(22, .medium)).foregroundStyle(Palette.onSurface)
                    Text(model.titleSecondary).font(Typography.rubik(14)).foregroundStyle(Palette.secondary)
                }
                .frame(maxWidth: .infinity)
                .multilineTextAlignment(.center)
                Button(action: onNext) { Image(systemName: "chevron.forward").frame(width: 44, height: 44) }
                    .accessibilityLabel(L.t("next_month"))
            }
            .buttonStyle(.plain)
            .foregroundStyle(Palette.primary)
            Picker("", selection: Binding(get: { mode }, set: onMode)) {
                Text(L.t("grid_gregorian")).tag(GridMode.gregorian)
                Text(L.t("grid_hebrew")).tag(GridMode.hebrew)
            }
            .pickerStyle(.segmented)
        }
        .background(alignment: .topTrailing) {
            MagenDavidShape().stroke(Palette.Brand.gold.opacity(0.09), lineWidth: 2).frame(width: 150, height: 150).offset(x: 45, y: -55)
        }
        .clipped()
    }
}

private struct MonthGrid: View {
    let model: MonthGridModel
    let mode: GridMode
    let today: CivilDate
    let selected: CivilDate?
    let weekStartsOnSunday: Bool
    let occ: [CivilDate: [(Event, Occurrence)]]
    let onTap: (CivilDate) -> Void

    var body: some View {
        let f = Formatters()
        let firstDow = weekStartsOnSunday ? 7 : 1
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                ForEach(0..<7, id: \.self) { i in
                    let dow = (firstDow - 1 + i) % 7 + 1
                    Text(f.weekdayShort(dow)).font(Typography.rubik(11, .medium))
                        .foregroundStyle(dow == 6 ? Palette.primary : Palette.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.bottom, 4)
            ForEach(Array(stride(from: 0, to: model.cells.count, by: 7)), id: \.self) { start in
                HStack(spacing: 0) {
                    ForEach(model.cells[start..<start + 7], id: \.self) { d in
                        DayCell(date: d, mode: mode, inMonth: model.inMonth(d), isToday: d == today, isSelected: d == selected,
                                items: occ[d] ?? [], f: f)
                            .onTapGesture { onTap(d) }
                    }
                }
            }
        }
    }
}

private struct DayCell: View {
    let date: CivilDate
    let mode: GridMode
    let inMonth: Bool
    let isToday: Bool
    let isSelected: Bool
    let items: [(Event, Occurrence)]
    let f: Formatters

    var body: some View {
        let h = M.fromGregorian(date)
        let big = mode == .gregorian ? String(date.day) : String(h.day)
        let small = mode == .gregorian ? (h.day == 1 ? "1 \(f.monthShort(h.month))" : String(h.day))
            : (date.day == 1 ? "1 \(f.gregorianMonthName(date.month).prefix(3))" : String(date.day))
        let alpha: Double = inMonth ? 1 : 0.38
        let fg = isSelected ? Palette.onPrimary : Palette.onSurface
        VStack(spacing: 1) {
            Text(big).font(Typography.rubik(16, isToday ? .bold : .medium)).monospacedDigit().foregroundStyle(fg.opacity(alpha))
            Text(small).font(Typography.rubik(10)).foregroundStyle((isSelected ? fg : Palette.onSurfaceVariant).opacity(alpha)).lineLimit(1)
            HStack(spacing: 2) {
                ForEach(Array(items.prefix(3).enumerated()), id: \.offset) { _, pair in
                    if pair.0.listId == holidaysListId {
                        Rectangle().fill(isSelected ? fg : Palette.gold.opacity(alpha)).frame(width: 4, height: 4).rotationEffect(.degrees(45))
                    } else {
                        Circle().fill(isSelected ? fg : Palette.forType(pair.0.type).opacity(alpha)).frame(width: 5, height: 5)
                    }
                }
                if items.count > 3 { Text("+\(items.count - 3)").font(.system(size: 8)).foregroundStyle(fg.opacity(alpha)) }
            }
            .frame(height: 6)
        }
        .padding(.top, 6).padding(.bottom, 4)
        .frame(maxWidth: .infinity, minHeight: 58, alignment: .top)
        .background(RoundedRectangle(cornerRadius: 12).fill(
            isSelected ? Palette.primary : isToday ? Palette.secondaryContainer : date.isoWeekday == 6 ? Palette.shabbat : .clear))
        .overlay {
            if isToday {
                RoundedRectangle(cornerRadius: 12).stroke(Palette.gold, lineWidth: 1.5)
                MagenDavidShape().stroke(isSelected ? Palette.onPrimary : Palette.gold, lineWidth: 1.1)
                    .frame(width: 11, height: 11).frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing).padding(3)
            }
        }
        .padding(2)
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(f.gregorianDayMonth(date)), \(f.hebrewDayMonth(h.day, h.month))"
            + (items.isEmpty ? "" : ", " + L.p("events_count", items.count)))
        .accessibilityAddTraits(.isButton)
    }
}

/// Сообщение, которое можно закрыть.
struct InfoBanner: View {
    let text: String
    let onClose: () -> Void
    var body: some View {
        HStack(alignment: .top) {
            Image(systemName: "info.circle").foregroundStyle(Palette.primary)
            Text(text).font(Typography.rubik(14))
            Spacer()
            Button(action: onClose) { Image(systemName: "xmark") }.buttonStyle(.plain).accessibilityLabel(L.t("close"))
        }
        .padding(12)
        .background(Palette.primaryContainer, in: RoundedRectangle(cornerRadius: 12))
    }
}

/// Предупреждение: напоминания могут не прийти (выключены уведомления, фон или очередь коротка).
struct HealthBanner: View {
    @Environment(AppModel.self) private var model
    var body: some View {
        let h = model.health
        VStack(alignment: .leading, spacing: 6) {
            Text(L.t(h.notificationsOff ? "health_notif_title" : h.backgroundRefreshOff ? "health_refresh_title" : "health_banner_title"))
                .font(Typography.rubik(15, .semibold))
            Text(h.notificationsOff ? L.t("health_notif_text") : h.backgroundRefreshOff ? L.t("health_refresh_text")
                 : L.f("health_queue_text", h.openEveryDays ?? 1))
                .font(Typography.rubik(13))
            if h.notificationsOff || h.backgroundRefreshOff {
                Button(L.t("health_fix")) {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Palette.secondaryContainer, in: RoundedRectangle(cornerRadius: 12))
        .foregroundStyle(Palette.onSecondaryContainer)
    }
}
