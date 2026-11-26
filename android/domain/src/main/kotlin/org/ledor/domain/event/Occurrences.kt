package org.ledor.domain.event

import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath as M
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import java.time.LocalDate
import java.time.Month

object AnniversaryCalculator {

    fun hebrewAnniversary(
        rule: AnniversaryRule,
        origin: HebrewParts,
        targetYear: Int,
        policy: YahrzeitAdarPolicy = YahrzeitAdarPolicy.ADAR_I,
    ): HebrewDate = when (rule) {
        AnniversaryRule.BIRTHDAY -> birthday(origin, targetYear)
        AnniversaryRule.YAHRZEIT -> yahrzeit(origin, targetYear, policy)
    }

    private fun lastAdar(y: Int) = if (M.isLeapYear(y)) HebrewMonth.ADAR_II else HebrewMonth.ADAR

    private fun birthday(o: HebrewParts, y: Int): HebrewDate {
        val leap = M.isLeapYear(y)
        val m = o.month
        val d = o.day
        return when {
            m == HebrewMonth.ADAR || m == HebrewMonth.ADAR_II -> HebrewDate(y, lastAdar(y), d)
            m == HebrewMonth.ADAR_I && d == 30 && !leap -> HebrewDate(y, HebrewMonth.NISAN, 1)
            m == HebrewMonth.ADAR_I -> HebrewDate(y, if (leap) HebrewMonth.ADAR_I else HebrewMonth.ADAR, d)
            m == HebrewMonth.CHESHVAN && d == 30 && !M.isCheshvanLong(y) -> HebrewDate(y, HebrewMonth.KISLEV, 1)
            m == HebrewMonth.KISLEV && d == 30 && M.isKislevShort(y) -> HebrewDate(y, HebrewMonth.TEVET, 1)
            else -> HebrewDate(y, m, d)
        }
    }

    private fun yahrzeit(o: HebrewParts, y: Int, policy: YahrzeitAdarPolicy): HebrewDate {
        val leap = M.isLeapYear(y)
        val m = o.month
        val d = o.day
        val oy = o.year
        if (m == HebrewMonth.CHESHVAN && d == 30 && oy != null && !M.isCheshvanLong(oy + 1)) {
            return HebrewDate(y, HebrewMonth.CHESHVAN, M.daysInMonth(y, HebrewMonth.CHESHVAN))
        }
        if (m == HebrewMonth.KISLEV && d == 30 && oy != null && M.isKislevShort(oy + 1)) {
            return HebrewDate(y, HebrewMonth.KISLEV, M.daysInMonth(y, HebrewMonth.KISLEV))
        }
        val base = when {
            m == HebrewMonth.ADAR_II -> HebrewDate(y, lastAdar(y), d)
            m == HebrewMonth.ADAR_I && d == 30 && !leap -> HebrewDate(y, HebrewMonth.SHEVAT, 30)
            m == HebrewMonth.ADAR_I -> HebrewDate(y, if (leap) HebrewMonth.ADAR_I else HebrewMonth.ADAR, d)
            m == HebrewMonth.ADAR -> HebrewDate(
                y,
                when {
                    !leap -> HebrewMonth.ADAR
                    policy == YahrzeitAdarPolicy.ADAR_I -> HebrewMonth.ADAR_I
                    else -> HebrewMonth.ADAR_II
                },
                d,
            )
            else -> HebrewDate(y, m, d)
        }
        return when {
            base.month == HebrewMonth.CHESHVAN && base.day == 30 && !M.isCheshvanLong(y) -> HebrewDate(y, HebrewMonth.KISLEV, 1)
            base.month == HebrewMonth.KISLEV && base.day == 30 && M.isKislevShort(y) -> HebrewDate(y, HebrewMonth.TEVET, 1)
            else -> base
        }
    }

    /** 29 февраля в невисокосный год → 28 февраля (D-11). */
    fun gregorianAnniversary(origin: GregorianParts, targetYear: Int): LocalDate {
        if (origin.month == 2 && origin.day == 29 && !java.time.Year.isLeap(targetYear.toLong())) {
            return LocalDate.of(targetYear, 2, 28)
        }
        return LocalDate.of(targetYear, Month.of(origin.month), origin.day)
    }
}

data class Occurrence(
    val eventId: String,
    val date: LocalDate,
    val hebrewDate: HebrewDate,
    val yearsSince: Int?,
    val startsEveBefore: Boolean,
) {
    /** По какому календарю это наступление: еврейский день начинается накануне вечером. */
    val calendar: CalendarKind get() = if (startsEveBefore) CalendarKind.HEBREW else CalendarKind.GREGORIAN
}

object OccurrenceCalculator {

    fun inHebrewYear(e: Event, year: Int, policy: YahrzeitAdarPolicy): Occurrence? {
        if (!e.recurring) return oneOff(e)?.takeIf { M.hebrewYearOf(it.date) == year }
        val h = e.date.hebrew ?: return null
        if (!M.isHebrewYearSupported(year)) return null
        if (h.year != null && year < h.year + EventDefaults.firstOccurrenceOffset(e.type)) return null
        val hd = AnniversaryCalculator.hebrewAnniversary(EventDefaults.rule(e.type), h, year, policy)
        if (!M.isValid(hd.year, hd.month, hd.day)) return null
        return Occurrence(e.id, M.toGregorian(hd), hd, h.year?.let { year - it }, true)
    }

