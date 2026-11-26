import Domain

/// Что подставить в редактор нового события.
struct EditorSeed: Hashable {
    var id: String?
    var calendar: CalendarKind?
    var year: Int?
    var month: Int?
    var hebrewMonth: HebrewMonth?
    var day: Int?
    var afterSunset = false
    var type: EventType?
    var title: String?
    var listId: String?
}

/// Экраны внутри вкладок.
enum Route: Hashable {
    case event(String)
    case editor(EditorSeed)
    case lists
    case listCreate
    case listJoin(String?)
    case listDetail(String)
    case settings
    case cityPicker
    case about
    case feedback(String?)
    case holidays
}
