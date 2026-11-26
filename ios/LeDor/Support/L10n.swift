import Foundation

/// Тексты из `Localizable.xcstrings` (перенесены из Android скриптом `ios/tools/strings.py`; ключи — имена Android).
enum L {
    static func t(_ key: String) -> String { NSLocalizedString(key, comment: "") }

    static func f(_ key: String, _ args: CVarArg...) -> String { String(format: t(key), locale: locale, arguments: args) }

    /// Формы множественного числа — из вариантов каталога.
    static func p(_ key: String, _ n: Int) -> String { String.localizedStringWithFormat(t(key), n) }

    /// Язык интерфейса (системный выбор для приложения): en, ru, he, yi, fr, es.
    static var langTag: String { Bundle.main.preferredLocalizations.first ?? "en" }
    static var locale: Locale { Locale(identifier: langTag) }

    /// Иврит и идиш: еврейские даты — буквами.
    static var hebrewLetters: Bool { langTag == "he" || langTag == "yi" }

    /// Язык сайта и политики конфиденциальности.
    static var siteLang: String { ["en", "ru", "he", "yi", "fr", "es"].contains(langTag) ? langTag : "en" }
}
