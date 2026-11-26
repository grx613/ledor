import Foundation

/// Пасхалка в «О программе» (ABT-6, D-57), как `EasterEgg.kt` в Android.
public enum Egg {
    /// Сколько раз нажать на знак, чтобы открыть пасхалку, и сколько раз попасть по Ецер а-Ра.
    public static let steps = 7
    /// Подсказка «Ещё 3…» появляется, когда осталось столько нажатий и меньше.
    public static let hintFrom = 3
}

/// Счётчик нажатий на знак: `Egg.steps` подряд; пауза дольше `window` секунд — счёт заново.
public struct EggTaps: Sendable {
    private let window: TimeInterval
    private var count = 0
    private var last = -TimeInterval.infinity

    public init(window: TimeInterval = 2) { self.window = window }

    /// Сколько нажатий осталось после этого; 0 — пасхалка открывается (и счёт начинается заново).
    public mutating func tap(at now: TimeInterval) -> Int {
        if now - last > window { count = 0 }
        last = now
        count += 1
        let left = Egg.steps - count
        if left <= 0 { count = 0 }
        return max(left, 0)
    }
}
