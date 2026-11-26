import Domain
import Foundation

public enum ThemeMode: String, Codable, Sendable, CaseIterable { case system = "SYSTEM", light = "LIGHT", dark = "DARK" }
public enum GridMode: String, Codable, Sendable { case gregorian = "GREGORIAN", hebrew = "HEBREW" }

/// Встроенный список «Еврейские праздники» (B-25).
public struct HolidaySettings: Codable, Hashable, Sendable {
    /// Показывать праздники в календаре и в событиях.
    public var enabled = true
    /// Напоминать накануне вечером.
    public var notify = true
    /// Шабат: напоминание в пятницу со временем зажигания свечей.
    public var shabbat = false
    public init() {}
}

/// Настройки приложения; значения по умолчанию — как на Android (`SettingsStore`).
public struct AppSettings: Codable, Hashable, Sendable {
    public var onboardingImportShown = false
    public var reminders = ReminderSettings()
    public var cityId: String?
    public var healthWarningsEnabled = true
    public var weekStartsOnSunday = true
    public var themeMode = ThemeMode.system
    public var gridMode = GridMode.hebrew
    public var adarPolicy = YahrzeitAdarPolicy.adarI
    public var lastListId: String?
    public var holidays = HolidaySettings()

    public init() {}

    /// Новые поля в старой записи берутся по умолчанию.
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let d = AppSettings()
        onboardingImportShown = try c.decodeIfPresent(Bool.self, forKey: .onboardingImportShown) ?? d.onboardingImportShown
        reminders = try c.decodeIfPresent(ReminderSettings.self, forKey: .reminders) ?? d.reminders
        cityId = try c.decodeIfPresent(String.self, forKey: .cityId)
        healthWarningsEnabled = try c.decodeIfPresent(Bool.self, forKey: .healthWarningsEnabled) ?? d.healthWarningsEnabled
        weekStartsOnSunday = try c.decodeIfPresent(Bool.self, forKey: .weekStartsOnSunday) ?? d.weekStartsOnSunday
        themeMode = try c.decodeIfPresent(ThemeMode.self, forKey: .themeMode) ?? d.themeMode
        gridMode = try c.decodeIfPresent(GridMode.self, forKey: .gridMode) ?? d.gridMode
        adarPolicy = try c.decodeIfPresent(YahrzeitAdarPolicy.self, forKey: .adarPolicy) ?? d.adarPolicy
        lastListId = try c.decodeIfPresent(String.self, forKey: .lastListId)
        holidays = try c.decodeIfPresent(HolidaySettings.self, forKey: .holidays) ?? d.holidays
    }
}

/// Настройки — одна JSON-запись в `UserDefaults`.
public struct SettingsStore: Sendable {
    private let key = "appSettings"
    private let suite: String?

    public init(suite: String? = nil) { self.suite = suite }

    private var defaults: UserDefaults { suite.flatMap(UserDefaults.init(suiteName:)) ?? .standard }

    public func load() -> AppSettings {
        defaults.data(forKey: key).flatMap { try? JSONDecoder().decode(AppSettings.self, from: $0) } ?? AppSettings()
    }

    public func save(_ s: AppSettings) {
        if let data = try? JSONEncoder().encode(s) { defaults.set(data, forKey: key) }
    }
}
