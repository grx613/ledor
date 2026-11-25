import SwiftUI

/// Маген Давид из двух треугольников в квадрате 24×24 — та же геометрия, что `drawMagenDavid` на Android.
struct MagenDavidShape: Shape {
    func path(in rect: CGRect) -> Path {
        let s = min(rect.width, rect.height) / 24
        let ox = rect.midX - 12 * s
        let oy = rect.midY - 12 * s
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: ox + x * s, y: oy + y * s) }
        var path = Path()
        path.addLines([p(12, 2), p(20.66, 17), p(3.34, 17)])
        path.closeSubpath()
        path.addLines([p(12, 22), p(3.34, 7), p(20.66, 7)])
        path.closeSubpath()
        return path
    }
}