    fun inGregorianYear(e: Event, year: Int): Occurrence? {
        if (!e.recurring) return oneOff(e)?.takeIf { it.date.year == year }
        val g = e.date.gregorian ?: return null
        if (!M.isGregorianYearSupported(year)) return null
        if (g.year != null && year < g.year + EventDefaults.firstOccurrenceOffset(e.type)) return null
        val date = AnniversaryCalculator.gregorianAnniversary(g, year)
        return Occurrence(e.id, date, M.fromGregorian(date), g.year?.let { year - it }, false)
    }

    fun oneOff(e: Event): Occurrence? {
        val date = e.date.gregorian?.toLocalDateOrNull() ?: return null
        val heb = e.date.hebrew?.toHebrewDateOrNull() ?: M.fromGregorian(date)
        return Occurrence(e.id, date, heb, 0, e.observeBy == CalendarKind.HEBREW)
    }

    /** Календари, по которым отмечается повторяющееся событие (основной — первым). */
    private fun calendars(e: Event): List<CalendarKind> =
        if (e.observeBoth && e.date.hebrew?.year != null && e.date.gregorian?.year != null) {
            listOf(e.observeBy, if (e.observeBy == CalendarKind.HEBREW) CalendarKind.GREGORIAN else CalendarKind.HEBREW)
        } else listOf(e.observeBy)

    /** Если в каком-то году обе даты совпали — одно наступление (по основному календарю). */
    private fun merge(list: List<Occurrence>): List<Occurrence> = list.distinctBy { it.date }.sortedBy { it.date }

    fun between(e: Event, from: LocalDate, toInclusive: LocalDate, policy: YahrzeitAdarPolicy): List<Occurrence> {
        if (toInclusive.isBefore(from)) return emptyList()
        if (!e.recurring) return listOfNotNull(oneOff(e)?.takeIf { !it.date.isBefore(from) && !it.date.isAfter(toInclusive) })
        val list = calendars(e).flatMap { cal ->
            if (cal == CalendarKind.HEBREW) {
                (M.hebrewYearOf(from)..M.hebrewYearOf(toInclusive)).mapNotNull { inHebrewYear(e, it, policy) }
            } else {
                (from.year..toInclusive.year).mapNotNull { inGregorianYear(e, it) }
            }
        }
        return merge(list.filter { !it.date.isBefore(from) && !it.date.isAfter(toInclusive) })
    }

    fun next(e: Event, today: LocalDate, policy: YahrzeitAdarPolicy): Occurrence? {
        if (!e.recurring) return oneOff(e)?.takeIf { !it.date.isBefore(today) }
        return merge(calendars(e).mapNotNull { nextBy(e, it, today, policy) }).firstOrNull()
    }

    /** Ближайшее наступление по каждому календарю события: для «по обоим» — две даты (совпавшие — одна). */
    fun nextEach(e: Event, today: LocalDate, policy: YahrzeitAdarPolicy): List<Occurrence> {
        if (!e.recurring) return listOfNotNull(next(e, today, policy))
        return merge(calendars(e).mapNotNull { nextBy(e, it, today, policy) })
    }

    private fun nextBy(e: Event, cal: CalendarKind, today: LocalDate, policy: YahrzeitAdarPolicy): Occurrence? {
        val candidates = if (cal == CalendarKind.HEBREW) {
            val y = M.hebrewYearOf(today)
            (y..y + 2).mapNotNull { inHebrewYear(e, it, policy) }
        } else {
            (today.year..today.year + 1).mapNotNull { inGregorianYear(e, it) }
        }
        val upcoming = candidates.filter { !it.date.isBefore(today) }.minByOrNull { it.date }
        if (upcoming != null) return upcoming
        // Событие начинается в будущем (например, свадьба через 3 года) — ищем дальше.
        val origin = e.date.originYear(cal) ?: return null
        return if (cal == CalendarKind.HEBREW) {
            inHebrewYear(e, origin + EventDefaults.firstOccurrenceOffset(e.type), policy)?.takeIf { !it.date.isBefore(today) }
        } else {
            inGregorianYear(e, origin + EventDefaults.firstOccurrenceOffset(e.type))?.takeIf { !it.date.isBefore(today) }
        }
    }
}

data class MitzvahHint(val type: EventType, val hebrewDate: HebrewDate, val date: LocalDate)

object MitzvahCalculator {
    fun mitzvahHints(birth: HebrewParts, today: LocalDate): List<MitzvahHint> {
        val y = birth.year ?: return emptyList()
        return listOf(EventType.BAR_MITZVAH to 13, EventType.BAT_MITZVAH to 12).mapNotNull { (type, age) ->
            if (!M.isHebrewYearSupported(y + age)) return@mapNotNull null
            val hd = AnniversaryCalculator.hebrewAnniversary(AnniversaryRule.BIRTHDAY, birth, y + age)
            val date = M.toGregorian(hd)
            if (date.isBefore(today)) null else MitzvahHint(type, hd, date)
        }
    }
}
