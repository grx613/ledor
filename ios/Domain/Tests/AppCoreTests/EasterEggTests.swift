import Foundation
import Testing
@testable import AppCore

/// ABT-6, D-57: пасхалка в «О программе» — как `EasterEggTest` в Android.
struct EasterEggTests {
    @Test func sevenTapsInARowOpenIt() {
        var taps = EggTaps()
        var lefts: [Int] = []
        for i in 0..<7 { lefts.append(taps.tap(at: 1 + Double(i) * 0.3)) }
        #expect(lefts == [6, 5, 4, 3, 2, 1, 0])
        // После открытия счёт начинается заново; пауза дольше двух секунд тоже его сбрасывает.
        #expect(taps.tap(at: 4) == 6)
        #expect(taps.tap(at: 4.5) == 5)
        #expect(taps.tap(at: 7) == 6)
    }

    /// Отговорок столько же, сколько попаданий, на всех языках; цитата на иврите и сокращение — в каталоге строк.
    @Test func catalogHasOneExcusePerHit() throws {
        let url = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
            .appendingPathComponent("../../../LeDor/Resources/Localizable.xcstrings").standardized
        let catalog = try JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any]
        let strings = try #require(catalog?["strings"] as? [String: [String: Any]])
        for i in 0..<Egg.steps {
            let locs = try #require(strings["egg_excuses.\(i)"]?["localizations"] as? [String: Any], "egg_excuses.\(i)")
            #expect(Set(locs.keys) == ["en", "ru", "he", "yi", "fr", "es"])
        }
        #expect(strings["egg_excuses.\(Egg.steps)"] == nil)
        for key in ["egg_steps", "egg_yetzer_name", "egg_quote", "egg_quote_source", "egg_back", "egg_quote_he", "egg_yetzer_abbr"] {
            #expect(strings[key] != nil, "\(key)")
        }
    }
}
