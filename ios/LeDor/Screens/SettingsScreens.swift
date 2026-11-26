import AppCore
import Domain
import SwiftUI
import UIKit

/// «Ещё»: списки, настройки, отзыв, «О программе».
struct MoreScreen: View {
    var body: some View {
        List {
            item(.lists, "person.2", L.t("manage_lists"), L.t("manage_lists_sub"))
            item(.holidays, "star", L.t("holidays_title"), L.t("holidays_sub"))
            item(.settings, "gearshape", L.t("settings"), L.t("settings_sub"))
            item(.feedback(nil), "envelope", L.t("feedback_title"), L.t("feedback_sub"))
            item(.about, "info.circle", L.t("about"), L.t("about_author"))
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("tab_more"))
        .navigationBarTitleDisplayMode(.inline)
        .ledorScreen()
    }

    private func item(_ route: Route, _ icon: String, _ title: String, _ sub: String) -> some View {
        NavigationLink(value: route) {
            Label { subtitled(title, sub) } icon: { Image(systemName: icon).foregroundStyle(Palette.primary) }
        }
    }
}

@MainActor
private func timeBinding(_ get: @escaping @MainActor @Sendable () -> TimeOfDay,
                         _ set: @escaping @MainActor @Sendable (TimeOfDay) -> Void) -> Binding<Date> {
    Binding(get: { Zoned.instant(CivilDate(year: 2001, month: 1, day: 1)!, get(), .current) },
            set: { set(Zoned.timeOfDay($0, .current)) })
}

struct SettingsScreen: View {
    @Environment(AppModel.self) private var model

    var body: some View {
        @Bindable var m = model
        let r = model.settings.reminders
        let f = Formatters()
        let zone = TimeZone.current
        let sunset: any SunsetProvider = model.city.map { CitySunsetProvider(city: $0) } ?? DefaultSunsetProvider(zone: zone)
        Form {
            Section(L.t("set_eve_title")) {
                Toggle(L.t("set_enabled"), isOn: $m.settings.reminders.eveEnabled)
                Picker("", selection: $m.settings.reminders.eveMode) {
                    Text(L.t("set_eve_before_sunset")).tag(EveMode.beforeSunset)
                    Text(L.t("set_eve_fixed")).tag(EveMode.fixedTime)
                }
                .pickerStyle(.inline).labelsHidden().disabled(!r.eveEnabled)
                if r.eveMode == .beforeSunset {
                    Text(L.f("set_before_sunset_value", r.eveMinutesBeforeSunset / 60, r.eveMinutesBeforeSunset % 60))
                        .font(Typography.rubik(16, .medium)).foregroundStyle(Palette.primary)
                    Slider(value: Binding(get: { Double(r.eveMinutesBeforeSunset) },
                                          set: { m.settings.reminders.eveMinutesBeforeSunset = Int($0 / 30) * 30 }),
                           in: 0...360, step: 30).disabled(!r.eveEnabled)
                } else {
                    DatePicker(L.t("set_time"), selection: timeBinding({ r.eveFixedTime }, { model.settings.reminders.eveFixedTime = $0 }),
                               displayedComponents: .hourAndMinute).disabled(!r.eveEnabled)
                }
            }
            Section(L.t("set_day_title")) {
                Toggle(L.t("set_enabled"), isOn: $m.settings.reminders.dayEnabled)
                DatePicker(L.t("set_time"), selection: timeBinding({ r.dayTime }, { model.settings.reminders.dayTime = $0 }),
                           displayedComponents: .hourAndMinute).disabled(!r.dayEnabled)
            }
            Section {
                NavigationLink(value: Route.cityPicker) {
                    if let c = model.city { Text(cityName(c)) } else { subtitled(L.t("city_none"), L.t("city_none_hint")) }
                }
            } header: { Text(L.t("set_city_title")) } footer: {
                if r.eveEnabled {
                    let today = model.today
                    Text(L.f("set_preview", f.time(sunset.sunset(today)), f.time(ReminderPlanner.eveTrigger(today.adding(days: 1), r, sunset, zone))))
                }
            }
            Section(L.t("set_health_title")) {
                let h = model.health
                if !h.hasIssues {
                    Label(L.t("health_all_ok"), systemImage: "checkmark.circle.fill").foregroundStyle(Palette.success)
                }
                if h.notificationsOff { issueRow(L.t("health_notif_title"), L.t("health_notif_text")) }
                if h.backgroundRefreshOff { issueRow(L.t("health_refresh_title"), L.t("health_refresh_text")) }
                if let days = h.openEveryDays { Text(L.f("health_queue_text", days)).font(Typography.rubik(13)) }
                if h.notificationsOff || h.backgroundRefreshOff {
                    Button(L.t("health_open_setting")) { openAppSettings() }
                }
                Button(L.t("set_test_notification")) { Task { await model.reminders.sendTest(); await model.refreshHealth() } }
            }
            Section {
                Button(L.t("set_language")) { openAppSettings() }
            } footer: { Text(L.t("lang_ios_hint")) }
            Section(L.t("set_view_title")) {
                Picker(L.t("set_week_start"), selection: $m.settings.weekStartsOnSunday) {
                    Text(L.t("sunday")).tag(true)
                    Text(L.t("monday")).tag(false)
                }
                Picker(L.t("set_theme"), selection: $m.settings.themeMode) {
                    Text(L.t("theme_system")).tag(ThemeMode.system)
                    Text(L.t("theme_light")).tag(ThemeMode.light)
                    Text(L.t("theme_dark")).tag(ThemeMode.dark)
                }
            }
            Section {
                Picker("", selection: $m.settings.adarPolicy) {
                    Text(L.t("set_adar_i")).tag(YahrzeitAdarPolicy.adarI)
                    Text(L.t("set_adar_ii")).tag(YahrzeitAdarPolicy.adarII)
                }
                .pickerStyle(.inline).labelsHidden()
            } header: { Text(L.t("set_adar_title")) } footer: { Text(L.t("set_adar_hint")) }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("settings"))
        .navigationBarTitleDisplayMode(.inline)
        .task { await model.refreshHealth() }
        .ledorScreen()
    }

