import Foundation

/// Адрес и публичный ключ сервера — из `LeDor.xcconfig` / `Config.xcconfig` через Info.plist.
enum AppConfig {
    static var apiURL: String { Bundle.main.object(forInfoDictionaryKey: "LeDorAPIURL") as? String ?? "" }
    static var apiKey: String { Bundle.main.object(forInfoDictionaryKey: "LeDorAPIKey") as? String ?? "" }
    static var version: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "" }
    static let site = "https://kalendar.bothub.chat"
    static let sourceCode = "https://github.com/grx613/ledor"
    static let authorChannel = "https://t.me/bothub_founder"
    /// Ссылка в приглашении к списку: страница сайта на языке интерфейса.
    static var appLink: String { "\(site)/\(L.siteLang)/" }
}
