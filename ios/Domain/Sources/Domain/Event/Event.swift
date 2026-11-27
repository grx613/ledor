import Foundation

/// Дата события: исходный календарь, введённые части и вычисленная дата во втором календаре (`03` §4).
public struct EventDate: Hashable, Codable, Sendable {
    public let inputCalendar: CalendarKind
    public let gregorian: GregorianParts?
    public let afterSunset: Bool
    public let hebrew: HebrewParts?

    public init(inputCalendar: CalendarKind, gregorian: GregorianParts?, afterSunset: Bool, hebrew: HebrewParts?) {
        self.inputCalendar = inputCalendar
        self.gregorian = gregorian
        self.afterSunset = afterSunset
        self.hebrew = hebrew
    }

    /// Год исходной даты в календаре, по которому событие отмечается.
    public func originYear(_ observeBy: CalendarKind) -> Int? { observeBy == .hebrew ? hebrew?.year : gregorian?.year }

    /// Григорианский ввод: при известном годе вычисляется еврейская дата.
    public static func fromGregorian(_ g: GregorianParts, afterSunset: Bool) -> EventDate {
        if let full = g.civilDate, HebrewCalendarMath.isGregorianYearSupported(full.year) {
            return EventDate(inputCalendar: .gregorian, gregorian: g, afterSunset: afterSunset,
                             hebrew: HebrewParts(HebrewCalendarMath.fromGregorian(full, afterSunset: afterSunset)))
        }
        return EventDate(inputCalendar: .gregorian, gregorian: g, afterSunset: false, hebrew: nil)
    }

    /// Еврейский ввод: при известном годе вычисляется григорианская дата.
    public static func fromHebrew(_ h: HebrewParts) -> EventDate {
        if let full = h.hebrewDate {
            let g = HebrewCalendarMath.toGregorian(full)
            return EventDate(inputCalendar: .hebrew, gregorian: GregorianParts(year: g.year, month: g.month, day: g.day),
                             afterSunset: false, hebrew: h)
        }
        return EventDate(inputCalendar: .hebrew, gregorian: nil, afterSunset: false, hebrew: h)
    }
}

public struct Event: Hashable, Codable, Sendable, Identifiable {
    public var id: String
    public var listId: String
    public var title: String
    public var type: EventType
    public var customLabel: String?
    public var recurring: Bool
    public var observeBy: CalendarKind
    public var date: EventDate
    public var note: String?
    /// Отмечать и по второму календарю тоже (B-1). Требует известного года; `observeBy` — основной календарь.
    public var observeBoth: Bool
    /// Повторять каждый месяц, а не каждый год (B-41). Действует только вместе с `isMonthly`.
    public var monthly: Bool

    public init(id: String, listId: String, title: String, type: EventType, customLabel: String?, recurring: Bool,
                observeBy: CalendarKind, date: EventDate, note: String?, observeBoth: Bool = false, monthly: Bool = false) {
        self.id = id
        self.listId = listId
        self.title = title
        self.type = type
        self.customLabel = customLabel
        self.recurring = recurring
        self.observeBy = observeBy
        self.date = date
        self.note = note
        self.observeBoth = observeBoth
        self.monthly = monthly
    }

    /// Ежемесячное событие (B-41): только «Другое», повторяющееся, с григорианской датой. Иначе флаг не действует —
    /// например, если старая версия приложения сменила тип или сделала событие одноразовым.
    public var isMonthly: Bool { monthly && recurring && type == .other && date.gregorian != nil }
}

public enum EventDefaults {
    public static func recurring(_ type: EventType) -> Bool { type != .barMitzvah && type != .batMitzvah }

    public static func rule(_ type: EventType) -> AnniversaryRule { type == .yahrzeit ? .yahrzeit : .birthday }

    /// Сам день рождения или смерти наступлением не считается.
    public static func firstOccurrenceOffset(_ type: EventType) -> Int { type == .birthday || type == .yahrzeit ? 1 : 0 }
}

public enum ValidationError: String, Sendable, CaseIterable {
    case titleEmpty = "TITLE_EMPTY", titleTooLong = "TITLE_TOO_LONG"
    case customLabelRequired = "CUSTOM_LABEL_REQUIRED", customLabelTooLong = "CUSTOM_LABEL_TOO_LONG"
    case customLabelNotAllowed = "CUSTOM_LABEL_NOT_ALLOWED"
    case dateMissing = "DATE_MISSING", dateInvalid = "DATE_INVALID", yearOutOfRange = "YEAR_OUT_OF_RANGE"
    case oneOffNeedsYear = "ONE_OFF_NEEDS_YEAR", observeNeedsYear = "OBSERVE_NEEDS_YEAR", noteTooLong = "NOTE_TOO_LONG"
}

/// Проверка ввода — как `EventValidator` на Android. Длина — в единицах UTF-16, как `String.length` в Kotlin.
public enum EventValidator {
    public static let titleMax = 120
    public static let labelMax = 60
    public static let noteMax = 500

    public static func validate(_ e: Event) -> [ValidationError] {
        var errors: [ValidationError] = []
        let title = e.title.trimmingCharacters(in: .whitespacesAndNewlines)
        if title.isEmpty { errors.append(.titleEmpty) }
        if title.utf16.count > titleMax { errors.append(.titleTooLong) }
        let label = (e.customLabel ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if e.type == .other {
            if label.isEmpty { errors.append(.customLabelRequired) }
            if label.utf16.count > labelMax { errors.append(.customLabelTooLong) }
        } else if !label.isEmpty {
            errors.append(.customLabelNotAllowed)
        }
        if (e.note?.utf16.count ?? 0) > noteMax { errors.append(.noteTooLong) }
        errors += validateDate(e.date)
        let d = e.date
        let inputYear = d.inputCalendar == .gregorian ? d.gregorian?.year : d.hebrew?.year
        if !e.recurring && inputYear == nil { errors.append(.oneOffNeedsYear) }
        if e.recurring && (e.observeBy != d.inputCalendar || e.observeBoth) && inputYear == nil {
            errors.append(.observeNeedsYear)
        }
        var seen = Set<ValidationError>()
        return errors.filter { seen.insert($0).inserted }
    }

    public static func validateDate(_ d: EventDate) -> [ValidationError] {
        switch d.inputCalendar {
        case .gregorian:
            guard let g = d.gregorian else { return [.dateMissing] }
            guard (1...12).contains(g.month) else { return [.dateInvalid] }
            if let year = g.year {
                if !HebrewCalendarMath.isGregorianYearSupported(year) { return [.yearOutOfRange] }
                return g.civilDate == nil ? [.dateInvalid] : []
            }
            let max = g.month == 2 ? 29 : CivilDate.daysInMonth(year: 2001, month: g.month)
            return (1...max).contains(g.day) ? [] : [.dateInvalid]
        case .hebrew:
            guard let h = d.hebrew else { return [.dateMissing] }
            if let year = h.year {
                if !HebrewCalendarMath.isHebrewYearSupported(year) { return [.yearOutOfRange] }
                return HebrewCalendarMath.isValid(year, h.month, h.day) ? [] : [.dateInvalid]
            }
            return (1...h.month.maxDayWithoutYear).contains(h.day) ? [] : [.dateInvalid]
        }
    }
}
