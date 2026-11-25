import SwiftUI

/// Брендовая заставка (`06-design-system.md` §7): градиент тхелет с едва заметным узором из Маген Давидов,
/// знак и слоган по центру, «Kalendář» у нижнего края.
struct BrandSplash: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Palette.Brand.tchelet, Palette.Brand.tcheletDeep], startPoint: .top, endPoint: .bottom)
            StarPattern()
            LogoBlock(onDark: true, markSize: 104)
        }
        .overlay(alignment: .bottom) {
            Wordmark(onDark: true).padding(.bottom, 40)
        }
        .ignoresSafeArea(edges: [.top, .horizontal])
        .background(Palette.Brand.tcheletDeep.ignoresSafeArea())
    }
}

/// Узор: шахматный ряд контурных звёзд 28 pt с шагом 72 pt, белый с прозрачностью 5 %.
private struct StarPattern: View {
    var body: some View {
        Canvas { context, size in
            let step: CGFloat = 72
            let star: CGFloat = 28
            let color = Color.white.opacity(0.05)
            var row = 0
            var y: CGFloat = 0
            while y < size.height + step {
                var x: CGFloat = row.isMultiple(of: 2) ? 0 : step / 2
                while x < size.width + step {
                    let rect = CGRect(x: x - star / 2, y: y - star / 2, width: star, height: star)
                    context.stroke(MagenDavidShape().path(in: rect), with: .color(color),
                                   style: StrokeStyle(lineWidth: 1, lineJoin: .round))
                    x += step
                }
                y += step * 0.87
                row += 1
            }
        }
        .accessibilityHidden(true)
    }
}

#Preview { BrandSplash() }
