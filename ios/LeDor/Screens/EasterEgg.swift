import AppCore
import SwiftUI

/// Пасхалка (ABT-6, D-57), как `YetzerGame` в Android: Ецер а-Ра шепчет отговорки — «позвонишь завтра». От каждого
/// попадания он сжимается, становится меньше и отскакивает; после седьмого исчезает, и появляется «Кто силён? Тот, кто
/// побеждает своё влечение» (Авот 4:1). Персонаж нарисован здесь же, картинок и звука нет.
struct YetzerGame: View {
    let onClose: () -> Void
    @State private var hits = 0
    @State private var spot = CGPoint(x: 0.5, y: 0.4)
    @State private var bob = false

    var body: some View {
        ZStack {
            Palette.background.ignoresSafeArea()
            if hits < Egg.steps { game } else { won.transition(.opacity) }
        }
        .overlay(alignment: .topTrailing) {
            Button(action: onClose) {
                Image(systemName: "xmark").font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Palette.onSurface).frame(width: 44, height: 44)
            }
            .accessibilityLabel(L.t("close"))
            .padding(8)
        }
        .sensoryFeedback(.impact(weight: .medium), trigger: hits)
    }

    private var game: some View {
        VStack(spacing: 0) {
            Text(L.t("egg_excuses.\(min(hits, Egg.steps - 1))"))
                .font(Typography.rubik(17, .medium)).italic()
                .multilineTextAlignment(.center)
                .padding(.horizontal, 20).padding(.vertical, 10)
                .background(Palette.surfaceVariant, in: Capsule())
                .padding(.top, 56).padding(.horizontal, 24)
            GeometryReader { geo in
                let size = CGFloat(136 - hits * 12)
                YetzerFigure(size: size)
                    .keyframeAnimator(initialValue: 1.0, trigger: hits) { content, squash in
                        content.scaleEffect(x: 2 - squash, y: squash)
                    } keyframes: { _ in
                        MoveKeyframe(0.7)
                        SpringKeyframe(1.0, duration: 0.7, spring: Spring(response: 0.35, dampingRatio: 0.35))
                    }
                    .offset(y: bob ? 6 : -6)
                    .contentShape(Rectangle())
                    .onTapGesture(perform: hit)
                    .accessibilityElement()
                    .accessibilityLabel(L.t("egg_yetzer_name"))
                    .accessibilityAddTraits(.isButton)
                    .accessibilityAction { hit() }
                    .position(x: size / 2 + (geo.size.width - size) * spot.x, y: size / 2 + (geo.size.height - size) * spot.y)
            }
            .padding(.top, 20).padding(.bottom, 16).padding(.horizontal, 16)
            Text(verbatim: "\(hits) / \(Egg.steps)")
                .font(Typography.rubik(14, .medium)).foregroundStyle(Palette.onSurfaceVariant)
                .padding(.bottom, 24)
        }
        .onAppear { withAnimation(.easeInOut(duration: 1.4).repeatForever(autoreverses: true)) { bob = true } }
    }

    private var won: some View {
        VStack(spacing: 12) {
            let hebrew = L.t("egg_quote_he")
            let translated = L.t("egg_quote")
            // Направление — справа налево: иначе в интерфейсе слева направо точка и вопрос уезжают не туда.
            Text(hebrew).font(Typography.rubik(24, .semibold)).multilineTextAlignment(.center)
                .environment(\.layoutDirection, .rightToLeft)
            if translated != hebrew {
                Text(translated).font(Typography.rubik(17, .medium)).multilineTextAlignment(.center)
            }
            Text(L.t("egg_quote_source")).font(Typography.rubik(12)).foregroundStyle(Palette.onSurfaceVariant)
            Spacer().frame(height: 8)
            Text(L.t("egg_back")).font(Typography.rubik(15)).multilineTextAlignment(.center)
            Button(action: onClose) { Text(L.t("done")).foregroundStyle(Palette.onPrimary) }
                .buttonStyle(.borderedProminent).tint(Palette.primary)
        }
        .foregroundStyle(Palette.onSurface)
        .padding(32)
    }

    private func hit() {
        withAnimation(.spring(response: 0.55, dampingFraction: 0.6)) {
            hits += 1
            spot = Self.nextSpot(from: spot)
        }
    }

    /// Новое место заметно в стороне от прежнего (доли свободного поля).
    private static func nextSpot(from: CGPoint) -> CGPoint {
        for _ in 0..<20 {
            // Явно CGFloat: иначе выбирается CGPoint(x: Int, y: Int) и он прыгает только по углам.
            let to = CGPoint(x: CGFloat.random(in: 0...1), y: CGFloat.random(in: 0...1))
            if hypot(to.x - from.x, to.y - from.y) > 0.35 { return to }
        }
        return CGPoint(x: 1 - from.x, y: 1 - from.y)
    }
}

