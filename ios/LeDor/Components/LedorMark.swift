import SwiftUI

/// Знак LeDor: лист календаря с золотым Маген Давидом на тхелет. Геометрия — `website/assets/img/mark.svg`
/// (видимая область 21…87 из 108, как у иконки Android).
struct LedorMark: View {
    var size: CGFloat = 96

    var body: some View {
        Canvas { context, canvas in
            let k = canvas.width / 66
            func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect {
                CGRect(x: (x - 21) * k, y: (y - 21) * k, width: w * k, height: h * k)
            }
            context.fill(Path(CGRect(origin: .zero, size: canvas)),
                         with: .linearGradient(Gradient(colors: [Palette.Brand.markTop, Palette.Brand.markBottom]),
                                               startPoint: .zero, endPoint: CGPoint(x: canvas.width, y: canvas.height)))
            // лист календаря и его шапка
            let corner = CGSize(width: 5 * k, height: 5 * k)
            context.fill(Path(roundedRect: r(31, 34, 46, 42), cornerSize: corner), with: .color(.white))
            context.fill(Path(roundedRect: r(31, 34, 46, 11), cornerSize: corner), with: .color(Palette.Brand.sheetHeader))
            context.fill(Path(r(31, 40, 46, 5)), with: .color(Palette.Brand.sheetHeader))
            // кольца
            let ring = CGSize(width: k, height: k)
            context.fill(Path(roundedRect: r(40, 29.5, 5, 9), cornerSize: ring), with: .color(Palette.Brand.tchelet))
            context.fill(Path(roundedRect: r(63, 29.5, 5, 9), cornerSize: ring), with: .color(Palette.Brand.tchelet))
            // Маген Давид: центр (54; 60,5), радиус 11
            let star = 22 * k / 0.833
            let center = CGPoint(x: (54 - 21) * k, y: (60.5 - 21) * k)
            let starRect = CGRect(x: center.x - star / 2, y: center.y - star / 2, width: star, height: star)
            context.stroke(MagenDavidShape().path(in: starRect), with: .color(Palette.Brand.gold),
                           style: StrokeStyle(lineWidth: 2.4 * k, lineJoin: .round))
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: size * 0.24, style: .continuous))
        .accessibilityElement()
        .accessibilityLabel(Text(verbatim: "LeDor"))
    }
}
