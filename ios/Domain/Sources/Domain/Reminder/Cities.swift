import Foundation

/// Город из встроенного списка (143 города, общий с Android файл `cities.json`).
public struct City: Hashable, Codable, Sendable, Identifiable {
    public let id: String
    public let nameRu: String
    public let nameEn: String
    public let nameHe: String
    public let countryCode: String
    public let lat: Double
    public let lon: Double
    public let zoneId: String

    public init(id: String, nameRu: String, nameEn: String, nameHe: String, countryCode: String, lat: Double, lon: Double,
                zoneId: String) {
        self.id = id
        self.nameRu = nameRu
        self.nameEn = nameEn
        self.nameHe = nameHe
        self.countryCode = countryCode
        self.lat = lat
        self.lon = lon
        self.zoneId = zoneId
    }

    public var timeZone: TimeZone { TimeZone(identifier: zoneId)! }
    public var inIsrael: Bool { countryCode == "IL" }
}

public struct CityCatalog: Sendable {
    public let cities: [City]
    private let index: [String: City]

    public init(cities: [City]) {
        self.cities = cities
        index = Dictionary(uniqueKeysWithValues: cities.map { ($0.id, $0) })
    }

    public func byId(_ id: String?) -> City? { id.flatMap { index[$0] } }

    public func search(_ query: String) -> [City] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if q.isEmpty { return cities }
        return cities.filter {
            $0.nameRu.lowercased().contains(q) || $0.nameEn.lowercased().contains(q) || $0.nameHe.contains(q)
        }
    }

    public static let builtIn: CityCatalog = {
        let url = Bundle.module.url(forResource: "cities", withExtension: "json")!
        return CityCatalog(cities: try! JSONDecoder().decode([City].self, from: Data(contentsOf: url)))
    }()
}

/// Заход солнца в день `date` — момент, по которому планируется напоминание накануне.
public protocol SunsetProvider: Sendable {
    func sunset(_ date: CivilDate) -> Date
}

public struct CitySunsetProvider: SunsetProvider {
    public static let nightfallDegrees = 8.5
    public let city: City

    public init(city: City) { self.city = city }

    /// Нет захода (полярный день или ночь) — 18:00 местного времени.
    public func sunset(_ date: CivilDate) -> Date {
        sunsetOrNil(date) ?? Zoned.instant(date, TimeOfDay(hour: 18, minute: 0), city.timeZone)
    }

    public func sunsetOrNil(_ date: CivilDate) -> Date? {
        SolarCalculator.sunset(date: date, latitude: city.lat, longitude: city.lon, zenith: SolarCalculator.geometricZenith)
    }

    /// Выход звёзд: солнце на 8,5° ниже горизонта; nil — так не темнеет.
    public func nightfallOrNil(_ date: CivilDate) -> Date? {
        SolarCalculator.sunset(date: date, latitude: city.lat, longitude: city.lon,
                               zenith: SolarCalculator.geometricZenith + Self.nightfallDegrees)
    }
}

/// Без города — 18:00 в поясе устройства.
public struct DefaultSunsetProvider: SunsetProvider {
    public let zone: TimeZone
    public init(zone: TimeZone) { self.zone = zone }
    public func sunset(_ date: CivilDate) -> Date { Zoned.instant(date, TimeOfDay(hour: 18, minute: 0), zone) }
}

public enum SunsetVerdict: Sendable { case before, twilight, after }

/// `sunset` и `nightfall` — местное время границ сумерек, к которым относится ответ.
public struct SunsetHint: Hashable, Sendable {
    public let verdict: SunsetVerdict
    public let sunset: TimeOfDay
    public let nightfall: TimeOfDay
}

/// Подсказка «до или после захода» по местному времени и городу события (B-24).
public enum SunsetHints {
    /// Если по градусам ночь не наступает — выход звёзд через 72 минуты после захода.
    public static let nightfallFallbackMinutes = 72

    /// nil — в этот день в этом городе солнце не заходит.
    public static func evaluate(date: CivilDate, time: TimeOfDay, city: City) -> SunsetHint? {
        let zone = city.timeZone
        let provider = CitySunsetProvider(city: city)
        let moment = Zoned.instant(date, time, zone)
        func bounds(_ d: CivilDate) -> (Date, Date)? {
            guard let sunset = provider.sunsetOrNil(d) else { return nil }
            let night = provider.nightfallOrNil(d).flatMap { $0 > sunset ? $0 : nil }
                ?? sunset.addingTimeInterval(Double(nightfallFallbackMinutes * 60))
            return (sunset, night)
        }
        func hint(_ v: SunsetVerdict, _ b: (Date, Date)) -> SunsetHint {
            SunsetHint(verdict: v, sunset: Zoned.timeOfDay(b.0, zone), nightfall: Zoned.timeOfDay(b.1, zone))
        }
        // После полуночи ещё могут длиться сумерки вчерашнего вечера (летом на севере).
        if let prev = bounds(date.adding(days: -1)), moment < prev.1 { return hint(.twilight, prev) }
        guard let today = bounds(date) else { return nil }
        if moment < today.0 { return hint(.before, today) }
        if moment < today.1 { return hint(.twilight, today) }
        return hint(.after, today)
    }
}