/// Ецер а-Ра: тёмный клуб дыма с хвостом, хитрыми глазами и сокращением יצה״ר. Свой персонаж, не копия чужих;
/// контур тот же, что в Android (`Yetzer` в `EasterEgg.kt`).
struct YetzerFigure: View {
    let size: CGFloat

    private static let lilac = Color(hex: 0xB79CFF)
    private static let smoke = Color(hex: 0x2E2650)
    private static let ink = Color(hex: 0x0C0A17)

    var body: some View {
        ZStack(alignment: .top) {
            Circle()
                .fill(RadialGradient(colors: [Self.lilac.opacity(0.45), .clear], center: .center, startRadius: 0, endRadius: size * 0.8))
                .frame(width: size * 1.6, height: size * 1.6)
                .frame(width: size, height: size)
            // Вихор на макушке выходит за верхний край — поэтому холст больше фигуры на `pad` с каждой стороны.
            Canvas { ctx, area in
                let pad = area.width * 0.12 / 1.24
                ctx.translateBy(x: pad, y: pad)
                draw(&ctx, w: area.width - 2 * pad, h: area.height - 2 * pad)
            }
            .frame(width: size * 1.24, height: size * 1.24)
            .frame(width: size, height: size)
            Text(L.t("egg_yetzer_abbr"))
                .font(Typography.rubik(size * 0.15, .bold)).foregroundStyle(Palette.gold)
                .environment(\.layoutDirection, .rightToLeft)
                .padding(.top, size * 0.60).padding(.leading, size * 0.06)
        }
        .frame(width: size, height: size)
    }

    private func draw(_ ctx: inout GraphicsContext, w: CGFloat, h: CGFloat) {
        func p(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: w * x, y: h * y) }
        let lilac = Self.lilac
        // Клуб дыма: круглая голова, книзу — хвост, уходящий в сторону.
        var body = Path()
        body.move(to: p(0.50, 0.08))
        body.addCurve(to: p(0.86, 0.60), control1: p(0.85, 0.08), control2: p(0.96, 0.40))
        body.addCurve(to: p(0.52, 0.88), control1: p(0.80, 0.76), control2: p(0.62, 0.80))
        body.addCurve(to: p(0.20, 0.97), control1: p(0.44, 0.94), control2: p(0.36, 0.99))
        body.addCurve(to: p(0.27, 0.76), control1: p(0.31, 0.92), control2: p(0.33, 0.84))
        body.addCurve(to: p(0.20, 0.20), control1: p(0.09, 0.62), control2: p(0.05, 0.36))
        body.addCurve(to: p(0.50, 0.08), control1: p(0.28, 0.11), control2: p(0.38, 0.08))
        body.closeSubpath()
        ctx.fill(body, with: .linearGradient(Gradient(colors: [Self.smoke, Self.ink]), startPoint: p(0, 0), endPoint: p(0, 1)))
        ctx.stroke(body, with: .color(lilac.opacity(0.7)), lineWidth: w * 0.018)
        // Вихор дыма на макушке.
        var curl = Path()
        curl.move(to: p(0.50, 0.10))
        curl.addCurve(to: p(0.63, 0.03), control1: p(0.44, -0.07), control2: p(0.70, -0.09))
        ctx.stroke(curl, with: .color(Self.smoke), style: StrokeStyle(lineWidth: w * 0.07, lineCap: .round))
        ctx.stroke(curl, with: .color(lilac.opacity(0.7)), style: StrokeStyle(lineWidth: w * 0.018, lineCap: .round))
        for side: CGFloat in [-1, 1] {
            let cx = 0.5 + side * 0.15
            ctx.fill(Path(ellipseIn: CGRect(origin: p(cx - 0.085, 0.26), size: CGSize(width: w * 0.17, height: h * 0.20))), with: .color(.white))
            let pupil = p(cx - side * 0.025, 0.385)
            let r = w * 0.045
            ctx.fill(Path(ellipseIn: CGRect(x: pupil.x - r, y: pupil.y - r, width: 2 * r, height: 2 * r)), with: .color(Self.ink))
            // Брови домиком к переносице — хитрый взгляд.
            var brow = Path()
            brow.move(to: p(cx + side * 0.11, 0.19))
            brow.addLine(to: p(cx - side * 0.09, 0.26))
            ctx.stroke(brow, with: .color(lilac), style: StrokeStyle(lineWidth: w * 0.03, lineCap: .round))
        }
        var smirk = Path()
        smirk.move(to: p(0.40, 0.53))
        smirk.addQuadCurve(to: p(0.64, 0.50), control: p(0.54, 0.61))
        ctx.stroke(smirk, with: .color(lilac), style: StrokeStyle(lineWidth: w * 0.025, lineCap: .round))
    }
}
