import SwiftUI

/// Шрифт Rubik (OFL 1.1, вариативный) — единственный в приложении: кириллица, латиница и иврит. `06-design-system.md` §2.
enum Typography {
    static func rubik(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .custom("Rubik", size: size).weight(weight)
    }
}
