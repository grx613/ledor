package org.ledor.ui.format

import android.content.Context
import org.ledor.R
import org.ledor.domain.calendar.Gematriya
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.Event
import org.ledor.domain.event.EventType
import org.ledor.domain.event.GregorianParts
import org.ledor.domain.event.HebrewParts
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Все тексты с датами — только здесь. Язык форматирования берётся из ресурсов (R.string.locale_tag). */
class DateFormatters(private val context: Context) {
    private val res = context.resources
    val locale: Locale = Locale.forLanguageTag(context.getString(R.string.locale_tag))

    private val monthsNom = res.getStringArray(R.array.hebrew_months_nom)
    private val monthsGen = res.getStringArray(R.array.hebrew_months_gen)
    private val monthsShort = res.getStringArray(R.array.hebrew_months_short)

    private val fFull = DateTimeFormatter.ofPattern("d MMMM yyyy", locale)
    private val fWeekday = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)
    private val fShortWeekday = DateTimeFormatter.ofPattern("EE, d MMMM yyyy", locale)
    private val fDayMonth = DateTimeFormatter.ofPattern("d MMMM", locale)
    private val fDayMonthWeekday = DateTimeFormatter.ofPattern("EE, d MMMM", locale)
    private val fMonthTitle = DateTimeFormatter.ofPattern("LLLL yyyy", locale)

    fun monthNom(m: HebrewMonth): String = monthsNom[m.ordinal]
    fun monthGen(m: HebrewMonth): String = monthsGen[m.ordinal]
    fun monthShort(m: HebrewMonth): String = monthsShort[m.ordinal]

    /** Иврит и идиш: числа буквами — כ״ח בניסן תש״ן. */
    private val letters = res.getBoolean(R.bool.hebrew_letters)
    private fun num(n: Int) = if (letters) Gematriya.format(n) else n.toString()

    fun hebrewDate(h: HebrewDate) = "${num(h.day)} ${monthGen(h.month)} ${num(h.year)}"
    fun hebrewDayMonth(day: Int, m: HebrewMonth) = "${num(day)} ${monthGen(m)}"
    fun hebrewParts(h: HebrewParts) = h.year?.let { "${num(h.day)} ${monthGen(h.month)} ${num(it)}" } ?: hebrewDayMonth(h.day, h.month)
    fun hebrewMonthTitle(year: Int, m: HebrewMonth) = "${monthNom(m)} ${num(year)}"

    fun gregorian(d: LocalDate): String = fFull.format(d)
    fun gregorianWeekday(d: LocalDate): String = fWeekday.format(d)
    fun gregorianShortWeekday(d: LocalDate): String = fShortWeekday.format(d)
    fun gregorianDayMonth(d: LocalDate): String = fDayMonth.format(d)
    fun gregorianDayMonthWeekday(d: LocalDate): String = fDayMonthWeekday.format(d)
    fun gregorianParts(g: GregorianParts): String =
        g.toLocalDateOrNull()?.let { gregorian(it) } ?: fDayMonth.format(LocalDate.of(2000, g.month, g.day))
    fun gregorianMonthTitle(ym: YearMonth): String = fMonthTitle.format(ym).replaceFirstChar { it.titlecase(locale) }
    fun gregorianMonthName(m: Int): String = Month.of(m).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    fun weekdayShort(d: java.time.DayOfWeek): String = d.getDisplayName(TextStyle.SHORT_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }

    fun relativeDays(days: Long): String = when (days) {
        0L -> context.getString(R.string.rel_today)
        1L -> context.getString(R.string.rel_tomorrow)
        else -> res.getQuantityString(R.plurals.rel_in_days, days.toInt(), days.toInt())
    }

    fun typeName(t: EventType): String = context.getString(when (t) {
        EventType.BIRTHDAY -> R.string.type_birthday
        EventType.YAHRZEIT -> R.string.type_yahrzeit
        EventType.BAR_MITZVAH -> R.string.type_bar_mitzvah
        EventType.BAT_MITZVAH -> R.string.type_bat_mitzvah
        EventType.WEDDING -> R.string.type_wedding
        EventType.OTHER -> R.string.type_other
    })

    /** Название типа для конкретного события (для «Другое» — своё название, свадьба через годы — «Годовщина свадьбы»). */
    fun typeLabel(e: Event, yearsSince: Int? = null): String = when {
        e.type == EventType.OTHER && !e.customLabel.isNullOrBlank() -> e.customLabel!!
        e.type == EventType.WEDDING && (yearsSince ?: 0) > 0 -> context.getString(R.string.type_wedding_anniversary)
        else -> typeName(e.type)
    }

    fun agePhrase(t: EventType, n: Int?): String? {
        if (n == null || n <= 0) return null
        return when (t) {
            EventType.BIRTHDAY -> res.getQuantityString(R.plurals.age_turns, n, n)
            EventType.WEDDING -> res.getQuantityString(R.plurals.age_wedding, n, n)
            EventType.YAHRZEIT -> context.getString(R.string.age_yahrzeit, n)
            else -> res.getQuantityString(R.plurals.age_years, n, n)
        }
    }
}
