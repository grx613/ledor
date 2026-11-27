import AppCore
import Domain
import Foundation

/// Повтор события в редакторе (EVT-4, B-41): «Раз в месяц» — только у типа «Другое».
enum Repeat: CaseIterable { case once, yearly, monthly }

/// Состояние редактора — как `EditorState` на Android.
struct EditorState: Equatable {
    var id: String?
    var title = ""
    var listId = localListId
    var type = EventType.birthday
    var customLabel = ""
    var recurring = true
    var recurringManual = false
    var monthly = false
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

    /// Каждый месяц (B-41): только «Другое», дата — григорианская, без захода солнца.
    var isMonthly: Bool { monthly && recurring && type == .other }
    var repeatMode: Repeat { !recurring ? .once : isMonthly ? .monthly : .yearly }

    func eventDate() -> EventDate {
        if isMonthly { return .fromGregorian(GregorianParts(year: gYearInt, month: gMonth, day: gDay), afterSunset: false) }
        return input == .gregorian
            ? .fromGregorian(GregorianParts(year: gYearInt, month: gMonth, day: gDay), afterSunset: afterSunset && gYearInt != nil)
            : .fromHebrew(HebrewParts(year: hYearInt, month: hMonth, day: hDay))
    }

    /// «По обоим календарям» — только для повторяющихся событий с известным годом (B-1).
    var effectiveBoth: Bool { recurring && !isMonthly && inputYearKnown && observeBoth }
    var effectiveObserve: CalendarKind {
        if isMonthly { return .gregorian }
        return !recurring || !inputYearKnown || effectiveBoth ? input : observeBy
    }

    func toEvent() -> Event {
        Event(id: id ?? "", listId: listId, title: title.trimmingCharacters(in: .whitespacesAndNewlines), type: type,
              customLabel: type == .other ? customLabel.trimmingCharacters(in: .whitespacesAndNewlines) : nil, recurring: recurring,
              observeBy: effectiveObserve, date: eventDate(),
              note: note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : note, observeBoth: effectiveBoth,
              monthly: isMonthly)
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
        // «Раз в месяц» бывает только у «Другое»: при смене типа повтор становится ежегодным.
        if t != .other { monthly = false }
        type = t
        if !recurringManual { recurring = EventDefaults.recurring(t) }
    }

    /// EVT-4 для «Другое»: один раз / раз в год / раз в месяц. Раз в месяц — дата всегда григорианская (B-41).
    mutating func setRepeat(_ r: Repeat, today: CivilDate) {
        syncOtherCalendar()
        recurring = r != .once
        monthly = r == .monthly
        recurringManual = true
        guard r == .monthly else { return }
        // Год нужен, чтобы знать первый раз; поле «Год неизвестен» для ежемесячного не показывается.
        if gYearUnknown || gYear.trimmingCharacters(in: .whitespaces).isEmpty { gYear = String(today.year) }
        input = .gregorian; afterSunset = false; observeBoth = false; gYearUnknown = false
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
        s.recurring = e.recurring; s.recurringManual = e.recurring != EventDefaults.recurring(e.type); s.monthly = e.isMonthly
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
