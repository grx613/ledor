import AppCore
import SwiftUI

/// Четыре вкладки как на Android: Календарь, События, Конвертер, Ещё; поверх — брендовая заставка 1500 мс.
struct RootView: View {
    @Environment(AppModel.self) private var model
    @Environment(\.scenePhase) private var phase
    @State private var splash = true
    @State private var tab = 0
    @State private var paths: [[Route]] = [[], [], [], []]

    var body: some View {
        TabView(selection: $tab) {
            stack(0) { CalendarScreen(path: $paths[0]) }
                .tabItem { Label(L.t("tab_calendar"), systemImage: "calendar") }.tag(0)
            stack(1) { EventsScreen(path: $paths[1]) }
                .tabItem { Label(L.t("tab_events"), systemImage: "list.bullet") }.tag(1)
            stack(2) { ConverterScreen() }
                .tabItem { Label(L.t("tab_converter"), systemImage: "arrow.left.arrow.right") }.tag(2)
            stack(3) { MoreScreen() }
                .tabItem { Label(L.t("tab_more"), systemImage: "line.3.horizontal") }.tag(3)
        }
        .tint(Palette.primary)
        .overlay {
            if splash { BrandSplash().transition(.opacity) }
        }
        .task {
            try? await Task.sleep(for: .milliseconds(1500))
            withAnimation(.easeOut(duration: 0.25)) { splash = false }
        }
        .onChange(of: phase, initial: true) { _, p in
            if p == .active { Task { await model.onActive() } }
            if p == .background { ReminderNotifications.scheduleRefresh() }
        }
        .onReceive(NotificationCenter.default.publisher(for: .openEvent)) { note in
            guard let id = note.object as? String else { return }
            tab = 0
            paths[0] = [.event(id)]
        }
        .preferredColorScheme(model.settings.themeMode == .light ? .light : model.settings.themeMode == .dark ? .dark : nil)
    }

    private func stack<Content: View>(_ i: Int, @ViewBuilder _ content: () -> Content) -> some View {
        NavigationStack(path: $paths[i]) {
            content().navigationDestination(for: Route.self) { RouteView(route: $0, path: $paths[i]) }
        }
    }
}

/// Экран по маршруту.
struct RouteView: View {
    let route: Route
    @Binding var path: [Route]

    var body: some View {
        switch route {
        case .event(let id):
            // У праздника нет своей карточки — открывается список праздников; у вехи — карточка йорцайта.
            if HolidayEvents.isBuiltIn(id) { HolidaysScreen() }
            else { EventDetailScreen(id: MilestoneEvents.isMilestone(id) ? MilestoneEvents.parentId(id) : id) }
        case .editor(let seed): EventEditorScreen(seed: seed)
        case .lists: ListsScreen()
        case .listCreate(let moveLocal): ListCreateScreen(moveLocal: moveLocal, path: $path)
        case .listJoin(let name): ListJoinScreen(initialName: name)
        case .listDetail(let id): ListDetailScreen(id: id, path: $path)
        case .settings: SettingsScreen()
        case .cityPicker: CityPickerScreen()
        case .about: AboutScreen()
        case .feedback(let prefill): FeedbackScreen(prefill: prefill)
        case .holidays: HolidaysScreen()
        }
    }
}

extension Notification.Name {
    /// Нажатие на уведомление: открыть событие.
    static let openEvent = Notification.Name("org.ledor.openEvent")
}
