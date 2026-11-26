import AppCore
import BackgroundTasks
import Domain
import UIKit
import UserNotifications

/// Состояние напоминаний для баннера и настроек (`09-ios-notes.md` §7).
struct ReminderHealth: Equatable {
    var notificationsOff = false
    var backgroundRefreshOff = false
    /// Очередь из 60 уведомлений покрывает меньше двух недель — открывать приложение раз в N дней.
    var openEveryDays: Int?
    var hasIssues: Bool { notificationsOff || backgroundRefreshOff || openEveryDays != nil }
}

/// Напоминания на iOS — очередь локальных уведомлений: iOS хранит не более 64, ставятся ближайшие 60.
/// Очередь перестраивается целиком при запуске, возврате, правках, синхронизации и в фоне (`BGAppRefreshTask`).
final class ReminderNotifications: Sendable {
    static let queueLimit = 60
    static let refreshTaskId = "org.ledor.refresh"
    private var center: UNUserNotificationCenter { .current() }

    func requestPermission() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
    }

    func rearm(own: [Event], all: [Event], settings: AppSettings, city: City?) async {
        let zone = TimeZone.current
        let now = Date()
        let today = Zoned.civilDate(now, zone)
        let sunset: any SunsetProvider = city.map { CitySunsetProvider(city: $0) } ?? DefaultSunsetProvider(zone: zone)
        // Вехи первого года напоминают как обычные события (B-26).
        let events = own + MilestoneEvents.build(own, today: today, settings.adarPolicy)
        // Праздники и Шабат — только накануне вечером (B-25).
        let holidays = settings.holidays.notify ? HolidayEvents.build(
            settings.holidays, city: city, from: today.adding(days: -2), to: today.adding(days: 400),
            shabbatFrom: today.adding(days: -2), shabbatTo: today.adding(days: 400), nextDays: false,
            yizkor: all.contains { $0.type == .yahrzeit }) : []
        var eveOnly = settings.reminders
        eveOnly.eveEnabled = true
        eveOnly.dayEnabled = false
        let to = now.addingTimeInterval(400 * 86400)
        let plan = (ReminderPlanner.plan(events, settings.reminders, sunset, zone, settings.adarPolicy, from: now, to: to)
            + ReminderPlanner.plan(holidays, eveOnly, sunset, zone, settings.adarPolicy, from: now, to: to))
            .sorted { $0.triggerAt < $1.triggerAt }
            .prefix(Self.queueLimit)
        let byId = Dictionary((events + holidays).map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })

        center.removeAllPendingNotificationRequests()
        for r in plan {
            guard let e = byId[r.eventId] else { continue }
            let (title, body) = texts(e, r)
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default
            content.threadIdentifier = "reminders"
            content.userInfo = ["eventId": e.id]
            let comps = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: r.triggerAt)
            let request = UNNotificationRequest(identifier: r.notificationId, content: content,
                                                trigger: UNCalendarNotificationTrigger(dateMatching: comps, repeats: false))
            try? await center.add(request)
        }
        let defaults = UserDefaults.standard
        if plan.count == Self.queueLimit, let last = plan.last {
            defaults.set(last.triggerAt.timeIntervalSince(now), forKey: "queueCoverage")
        } else {
            defaults.removeObject(forKey: "queueCoverage")
        }
    }

    /// Тексты — как `NotificationFactory` на Android.
    func texts(_ e: Event, _ r: PlannedReminder) -> (String, String) {
        let f = Formatters()
        let type = f.typeLabel(e, r.yearsSince)
        let hebrew = HebrewCalendarMath.fromGregorian(r.occurrenceDate)
        let age = f.agePhrase(e.type, r.yearsSince)
        func join(_ parts: [String?]) -> String { parts.compactMap { $0 }.joined(separator: " ") }
        if e.listId == holidaysListId {
            // Пост «с рассвета» начинается завтра утром, остальные праздники — сегодня вечером.
            let key = HolidayEvents.holidayOf(e.id)?.dawn == true ? "notif_holiday_tomorrow_title" : "notif_holiday_eve_title"
            return (L.f(key, e.title), join([L.f("notif_eve_starts", f.hebrewDate(hebrew)), HolidayEvents.notificationLine(e)]))
        }
        if r.kind == .day {
            // Главная дата — первой (D-54).
            let dates = r.calendar == .gregorian ? "\(f.gregorian(r.occurrenceDate)) · \(f.hebrewDate(hebrew))."
                : "\(f.hebrewDate(hebrew)) · \(f.gregorian(r.occurrenceDate))."
            return (L.f("notif_day_title", type, e.title), join([dates, age]))
        }
        if r.calendar == .gregorian && e.recurring {
            return (L.f("notif_eve_greg_title", type, e.title), join([f.gregorian(r.occurrenceDate) + ".", age]))
        }
        if e.type == .yahrzeit {
            return (L.f("notif_eve_yahrzeit_title", e.title), join([L.f("notif_eve_starts", f.hebrewDate(hebrew)), L.t("notif_candle"), age]))
        }
        return (L.f("notif_eve_title", type, e.title), join([L.f("notif_eve_starts", f.hebrewDate(hebrew)), age]))
    }

    func sendTest() async {
        guard await requestPermission() else { return }
        let content = UNMutableNotificationContent()
        content.title = L.t("notif_test_title")
        content.body = L.t("notif_test_text")
        content.sound = .default
        try? await center.add(UNNotificationRequest(identifier: "test", content: content,
                                                    trigger: UNTimeIntervalNotificationTrigger(timeInterval: 2, repeats: false)))
    }

    func health(settings: AppSettings) async -> ReminderHealth {
        guard settings.reminders.anyEnabled || settings.holidays.notify else { return ReminderHealth() }
        let status = await center.notificationSettings().authorizationStatus
        var h = ReminderHealth()
        h.notificationsOff = status == .denied
        h.backgroundRefreshOff = await MainActor.run { UIApplication.shared.backgroundRefreshStatus != .available }
        let coverage = UserDefaults.standard.double(forKey: "queueCoverage")
        if coverage > 0 && coverage < 14 * 86400 { h.openEveryDays = max(1, Int(coverage / 86400)) }
        return h
    }

    /// Фоновое обновление — не чаще раза в сутки; запустит ли его система, решает она.
    static func scheduleRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: refreshTaskId)
        request.earliestBeginDate = Date().addingTimeInterval(24 * 3600)
        try? BGTaskScheduler.shared.submit(request)
    }
}
