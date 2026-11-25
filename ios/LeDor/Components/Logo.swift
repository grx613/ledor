import SwiftUI

/// Фирменный блок: знак → LeDor → отступ → слоган на латинице и иврите (приглушённо). `06-design-system.md` §6.
struct LogoBlock: View {
    var onDark: Bool
    var markSize: CGFloat = 96

    var body: some View {
        let main = onDark ? Color.white : Palette.onSurface
        let soft = onDark ? Color.white.opacity(0.72) : Palette.onSurfaceVariant
        VStack(spacing: 0) {
            LedorMark(size: markSize)
            Spacer().frame(height: 16)
            Text(verbatim: "LeDor")
                .font(Typography.rubik(40, .semibold)).tracking(0.5).foregroundStyle(main)
            Spacer().frame(height: 28)
            Text(verbatim: "LeDor VaDor")
                .font(Typography.rubik(16)).tracking(1.5).foregroundStyle(soft)
            Spacer().frame(height: 2)
            Text(verbatim: "לדור ודור")
                .font(Typography.rubik(17)).foregroundStyle(soft)
        }
        .accessibilityElement(children: .combine)
    }
}

/// Надпись «Kalendář» у нижнего края экрана: средний размер, разрядка, золото.
struct Wordmark: View {
    var onDark: Bool

    var body: some View {
        Text(verbatim: "Kalendář")
            .font(Typography.rubik(21, .medium)).tracking(4)
            .foregroundStyle(onDark ? Palette.Brand.goldOnDark : Palette.gold)
    }
}
