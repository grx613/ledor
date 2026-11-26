import Domain
import SwiftUI

extension Palette {
    /// Цвета типов событий (`06-design-system.md` §1).
    static func forType(_ t: EventType) -> Color {
        switch t {
        case .birthday: dynamicColor(0xD4A72C, 0xF2C75C)
        case .yahrzeit: dynamicColor(0x64748B, 0xA3B1C6)
        case .barMitzvah: dynamicColor(0x2563EB, 0x8FB4FF)
        case .batMitzvah: dynamicColor(0xB4418E, 0xF0A6D6)
        case .wedding: dynamicColor(0xD9466F, 0xFF9DB5)
        case .other: dynamicColor(0x0F9D8A, 0x5FD6C4)
        }
    }

    /// Метки списков — 8 цветов по кругу.
    static func listColor(_ index: Int) -> Color {
        let colors: [UInt32] = [0x0B3FAE, 0xD4A72C, 0x0F9D8A, 0xD9466F, 0x5B4B8A, 0xE0792B, 0x3B8E3B, 0x64748B]
        return Color(hex: colors[((index % 8) + 8) % 8])
    }

    /// Подложка субботы в сетке календаря.
    static let shabbat = Color(UIColor { $0.userInterfaceStyle == .dark
        ? UIColor(hex: 0x9DB8FF).withAlphaComponent(0.06) : UIColor(hex: 0x0B3FAE).withAlphaComponent(0.05) })

    static let observeHint = Color(hex: 0x6B7FB3)
    static let success = dynamicColor(0x2E7D32, 0x81C784)

    static func dynamicColor(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
}
