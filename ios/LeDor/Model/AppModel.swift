import AppCore
import Domain
import Foundation
import Observation

/// Событие + ближайшее наступление + список.
struct UpcomingItem: Identifiable {
    let event: Event
    let occurrence: Occurrence
    let list: ListRecord?
    let daysUntil: Int
    /// У события «по обоим календарям» две строки — ключ включает дату.
    var id: String { "\(event.id)@\(occurrence.date.iso)" }
}

/// Общие данные всех экранов (как `AppViewModel` + `AppContainer` на Android).
@Observable @MainActor
final class AppModel {
    let data: DataService
    let catalog = CityCatalog.builtIn
    let reminders = ReminderNotifications()
    private let settingsStore = SettingsStore()

    private(set) var db = DatabaseState()
    private(set) var today = Zoned.civilDate(Date(), .current)
    /// События пользователя + вехи первого года + праздники (на год назад и на десять вперёд).
    private(set) var events: [Event] = []
    private(set) var upcoming: [UpcomingItem] = []
    private(set) var health = ReminderHealth()

    var settings: AppSettings {
        didSet {
            guard settings != oldValue else { return }
            settingsStore.save(settings)
            rebuild()
            if settings.reminders != oldValue.reminders || settings.cityId != oldValue.cityId
                || settings.adarPolicy != oldValue.adarPolicy || settings.holidays != oldValue.holidays { scheduleRearm() }
        }
    }

    private var rearmTask: Task<Void, Never>?
    private var pushTask: Task<Void, Never>?

    init() {
        settings = settingsStore.load()
        let store = LocalStore(fileURL: LocalStore.defaultURL)
        let rpc = RpcClient(baseURL: AppConfig.apiURL, apiKey: AppConfig.apiKey)
        // Правки из хранилища приходят через поток `changes`; здесь — только отправка облачных правок.
        let pushes = AsyncStream<String>.makeStream()
        data = DataService(store: store, rpc: rpc) { cloudListId in if let cloudListId { pushes.continuation.yield(cloudListId) } }
        Task { [weak self] in
            for await state in await store.changes() { self?.apply(state) }
        }
        Task { [weak self] in
            for await listId in pushes.stream {
                guard let self else { return }
                pushTask?.cancel()
                pushTask = Task { try? await Task.sleep(for: .seconds(2)); if !Task.isCancelled { _ = await self.data.syncList(listId) } }
            }
        }
    }

    private func apply(_ state: DatabaseState) {
        let notifyChanged = state.lists.map { "\($0.id)\($0.notifyEnabled)" } != db.lists.map { "\($0.id)\($0.notifyEnabled)" }
        let eventsChanged = state.events != db.events
        db = state
        rebuild()
        if notifyChanged || eventsChanged { scheduleRearm() }
    }

    var city: City? { catalog.byId(settings.cityId) }
    var lists: [ListRecord] { db.sortedLists }
    func list(_ id: String) -> ListRecord? { db.list(id) }
    var ownEvents: [Event] { db.visibleEvents.map(\.event) }
    func event(_ id: String) -> Event? { events.first { $0.id == id } }
    func isPending(_ id: String) -> Bool { db.event(id).map { $0.syncState != .synced } ?? false }

    private func rebuild() {
        let own = ownEvents
        let t = today
        events = own + MilestoneEvents.build(own, today: t, settings.adarPolicy)
            + HolidayEvents.build(settings.holidays, city: city, from: CivilDate(year: t.year - 1, month: t.month, day: 1)!,
                                  to: CivilDate(year: t.year + 10, month: t.month, day: 1)!, shabbatFrom: t, shabbatTo: t.adding(days: 13),
                                  nextDays: true, yizkor: own.contains { $0.type == .yahrzeit })
        var shabbatShown = false
        // «По обоим» календарям — две строки: ближайшая дата по каждому календарю (B-28).
        upcoming = events.flatMap { e in
            OccurrenceCalculator.nextEach(e, today: t, settings.adarPolicy).map { o in
                UpcomingItem(event: e, occurrence: o, list: db.list(e.listId), daysUntil: t.days(until: o.date))
            }
        }
        .sorted { ($0.occurrence.date, $0.event.title) < ($1.occurrence.date, $1.event.title) }
        .filter { u in
            let id = u.event.id
            guard HolidayEvents.isBuiltIn(id) else { return true }
            // Праздники — на год вперёд; следующие дни — только сегодняшний; Шабат — только ближайший.
            if u.daysUntil > 366 { return false }
            if HolidayEvents.isNextDay(id) { return u.daysUntil == 0 }
            if HolidayEvents.isShabbat(id) { defer { shabbatShown = true }; return !shabbatShown }
            return true
        }
    }

    /// При каждом открытии приложения (запуск и возврат из фона): новая дата, проверка разрешений, перестройка уведомлений
    /// и синхронизация общих списков, если с прошлой попытки прошло больше 5 минут (B-39).
    func onActive() async {
        let now = Zoned.civilDate(Date(), .current)
        if now != today { today = now; rebuild() }
        await rearmNow()
        health = await reminders.health(settings: settings)
        if shouldSyncOnOpen(configured: data.cloudConfigured, now: Date(), lastAttempt: db.lastSyncAttempt),
           db.lists.contains(where: \.isCloud) {
            _ = await data.syncAll()
        }
    }

    private func scheduleRearm() {
        rearmTask?.cancel()
        rearmTask = Task { try? await Task.sleep(for: .milliseconds(500)); if !Task.isCancelled { await rearmNow() } }
    }

    func rearmNow() async {
        let notifyLists = Set(db.lists.filter(\.notifyEnabled).map(\.id))
        let own = ownEvents.filter { notifyLists.contains($0.listId) }
        await reminders.rearm(own: own, all: ownEvents, settings: settings, city: city)
        health = await reminders.health(settings: settings)
    }

    func refreshHealth() async { health = await reminders.health(settings: settings) }
    func dismissNotices() { Task { await data.clearNotices() } }
}
