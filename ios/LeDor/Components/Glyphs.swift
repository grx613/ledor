import Domain
import SwiftUI

/// Значки типов событий — та же геометрия 24×24, что `EventTypeGlyph` на Android.
struct EventTypeGlyph: View {
    let type: EventType
    let color: Color

    var body: some View {
        Canvas { ctx, size in
            let u = min(size.width, size.height) / 24
            let st = StrokeStyle(lineWidth: 1.8 * u, lineCap: .round, lineJoin: .round)
            func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * u, y: y * u) }
            func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect { CGRect(x: x * u, y: y * u, width: w * u, height: h * u) }
            func line(_ a: CGPoint, _ b: CGPoint, _ w: CGFloat) {
                var path = Path(); path.move(to: a); path.addLine(to: b)
                ctx.stroke(path, with: .color(color), style: StrokeStyle(lineWidth: w * u, lineCap: .round))
            }
            switch type {
            case .birthday:
                ctx.stroke(Path(roundedRect: r(4, 12, 16, 9), cornerRadius: 2 * u), with: .color(color), style: st)
                line(p(4, 16), p(20, 16), 1.8)
                for x: CGFloat in [8, 12, 16] {
                    line(p(x, 12), p(x, 8), 1.8)
                    ctx.fill(Path(ellipseIn: r(x - 1.1, 4, 2.2, 3)), with: .color(color))
                }
            case .yahrzeit:
                ctx.stroke(Path(roundedRect: r(8.5, 10, 7, 11), cornerRadius: 1.5 * u), with: .color(color), style: st)
                var flame = Path()
                flame.move(to: p(12, 2.5))
                flame.addCurve(to: p(12, 8.5), control1: p(15, 5.5), control2: p(14.5, 8.5))
                flame.addCurve(to: p(12, 2.5), control1: p(9.5, 8.5), control2: p(9, 5.5))
                ctx.fill(flame, with: .color(color))
            case .barMitzvah:
                line(p(5, 3), p(5, 21), 2.2)
                line(p(19, 3), p(19, 21), 2.2)
                ctx.stroke(Path(r(7, 5, 10, 14)), with: .color(color), style: st)
                line(p(9.5, 9), p(14.5, 9), 1.4)
                line(p(9.5, 12), p(14.5, 12), 1.4)
                line(p(9.5, 15), p(13, 15), 1.4)
            case .batMitzvah:
                line(p(4, 21), p(20, 21), 1.8)
                ctx.stroke(Path(roundedRect: r(6.5, 10, 4, 11), cornerRadius: u), with: .color(color), style: st)
                ctx.stroke(Path(roundedRect: r(13.5, 10, 4, 11), cornerRadius: u), with: .color(color), style: st)
                ctx.fill(Path(ellipseIn: r(7.4, 4, 2.2, 4)), with: .color(color))
                ctx.fill(Path(ellipseIn: r(14.4, 4, 2.2, 4)), with: .color(color))
            case .wedding:
                ctx.stroke(Path(ellipseIn: r(3.5, 8.5, 11, 11)), with: .color(color), style: st)
                ctx.stroke(Path(ellipseIn: r(9.5, 8.5, 11, 11)), with: .color(color), style: st)
                var gem = Path()
                gem.addLines([p(9, 3), p(11, 5.5), p(9, 8), p(7, 5.5)])
                gem.closeSubpath()
                ctx.fill(gem, with: .color(color))
            case .other:
                var star = Path()
                for i in 0..<10 {
                    let radius: CGFloat = i % 2 == 0 ? 9.5 : 4
                    let a = (-90.0 + Double(i) * 36.0) * .pi / 180
                    let pt = p(12 + radius * CGFloat(cos(a)), 12.5 + radius * CGFloat(sin(a)))
                    if i == 0 { star.move(to: pt) } else { star.addLine(to: pt) }
                }
                star.closeSubpath()
                ctx.stroke(star, with: .color(color), style: st)
            }
        }
    }
}

/// Солнце или луна — для переключателя «до / после захода».
struct SunGlyph: View {
    let color: Color
    var moon = false

    var body: some View {
        Canvas { ctx, size in
            let u = min(size.width, size.height) / 24
            func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: x * u, y: y * u) }
            if moon {
                var path = Path()
                path.move(to: p(15, 3))
                path.addCurve(to: p(9, 19), control1: p(6, 3), control2: p(3, 14))
                path.addCurve(to: p(21, 16), control1: p(13, 22.5), control2: p(19.5, 21))
                path.addCurve(to: p(15, 3), control1: p(14, 18), control2: p(9, 10))
                ctx.fill(path, with: .color(color))
            } else {
                ctx.fill(Path(ellipseIn: CGRect(x: 7.5 * u, y: 7.5 * u, width: 9 * u, height: 9 * u)), with: .color(color))
                for i in 0..<8 {
                    let a = Double(i) * 45 * .pi / 180
                    var ray = Path()
                    ray.move(to: p(12 + 7 * CGFloat(cos(a)), 12 + 7 * CGFloat(sin(a))))
                    ray.addLine(to: p(12 + 10 * CGFloat(cos(a)), 12 + 10 * CGFloat(sin(a))))
                    ctx.stroke(ray, with: .color(color), style: StrokeStyle(lineWidth: 1.8 * u, lineCap: .round))
                }
            }
        }
        .frame(width: 18, height: 18)
    }
}
