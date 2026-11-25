// swift-tools-version: 6.0
import PackageDescription

// Календарная логика LeDor без UIKit: тесты идут через `swift test` без симулятора.
let package = Package(
    name: "Domain",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [.library(name: "Domain", targets: ["Domain"])],
    targets: [
        .target(name: "Domain"),
        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
    ]
)
