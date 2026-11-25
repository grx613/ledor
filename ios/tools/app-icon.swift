// Рисует значок приложения 1024×1024 по геометрии mark.svg (полный квадрат: углы скругляет система).
// Запуск: swift ios/tools/app-icon.swift ios/LeDor/Assets.xcassets/AppIcon.appiconset/AppIcon.png
import AppKit
let n = 1024
let k = CGFloat(n) / 66
let cs = CGColorSpace(name: CGColorSpace.sRGB)!
let ctx = CGContext(data: nil, width: n, height: n, bitsPerComponent: 8, bytesPerRow: 0, space: cs,
                    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
ctx.translateBy(x: 0, y: CGFloat(n)); ctx.scaleBy(x: 1, y: -1)
func c(_ h: UInt32) -> CGColor { CGColor(srgbRed: CGFloat(h >> 16 & 255) / 255, green: CGFloat(h >> 8 & 255) / 255, blue: CGFloat(h & 255) / 255, alpha: 1) }
func r(_ x: CGFloat, _ y: CGFloat, _ w: CGFloat, _ h: CGFloat) -> CGRect { CGRect(x: (x - 21) * k, y: (y - 21) * k, width: w * k, height: h * k) }
let g = CGGradient(colorsSpace: cs, colors: [c(0x1450C8), c(0x072C7C)] as CFArray, locations: [0, 1])!
ctx.drawLinearGradient(g, start: .zero, end: CGPoint(x: n, y: n), options: [])
func rr(_ rect: CGRect, _ rad: CGFloat, _ col: UInt32) { ctx.addPath(CGPath(roundedRect: rect, cornerWidth: rad, cornerHeight: rad, transform: nil)); ctx.setFillColor(c(col)); ctx.fillPath() }
rr(r(31, 34, 46, 42), 5 * k, 0xFFFFFF)
rr(r(31, 34, 46, 11), 5 * k, 0xDCE6FF)
ctx.setFillColor(c(0xDCE6FF)); ctx.fill(r(31, 40, 46, 5))
rr(r(40, 29.5, 5, 9), k, 0x0B3FAE); rr(r(63, 29.5, 5, 9), k, 0x0B3FAE)
func pt(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: (x - 21) * k, y: (y - 21) * k) }
ctx.setStrokeColor(c(0xD4A72C)); ctx.setLineWidth(2.4 * k); ctx.setLineJoin(.round)
ctx.addLines(between: [pt(54, 49.5), pt(63.53, 66), pt(44.47, 66)]); ctx.closePath()
ctx.addLines(between: [pt(54, 71.5), pt(44.47, 55), pt(63.53, 55)]); ctx.closePath()
ctx.strokePath()
let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
