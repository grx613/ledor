import AppCore
import Domain
import Foundation

/// Состояние редактора — как `EditorState` на Android.
struct EditorState: Equatable {
    var id: String?
    var title = ""
    var listId = localListId
    var type = EventType.birthday
    var customLabel = ""
    var recurring = true
    var recurringManual = false
    var input = CalendarKind.gregorian
    var gDay: Int
    var gMonth: Int
    var gYear: String
    var gYearUnknown = false
    var afterSunset = false
    var hDay = 1
    var hMonth = HebrewMonth.tishrei
    var hYear = ""
    var hYearUnknown = false
    var observeBy = CalendarKind.hebrew
    var observeBoth = false
    var note = ""
    var readOnly = false

    init(today: CivilDate) {
        gDay = today.day
        gMonth = today.month
        gYear = String(today.year)
    }

    var gYearInt: Int? { gYearUnknown ? nil : Int(gYear) }
    var hYearInt: Int? { hYearUnknown ? nil : Int(hYear) }
    var inputYearKnown: Bool { input == .gregorian ? gYearInt != nil : hYearInt != nil }

    func eventDate() -> EventDate {
        input == .gregorian
            ? .fromGregorian(GregorianParts(year: gYearInt, month: gMonth, day: gDay), afterSunset: afterSunset && gYearInt != nil)
            : .fromHebrew(HebrewParts(year: hYearInt, month: hMonth, day: hDay))
    }

    /// «По обоим календарям» — только для повторяющихся событий с известным годом (B-1).
    var effectiveBoth: Bool { recurring && inputYearKnown && observeBoth }
    var effectiveObserve: CalendarKind { !recurring || !inputYearKnown || effectiveBoth ? input : observeBy }

    func toEvent() -> Event {
        Event(id: id ?? "", listId: listId, title: title.trimmingCharacters(in: .whitespacesAndNewlines), type: type,
              customLabel: type == .other ? customLabel.trimmingCharacters(in: .whitespacesAndNewlines) : nil, recurring: recurring,
              observeBy: effectiveObserve, date: eventDate(),
              note: note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : note, observeBoth: effectiveBoth)
    }

    var yearMissingForOneOff: Bool { !recurring && !inputYearKnown }

    var errors: [ValidationError] {
        var e = toEvent()
        e.id = "x"
        return EventValidator.validate(e)
    }

    /// Перенос даты в другой календарь, если она полная.
    mutating func syncOtherCalendar() {
        let d = eventDate()
        if input == .gregorian, let h = d.hebrew {
            hDay = h.day; hMonth = h.month; hYear = h.year.map(String.init) ?? ""; hYearUnknown = false
        } else if input == .hebrew, let g = d.gregorian {
            gDay = g.day; gMonth = g.month; gYear = g.year.map(String.init) ?? ""; gYearUnknown = false; afterSunset = false
        }
    }

    mutating func setType(_ t: EventType) {
        type = t
        if !recurringManual { recurring = EventDefaults.recurring(t) }
    }

    /// Если месяц не существует в новом году — подобрать соответствующий Адар.
    mutating func setHYear(_ v: String) {
        hYear = v
        if let y = Int(v), HebrewCalendarMath.isHebrewYearSupported(y), !HebrewCalendarMath.isMonthInYear(hMonth, y) {
            hMonth = hMonth == .adar ? .adarII : .adar
        }
    }

    static func from(_ e: Event, today: CivilDate) -> EditorState {
        var s = EditorState(today: today)
        s.id = e.id; s.title = e.title; s.listId = e.listId; s.type = e.type; s.customLabel = e.customLabel ?? ""
        s.recurring = e.recurring; s.recurringManual = e.recurring != EventDefaults.recurring(e.type)
        s.input = e.date.inputCalendar; s.afterSunset = e.date.afterSunset; s.observeBy = e.observeBy; s.observeBoth = e.observeBoth
        s.note = e.note ?? ""
        if let g = e.date.gregorian {
            s.gDay = g.day; s.gMonth = g.month; s.gYear = g.year.map(String.init) ?? ""; s.gYearUnknown = g.year == nil
        } else { s.gYear = "" }
        if let h = e.date.hebrew {
            s.hDay = h.day; s.hMonth = h.month; s.hYear = h.year.map(String.init) ?? ""; s.hYearUnknown = h.year == nil
        }
        return s
    }
}
