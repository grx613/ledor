import BackgroundTasks
import SwiftUI
import UserNotifications

@main
struct LeDorApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate
    @State private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView().environment(model)
        }
        .backgroundTask(.appRefresh(ReminderNotifications.refreshTaskId)) {
            // Фон: синхронизация общих списков и перестройка очереди уведомлений; следующий запуск — не раньше чем через сутки.
            ReminderNotifications.scheduleRefresh()
            _ = await model.data.syncAll()
            await model.rearmNow()
        }
    }
}

/// Нажатие на уведомление открывает карточку события.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse,
                                            withCompletionHandler completionHandler: @escaping () -> Void) {
        let id = response.notification.request.content.userInfo["eventId"] as? String
        Task { @MainActor in
            if let id { NotificationCenter.default.post(name: .openEvent, object: id) }
        }
        completionHandler()
    }

    nonisolated func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                            withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .list])
    }
}
