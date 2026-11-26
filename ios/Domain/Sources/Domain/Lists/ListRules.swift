import Foundation

/// Название общего списка — как на сервере: `lower(regexp_replace(btrim(normalize(name, NFKC)), '\s+', ' '))`.
public enum ListNameRules {
    public static let min = 3
    public static let max = 60

    public static func clean(_ name: String) -> String {
        name.split(whereSeparator: { $0.isWhitespace }).joined(separator: " ")
    }

    public static func isValid(_ name: String) -> Bool { (min...max).contains(clean(name).utf16.count) }

    public static func key(_ name: String) -> String { clean(name.precomposedStringWithCompatibilityMapping).lowercased() }
}

public enum PinRules {
    public static let readLength = 4...8
    public static let masterLength = 6...12

    private static func digitsOnly(_ pin: String) -> Bool { !pin.isEmpty && pin.allSatisfy { ("0"..."9").contains($0) } }

    public static func isValidReadPin(_ pin: String) -> Bool { readLength.contains(pin.count) && digitsOnly(pin) }
    public static func isValidMasterPin(_ pin: String) -> Bool { masterLength.contains(pin.count) && digitsOnly(pin) }
    public static func isValidAnyPin(_ pin: String) -> Bool { (4...12).contains(pin.count) && digitsOnly(pin) }
}

/// ПИН из случайных цифр; `SystemRandomNumberGenerator` на платформах Apple — криптостойкий.
public enum PinGenerator {
    public static func generate(length: Int) -> String {
        String((0..<length).map { _ in Character(String(Int.random(in: 0...9))) })
    }
}
