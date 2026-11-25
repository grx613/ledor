import SwiftUI
import UIKit

/// Цвета — `06-design-system.md` §1. Светлая и тёмная («ночное небо») темы.
enum Palette {
    static let primary = dynamic(0x0B3FAE, 0x9DB8FF)
    static let onPrimary = dynamic(0xFFFFFF, 0x002B75)
    static let primaryContainer = dynamic(0xDCE6FF, 0x0B3FAE)
    static let onPrimaryContainer = dynamic(0x001A4D, 0xDCE6FF)
    static let secondary = dynamic(0x7A5900, 0xF2C75C)
    static let onSecondary = dynamic(0xFFFFFF, 0x3F2E00)
    static let secondaryContainer = dynamic(0xFFE9B3, 0x5A4300)
    static let onSecondaryContainer = dynamic(0x261A00, 0xFFE9B3)
    static let tertiary = dynamic(0x5B4B8A, 0xC9BCF5)
    static let background = dynamic(0xFBF9F4, 0x0B1220)
    static let surface = dynamic(0xFFFFFF, 0x111A2E)
    static let surfaceVariant = dynamic(0xEEF1F8, 0x1C2740)
    static let onSurface = dynamic(0x141B2D, 0xE6EAF5)
    static let onSurfaceVariant = dynamic(0x4A5368, 0xB4BCD0)
    static let outline = dynamic(0xC3C8D6, 0x414B63)
    static let outlineVariant = dynamic(0xE2E6EF, 0x2A344B)
    static let error = dynamic(0xB3261E, 0xF2B8B5)
    static let onError = dynamic(0xFFFFFF, 0x601410)
    /// Декоративное золото: линии, контуры, иллюстрации — не текст.
    static let gold = dynamic(0xD4A72C, 0xF2C75C)

    /// Цвета знака и заставки — одинаковые в обеих темах.
    enum Brand {
        static let tchelet = Color(hex: 0x0B3FAE)
        static let tcheletDeep = Color(hex: 0x061F5C)
        static let markTop = Color(hex: 0x1450C8)
        static let markBottom = Color(hex: 0x072C7C)
        static let sheetHeader = Color(hex: 0xDCE6FF)
        static let gold = Color(hex: 0xD4A72C)
        static let goldOnDark = Color(hex: 0xF2C75C)
    }

    private static func dynamic(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
}

extension Color {
    init(hex: UInt32) { self.init(UIColor(hex: hex)) }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(red: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
                  blue: CGFloat(hex & 0xFF) / 255, alpha: 1)
    }
}
