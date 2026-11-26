import Foundation

/// Заход солнца по алгоритму NOAA — так же, как `NOAACalculator` в KosherJava 3.1 (Android): высота над морем 0,
/// для захода зенит 90° поправляется на рефракцию 34′ и радиус солнца 16′; для градусов ниже горизонта — без поправок.
enum SolarCalculator {
    static let geometricZenith = 90.0

    /// Момент, когда центр солнца опускается до `zenith` в день `date` (по местному календарю места); nil — не опускается.
    static func sunset(date: CivilDate, latitude: Double, longitude: Double, zenith: Double) -> Date? {
        let adjusted = zenith == geometricZenith ? zenith + 16.0 / 60 + 34.0 / 60 : zenith
        let minutes = sunsetUTCMinutes(date, latitude, -longitude, adjusted)
        guard minutes.isFinite else { return nil }
        var hours = (minutes / 60).truncatingRemainder(dividingBy: 24)
        if hours <= 0 { hours += 24 }
        // Как getDateFromTime в KosherJava: если по UTC заход «рано утром», он относится к следующей дате UTC.
        var utcDate = date
        if Int(longitude) / 15 + Int(hours) < 6 { utcDate = date.adding(days: 1) }
        let midnight = Zoned.instant(utcDate, TimeOfDay(hour: 0, minute: 0), TimeZone(identifier: "UTC")!)
        return midnight.addingTimeInterval((hours * 3600 * 1000).rounded(.down) / 1000)
    }

    private static func rad(_ d: Double) -> Double { d * .pi / 180 }
    private static func deg(_ r: Double) -> Double { r * 180 / .pi }

    private static func julianDay(_ d: CivilDate) -> Double {
        var (y, m) = (d.year, d.month)
        if m <= 2 { y -= 1; m += 12 }
        let a = y / 100
        let b = 2 - a + a / 4
        return (365.25 * Double(y + 4716)).rounded(.down) + (30.6001 * Double(m + 1)).rounded(.down) + Double(d.day + b) - 1524.5
    }

    private static func centuries(_ jd: Double) -> Double { (jd - 2_451_545.0) / 36525 }

    private static func meanLongitude(_ t: Double) -> Double {
        var l = 280.46646 + t * (36000.76983 + 0.0003032 * t)
        l = l.truncatingRemainder(dividingBy: 360)
        return l < 0 ? l + 360 : l
    }

    private static func meanAnomaly(_ t: Double) -> Double { 357.52911 + t * (35999.05029 - 0.0001537 * t) }
    private static func eccentricity(_ t: Double) -> Double { 0.016708634 - t * (0.000042037 + 0.0000001267 * t) }

    private static func equationOfCenter(_ t: Double) -> Double {
        let m = rad(meanAnomaly(t))
        return sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t)) + sin(2 * m) * (0.019993 - 0.000101 * t) + sin(3 * m) * 0.000289
    }

    private static func apparentLongitude(_ t: Double) -> Double {
        meanLongitude(t) + equationOfCenter(t) - 0.00569 - 0.00478 * sin(rad(125.04 - 1934.136 * t))
    }

    private static func obliquity(_ t: Double) -> Double {
        let seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
        let mean = 23.0 + (26.0 + seconds / 60) / 60
        return mean + 0.00256 * cos(rad(125.04 - 1934.136 * t))
    }

    private static func declination(_ t: Double) -> Double { deg(asin(sin(rad(obliquity(t))) * sin(rad(apparentLongitude(t))))) }

    /// Уравнение времени, минуты.
    private static func equationOfTime(_ t: Double) -> Double {
        let e = obliquity(t), l0 = rad(meanLongitude(t)), ecc = eccentricity(t), m = rad(meanAnomaly(t))
        let y = pow(tan(rad(e) / 2), 2)
        let eq = y * sin(2 * l0) - 2 * ecc * sin(m) + 4 * ecc * y * sin(m) * cos(2 * l0)
            - 0.5 * y * y * sin(4 * l0) - 1.25 * ecc * ecc * sin(2 * m)
        return deg(eq) * 4
    }

    private static func hourAngleSunset(_ lat: Double, _ decl: Double, _ zenith: Double) -> Double {
        let latR = rad(lat), dR = rad(decl)
        return -acos(cos(rad(zenith)) / (cos(latR) * cos(dR)) - tan(latR) * tan(dR))
    }

    /// Минуты UTC от полуночи; `lonWest` — долгота с плюсом на запад.
    private static func sunsetUTCMinutes(_ date: CivilDate, _ lat: Double, _ lonWest: Double, _ zenith: Double) -> Double {
        let jd = julianDay(date)
        let tApprox = centuries(jd + lonWest / 360)
        let noonApprox = lonWest * 4 - equationOfTime(tApprox)
        let noon = 720 + lonWest * 4 - equationOfTime(centuries(jd + noonApprox / 1440))
        func pass(_ t: Double) -> Double {
            let ha = hourAngleSunset(lat, declination(t), zenith)
            return 720 + 4 * (lonWest - deg(ha)) - equationOfTime(t)
        }
        let first = pass(centuries(jd + noon / 1440))
        return pass(centuries(jd + first / 1440))
    }
}