    private func issueRow(_ title: String, _ text: String) -> some View {
        Label { subtitled(title, text) } icon: {
            Image(systemName: "exclamationmark.triangle.fill").foregroundStyle(Palette.error)
        }
    }
}

@MainActor
func openAppSettings() {
    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
}

struct CityPickerScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""

    var body: some View {
        List {
            Button { model.settings.cityId = nil; dismiss() } label: { subtitled(L.t("city_none"), L.t("city_none_hint")) }
                .foregroundStyle(Palette.onSurface)
            ForEach(model.catalog.search(query)) { c in
                Button { model.settings.cityId = c.id; dismiss() } label: {
                    HStack {
                        subtitled(cityName(c), "\(c.nameEn) · \(c.nameHe)")
                        Spacer()
                        if c.id == model.settings.cityId { Image(systemName: "checkmark").foregroundStyle(Palette.primary) }
                    }
                }
                .foregroundStyle(Palette.onSurface)
            }
        }
        .searchable(text: $query, prompt: L.t("city_search"))
        .navigationTitle(L.t("set_city_title"))
        .navigationBarTitleDisplayMode(.inline)
        .ledorScreen()
    }
}

struct AboutScreen: View {
    @Environment(\.openURL) private var openURL
    @State private var licenses = false

    var body: some View {
        List {
            Section {
                VStack(spacing: 8) {
                    LogoBlock(onDark: false, markSize: 88)
                    Text(L.f("version", AppConfig.version)).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
            }
            .listRowBackground(Color.clear)
            Section { Text(L.t("about_text")) }
            Section {
                Text(L.t("about_author")).font(Typography.rubik(15, .medium))
                link(L.t("about_author_channel"), AppConfig.authorChannel)
            }
            Section {
                link(L.t("about_site"), "\(AppConfig.site)/\(L.siteLang)/")
                link(L.t("about_privacy"), "\(AppConfig.site)/\(L.siteLang)/privacy/")
                link(L.t("about_source"), AppConfig.sourceCode)
                NavigationLink(value: Route.feedback(nil)) { Text(L.t("feedback_title")).foregroundStyle(Palette.primary) }
                Button(L.t("about_licenses")) { withAnimation { licenses.toggle() } }
                if licenses { Text(L.t("licenses_text_ios")).font(Typography.rubik(12)) }
            }
            Section { Wordmark(onDark: false).frame(maxWidth: .infinity) }.listRowBackground(Color.clear)
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("about"))
        .navigationBarTitleDisplayMode(.inline)
        .ledorScreen()
    }

    private func link(_ title: String, _ url: String) -> some View {
        Button(title) { if let u = URL(string: url) { openURL(u) } }
    }
}

/// Отзыв разработчику (B-2): текст, необязательный контакт, по согласию — техданные без событий и ПИН-кодов.
struct FeedbackScreen: View {
    @Environment(AppModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let prefill: String?
    @State private var text = ""
    @State private var contact = ""
    @State private var attachMeta = true
    @State private var sending = false
    @State private var sent = false
    @State private var error: String?
    @State private var loaded = false

    private static let messageMax = 2000, contactMax = 200

    var body: some View {
        let device = UIDevice.current.model
        let os = UIDevice.current.systemVersion
        Form {
            if sent {
                Section {
                    Text(L.t("feedback_thanks_title")).font(Typography.rubik(17, .medium))
                    Text(L.t("feedback_thanks_text"))
                }
                Button(L.t("done")) { dismiss() }
            } else {
                Section {
                    TextField(L.t("feedback_message"), text: Binding(get: { text }, set: { text = String($0.prefix(Self.messageMax)); error = nil }),
                              axis: .vertical).lineLimit(5...12)
                } header: { Text(L.t("feedback_intro")).textCase(nil) } footer: { Text("\(text.count) / \(Self.messageMax)") }
                Section {
                    TextField(L.t("feedback_contact"), text: Binding(get: { contact }, set: { contact = String($0.prefix(Self.contactMax)) }))
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                } footer: { Text(L.t("feedback_contact_hint")) }
                Section {
                    Toggle(isOn: $attachMeta) {
                        subtitled(L.t("feedback_attach_meta"), L.f("feedback_meta_detail_ios", AppConfig.version, device, os))
                    }
                }
                if let error { Section { Text(error).foregroundStyle(Palette.error) } }
                Section {
                    Button(L.t(sending ? "feedback_sending" : "feedback_send")) { send(device: device, os: os) }
                        .disabled(sending || text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                } footer: { Text(L.t("feedback_privacy")) }
            }
        }
        .scrollContentBackground(.hidden)
        .background(Palette.background)
        .navigationTitle(L.t("feedback_title"))
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { if !loaded { loaded = true; text = prefill.map { $0 + "\n\n" } ?? "" } }
        .ledorScreen()
    }

    private func send(device: String, os: String) {
        sending = true
        error = nil
        let meta = attachMeta ? ["app_version": AppConfig.version, "device": device, "os_version": "iOS \(os)", "locale": L.langTag] : nil
        let c = contact.trimmingCharacters(in: .whitespacesAndNewlines)
        Task {
            switch await model.data.rpc.submitFeedback(message: text.trimmingCharacters(in: .whitespacesAndNewlines),
                                                       contact: c.isEmpty ? nil : c, meta: meta) {
            case .ok: sent = true
            case .error(let code, _, _): error = L.t(code == "RATE_LIMITED" ? "feedback_rate_limited" : "feedback_failed")
            case .failure(let kind): error = L.t(kind == .network ? "feedback_no_network" : "feedback_failed")
            }
            sending = false
        }
    }
}
