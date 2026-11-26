import Foundation

/// Эталонные векторы `spec/golden/*.json` читаются прямо из репозитория — общие для Android, сайта и iOS.
enum Golden {
    static let directory = URL(fileURLWithPath: #filePath)
        .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        .deletingLastPathComponent().deletingLastPathComponent()
        .appendingPathComponent("spec/golden")

    /// Строки-массивы файла (`rows`).
    static func rows(_ file: String) throws -> [[Any]] {
        let data = try Data(contentsOf: directory.appendingPathComponent(file))
        let json = try JSONSerialization.jsonObject(with: data) as! [String: Any]
        return json["rows"] as! [[Any]]
    }
}
