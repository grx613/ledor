import AppCore
import Domain
import Foundation

/// Все тексты с датами — только здесь (как `DateFormatters` на Android).
struct Formatters {
    let locale = L.locale

    private func formatter(_ pattern: String) -> DateFormatter {
        let f = DateFormatter()
        f.locale = locale
        f.timeZone = TimeZone(identifier: "UTC")
        f.dateFormat = pattern
        return f
    }

    private func date(_ d: CivilDate) -> Date {
        Zoned.instant(d, .noon, TimeZone(identifier: "UTC")!)
    }

    func monthNom(_ m: HebrewMonth) -> String { L.t("hebrew_months_nom.\(index(m))") }
    func monthGen(_ m: HebrewMonth) -> String { L.t("hebrew_months_gen.\(index(m))") }
    func monthShort(_ m: HebrewMonth) -> String { L.t("hebrew_months_short.\(index(m))") }
    private func index(_ m: HebrewMonth) -> Int { HebrewMonth.allCases.firstIndex(of: m)! }

    private func num(_ n: Int) -> String { L.hebrewLetters ? Gematriya.format(n) : String(n) }

    func hebrewDate(_ h: HebrewDate) -> String { "\(num(h.day)) \(monthGen(h.month)) \(num(h.year))" }
    func hebrewDayMonth(_ day: Int, _ m: HebrewMonth) -> String { "\(num(day)) \(monthGen(m))" }
    func hebrewParts(_ h: HebrewParts) -> String {
        h.year.map { "\(num(h.day)) \(monthGen(h.month)) \(num($0))" } ?? hebrewDayMonth(h.day, h.month)
    }
    func hebrewMonthTitle(_ year: Int, _ m: HebrewMonth) -> String { "\(monthNom(m)) \(num(year))" }
    func hebrewYear(_ y: Int) -> String { num(y) }

    func gregorian(_ d: CivilDate) -> String { formatter("d MMMM yyyy").string(from: date(d)) }
    func gregorianWeekday(_ d: CivilDate) -> String { formatter("EEEE, d MMMM yyyy").string(from: date(d)) }
    func gregorianShortWeekday(_ d: CivilDate) -> String { formatter("EE, d MMMM yyyy").string(from: date(d)) }
    func gregorianDayMonth(_ d: CivilDate) -> String { formatter("d MMMM").string(from: date(d)) }
    func gregorianDayMonthWeekday(_ d: CivilDate) -> String { formatter("EE, d MMMM").string(from: date(d)) }
    func gregorianParts(_ g: GregorianParts) -> String {
        g.civilDate.map(gregorian) ?? gregorianDayMonth(CivilDate(year: 2000, month: g.month, day: g.day)!)
    }
    func gregorianMonthTitle(_ year: Int, _ month: Int) -> String {
        formatter("LLLL yyyy").string(from: date(CivilDate(year: year, month: month, day: 1)!)).capitalizedFirst(locale)
    }
    func gregorianMonthName(_ m: Int) -> String {
        formatter("LLLL").string(from: date(CivilDate(year: 2001, month: m, day: 1)!)).capitalizedFirst(locale)
    }
    /// 1 = понедельник … 7 = воскресенье.
    func weekdayShort(_ isoWeekday: Int) -> String {
        formatter("ccc").string(from: date(CivilDate(year: 2024, month: 1, day: isoWeekday)!)).capitalizedFirst(locale)
    }
    func time(_ instant: Date, _ zone: TimeZone = .current) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.timeZone = zone
        f.dateFormat = "H:mm"
        return f.string(from: instant)
    }
    func time(_ t: TimeOfDay) -> String { String(format: "%d:%02d", t.hour, t.minute) }
    func dateTime(_ instant: Date) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.dateFormat = "d MMMM, H:mm"
        return f.string(from: instant)
    }

    /// Две даты наступления (D-54, заменяет D-46): главная крупно, под ней — тот же день в другом календаре.
    /// По еврейскому календарю первой идёт календарь `first` (вкладка); по григорианскому — всегда григорианская:
    /// еврейская дата того же дня заголовком строки не бывает.
    func occurrenceDates(_ o: Occurrence, first: CalendarKind? = nil) -> [String] {
        let greg = gregorianShortWeekday(o.date)
        let heb = hebrewDate(o.hebrewDate)
        return o.calendar == .hebrew && (first ?? o.calendar) == .hebrew ? [heb, greg] : [greg, heb]
    }

    /// Одной строкой, григорианская дата первой: «ср, 5 мая 2027 (28 Нисана 5787)».
    func occurrenceDatesInline(_ o: Occurrence) -> String {
        let d = occurrenceDates(o, first: .gregorian)
        return "\(d[0]) (\(d[1]))"
    }

    func relativeDays(_ days: Int) -> String {
        switch days {
        case 0: L.t("rel_today")
        case 1: L.t("rel_tomorrow")
        default: L.p("rel_in_days", days)
        }
    }

    func typeName(_ t: EventType) -> String {
        switch t {
        case .birthday: L.t("type_birthday")
        case .yahrzeit: L.t("type_yahrzeit")
        case .barMitzvah: L.t("type_bar_mitzvah")
        case .batMitzvah: L.t("type_bat_mitzvah")
        case .wedding: L.t("type_wedding")
        case .other: L.t("type_other")
        }
    }

    /// Для «Другое» — своё название; свадьба через годы — «Годовщина свадьбы».
    func typeLabel(_ e: Event, _ yearsSince: Int? = nil) -> String {
        if e.type == .other, let label = e.customLabel, !label.trimmingCharacters(in: .whitespaces).isEmpty { return label }
        if e.type == .wedding, (yearsSince ?? 0) > 0 { return L.t("type_wedding_anniversary") }
        return typeName(e.type)
    }

    func agePhrase(_ t: EventType, _ n: Int?) -> String? {
        guard let n, n > 0 else { return nil }
        switch t {
        case .birthday: return L.p("age_turns", n)
        case .wedding: return L.p("age_wedding", n)
        case .yahrzeit: return L.f("age_yahrzeit", n)
        default: return L.p("age_years", n)
        }
    }

    func listName(_ list: ListRecord?) -> String {
        guard let list, list.id != localListId else { return L.t("list_personal") }
        return list.name
    }
}

extension String {
    func capitalizedFirst(_ locale: Locale) -> String {
        guard let first else { return self }
        return String(first).uppercased(with: locale) + dropFirst()
    }
}
