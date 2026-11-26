// swift-tools-version: 6.0
import PackageDescription

// Логика LeDor без UIKit: Domain — календарь, события, напоминания; AppCore — хранение, сервер, синхронизация.
// Тесты идут через `swift test` без симулятора.
let package = Package(
    name: "Domain",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "AppCore", targets: ["AppCore"]),
    ],
    targets: [
        .target(name: "Domain", resources: [.copy("Resources/cities.json")]),
        .target(name: "AppCore", dependencies: ["Domain"]),
        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
        .testTarget(name: "AppCoreTests", dependencies: ["AppCore"]),
    ]
)
