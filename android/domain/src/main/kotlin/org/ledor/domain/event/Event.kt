package org.ledor.domain.event

import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import java.time.LocalDate

enum class EventType { BIRTHDAY, YAHRZEIT, BAR_MITZVAH, BAT_MITZVAH, WEDDING, OTHER;
    companion object { fun parse(s: String?) = entries.firstOrNull { it.name == s } ?: OTHER }
}

data class GregorianParts(val year: Int?, val month: Int, val day: Int) {
    fun toLocalDateOrNull(): LocalDate? = year?.let { runCatching { LocalDate.of(it, month, day) }.getOrNull() }
}

data class HebrewParts(val year: Int?, val month: HebrewMonth, val day: Int) {
    fun toHebrewDateOrNull(): HebrewDate? =
        year?.takeIf { HebrewCalendarMath.isValid(it, month, day) }?.let { HebrewDate(it, month, day) }

    companion object { fun of(d: HebrewDate) = HebrewParts(d.year, d.month, d.day) }
}

data class EventDate(
    val inputCalendar: CalendarKind,
    val gregorian: GregorianParts?,
    val afterSunset: Boolean,
    val hebrew: HebrewParts?,
) {
    /** Год исходной даты в календаре, по которому событие отмечается. */
    fun originYear(observeBy: CalendarKind): Int? =
        if (observeBy == CalendarKind.HEBREW) hebrew?.year else gregorian?.year

    companion object {
        /** Григорианский ввод: при известном годе вычисляется еврейская дата. */
        fun fromGregorian(g: GregorianParts, afterSunset: Boolean): EventDate {
            val full = g.toLocalDateOrNull()
            return if (full != null && HebrewCalendarMath.isGregorianYearSupported(full.year)) {
                EventDate(CalendarKind.GREGORIAN, g, afterSunset,
                    HebrewParts.of(HebrewCalendarMath.fromGregorian(full, afterSunset)))
            } else EventDate(CalendarKind.GREGORIAN, g, false, null)
        }

        /** Еврейский ввод: при известном годе вычисляется григорианская дата. */
        fun fromHebrew(h: HebrewParts): EventDate {
            val full = h.toHebrewDateOrNull()
            return if (full != null) {
                val g = HebrewCalendarMath.toGregorian(full)
                EventDate(CalendarKind.HEBREW, GregorianParts(g.year, g.monthValue, g.dayOfMonth), false, h)
            } else EventDate(CalendarKind.HEBREW, null, false, h)
        }
    }
}

data class Event(
    val id: String,
    val listId: String,
    val title: String,
    val type: EventType,
    val customLabel: String?,
    val recurring: Boolean,
    val observeBy: CalendarKind,
    val date: EventDate,
    val note: String?,
    /** Отмечать и по второму календарю тоже (B-1). Требует известного года; observeBy — основной календарь. */
    val observeBoth: Boolean = false,
)

enum class AnniversaryRule { BIRTHDAY, YAHRZEIT }
enum class YahrzeitAdarPolicy { ADAR_I, ADAR_II }

object EventDefaults {
    fun recurring(type: EventType): Boolean = when (type) {
        EventType.BAR_MITZVAH, EventType.BAT_MITZVAH -> false
        else -> true
    }

    fun rule(type: EventType): AnniversaryRule =
        if (type == EventType.YAHRZEIT) AnniversaryRule.YAHRZEIT else AnniversaryRule.BIRTHDAY

    /** Сам день рождения/смерти наступлением не считается. */
    fun firstOccurrenceOffset(type: EventType): Int = when (type) {
        EventType.BIRTHDAY, EventType.YAHRZEIT -> 1
        else -> 0
    }
}

enum class ValidationError {
    TITLE_EMPTY, TITLE_TOO_LONG, CUSTOM_LABEL_REQUIRED, CUSTOM_LABEL_TOO_LONG, CUSTOM_LABEL_NOT_ALLOWED,
    DATE_MISSING, DATE_INVALID, YEAR_OUT_OF_RANGE, ONE_OFF_NEEDS_YEAR, OBSERVE_NEEDS_YEAR, NOTE_TOO_LONG,
}

object EventValidator {
    const val TITLE_MAX = 120
    const val LABEL_MAX = 60
    const val NOTE_MAX = 500

    fun validate(e: Event): List<ValidationError> = buildList {
        val title = e.title.trim()
        if (title.isEmpty()) add(ValidationError.TITLE_EMPTY)
        if (title.length > TITLE_MAX) add(ValidationError.TITLE_TOO_LONG)
        val label = e.customLabel?.trim().orEmpty()
        if (e.type == EventType.OTHER) {
            if (label.isEmpty()) add(ValidationError.CUSTOM_LABEL_REQUIRED)
            if (label.length > LABEL_MAX) add(ValidationError.CUSTOM_LABEL_TOO_LONG)
        } else if (label.isNotEmpty()) add(ValidationError.CUSTOM_LABEL_NOT_ALLOWED)
        if ((e.note?.length ?: 0) > NOTE_MAX) add(ValidationError.NOTE_TOO_LONG)
        addAll(validateDate(e.date))
        val d = e.date
        val inputYear = if (d.inputCalendar == CalendarKind.GREGORIAN) d.gregorian?.year else d.hebrew?.year
        if (!e.recurring && inputYear == null) add(ValidationError.ONE_OFF_NEEDS_YEAR)
        if (e.recurring && (e.observeBy != d.inputCalendar || e.observeBoth) && inputYear == null) add(ValidationError.OBSERVE_NEEDS_YEAR)
    }.distinct()

    fun validateDate(d: EventDate): List<ValidationError> = buildList {
        when (d.inputCalendar) {
            CalendarKind.GREGORIAN -> {
                val g = d.gregorian ?: run { add(ValidationError.DATE_MISSING); return@buildList }
                if (g.month !in 1..12) { add(ValidationError.DATE_INVALID); return@buildList }
                if (g.year != null) {
                    if (!HebrewCalendarMath.isGregorianYearSupported(g.year)) add(ValidationError.YEAR_OUT_OF_RANGE)
                    else if (g.toLocalDateOrNull() == null) add(ValidationError.DATE_INVALID)
                } else {
                    val max = java.time.Month.of(g.month).maxLength()
                    if (g.day !in 1..max) add(ValidationError.DATE_INVALID)
                }
            }
            CalendarKind.HEBREW -> {
                val h = d.hebrew ?: run { add(ValidationError.DATE_MISSING); return@buildList }
                if (h.year != null) {
                    if (!HebrewCalendarMath.isHebrewYearSupported(h.year)) add(ValidationError.YEAR_OUT_OF_RANGE)
                    else if (!HebrewCalendarMath.isValid(h.year, h.month, h.day)) add(ValidationError.DATE_INVALID)
                } else if (h.day !in 1..h.month.maxDayWithoutYear) add(ValidationError.DATE_INVALID)
            }
        }
    }
}
