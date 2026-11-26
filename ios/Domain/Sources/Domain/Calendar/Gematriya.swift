/// Числа еврейскими буквами (для иврита и идиша): 28 → כ״ח, 5750 → תש״ן. Тысячи года не пишутся.
public enum Gematriya {
    private static let ones = ["", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט"]
    private static let tens = ["", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ"]
    private static let hundreds = ["", "ק", "ר", "ש", "ת"]
    private static let finals: [Character: Character] = ["כ": "ך", "מ": "ם", "נ": "ן", "פ": "ף", "צ": "ץ"]

    public static func format(_ number: Int) -> String {
        let n = number % 1000
        var s = ""
        var h = n / 100
        while h > 4 { s += "ת"; h -= 4 }
        s += hundreds[h]
        let r = n % 100
        switch r {
        case 15: s += "טו"
        case 16: s += "טז"
        default: s += tens[r / 10] + ones[r % 10]
        }
        if s.count == 1 { return s + "׳" }
        let last = s.removeLast()
        return s + "״" + String(finals[last] ?? last)
    }
}
