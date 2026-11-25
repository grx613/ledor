package org.ledor.domain.holiday

import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.reminder.City
import org.ledor.domain.reminder.CitySunsetProvider
import org.ledor.domain.reminder.SunsetHints
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class HolidayKind { FESTIVAL, FAST, MEMORIAL }

/**
 * Праздники встроенного списка. Имена — контракт (эталонные векторы, iOS).
 * [candles] — накануне первого дня зажигают свечи.
 * [dawn] — пост, который начинается не накануне вечером, а на рассвете и длится до выхода звёзд.
 */
enum class Holiday(val kind: HolidayKind, val candles: Boolean = false, val dawn: Boolean = false) {
    ROSH_HASHANA(HolidayKind.FESTIVAL, true),
    TZOM_GEDALIAH(HolidayKind.FAST, dawn = true),
    YOM_KIPPUR(HolidayKind.FESTIVAL, true),
    SUKKOT(HolidayKind.FESTIVAL, true),
    SHMINI_ATZERET(HolidayKind.FESTIVAL, true),
    CHANUKAH(HolidayKind.FESTIVAL),
    ASARA_BETEVET(HolidayKind.FAST, dawn = true),
    TU_BISHVAT(HolidayKind.FESTIVAL),
    TAANIT_ESTHER(HolidayKind.FAST, dawn = true),
    PURIM(HolidayKind.FESTIVAL),
    SHUSHAN_PURIM(HolidayKind.FESTIVAL),
    PESACH(HolidayKind.FESTIVAL, true),
    PESACH_VII(HolidayKind.FESTIVAL, true),
    YOM_HASHOAH(HolidayKind.MEMORIAL),
    YOM_HAZIKARON(HolidayKind.MEMORIAL),
    YOM_HAATZMAUT(HolidayKind.FESTIVAL),
    LAG_BAOMER(HolidayKind.FESTIVAL),
    YOM_YERUSHALAYIM(HolidayKind.FESTIVAL),
    SHAVUOT(HolidayKind.FESTIVAL, true),
    TZOM_TAMMUZ(HolidayKind.FAST, dawn = true),
    TISHA_BAV(HolidayKind.FAST),
    TU_BAV(HolidayKind.FESTIVAL),
}

/** [start] — первый день (гражданская дата); праздник начинается накануне вечером. */
data class HolidayOccurrence(val holiday: Holiday, val start: LocalDate, val days: Int) {
    val end: LocalDate get() = start.plusDays(days - 1L)
}

/** Правила — docs/03-calendar-domain.md §12; проверены на spec/golden/holidays.json. */
object Holidays {
    private val M = HebrewCalendarMath

    fun inHebrewYear(year: Int, inIsrael: Boolean): List<HolidayOccurrence> {
        fun day(m: HebrewMonth, d: Int) = M.toGregorian(HebrewDate(year, m, d))
        /** Пост, выпавший на субботу, переносится на воскресенье. */
        fun fast(m: HebrewMonth, d: Int) = day(m, d).let { if (it.dayOfWeek == DayOfWeek.SATURDAY) it.plusDays(1) else it }
        val adar = if (M.isLeapYear(year)) HebrewMonth.ADAR_II else HebrewMonth.ADAR
        val yomTovDays = if (inIsrael) 1 else 2

        val esther = day(adar, 13).let { if (it.dayOfWeek == DayOfWeek.SATURDAY) it.minusDays(2) else it }
        val shoah = day(HebrewMonth.NISAN, 27).let {
            when (it.dayOfWeek) { DayOfWeek.FRIDAY -> it.minusDays(1); DayOfWeek.SUNDAY -> it.plusDays(1); else -> it }
        }
        val atzmaut = day(HebrewMonth.IYAR, 5).let {
            when (it.dayOfWeek) {
                DayOfWeek.FRIDAY -> it.minusDays(1); DayOfWeek.SATURDAY -> it.minusDays(2)
                DayOfWeek.MONDAY -> it.plusDays(1); else -> it
            }
        }
        fun o(h: Holiday, start: LocalDate, days: Int = 1) = HolidayOccurrence(h, start, days)
        return listOf(
            o(Holiday.ROSH_HASHANA, day(HebrewMonth.TISHREI, 1), 2),
            o(Holiday.TZOM_GEDALIAH, fast(HebrewMonth.TISHREI, 3)),
            o(Holiday.YOM_KIPPUR, day(HebrewMonth.TISHREI, 10)),
            o(Holiday.SUKKOT, day(HebrewMonth.TISHREI, 15), 7),
            o(Holiday.SHMINI_ATZERET, day(HebrewMonth.TISHREI, 22), yomTovDays),
            o(Holiday.CHANUKAH, day(HebrewMonth.KISLEV, 25), 8),
            o(Holiday.ASARA_BETEVET, day(HebrewMonth.TEVET, 10)),
            o(Holiday.TU_BISHVAT, day(HebrewMonth.SHEVAT, 15)),
            o(Holiday.TAANIT_ESTHER, esther),
            o(Holiday.PURIM, day(adar, 14)),
            o(Holiday.SHUSHAN_PURIM, day(adar, 15)),
            o(Holiday.PESACH, day(HebrewMonth.NISAN, 15), if (inIsrael) 7 else 8),
            o(Holiday.PESACH_VII, day(HebrewMonth.NISAN, 21), yomTovDays),
            o(Holiday.YOM_HASHOAH, shoah),
            o(Holiday.YOM_HAZIKARON, atzmaut.minusDays(1)),
            o(Holiday.YOM_HAATZMAUT, atzmaut),
            o(Holiday.LAG_BAOMER, day(HebrewMonth.IYAR, 18)),
            o(Holiday.YOM_YERUSHALAYIM, day(HebrewMonth.IYAR, 28)),
            o(Holiday.SHAVUOT, day(HebrewMonth.SIVAN, 6), yomTovDays),
            o(Holiday.TZOM_TAMMUZ, fast(HebrewMonth.TAMMUZ, 17)),
            o(Holiday.TISHA_BAV, fast(HebrewMonth.AV, 9)),
            o(Holiday.TU_BAV, day(HebrewMonth.AV, 15)),
        )
    }

    /** Праздники, хотя бы один день которых попадает в [from, toInclusive], по возрастанию даты. */
    fun between(from: LocalDate, toInclusive: LocalDate, inIsrael: Boolean): List<HolidayOccurrence> {
        if (toInclusive.isBefore(from)) return emptyList()
        val first = maxOf(M.hebrewYearOf(from), M.MIN_HEBREW_YEAR)
        val last = minOf(M.hebrewYearOf(toInclusive), M.MAX_HEBREW_YEAR)
        return (first..last).flatMap { inHebrewYear(it, inIsrael) }
            .filter { !it.end.isBefore(from) && !it.start.isAfter(toInclusive) }
            .sortedBy { it.start }
    }

    /**
     * День, когда читают Изкор: Йом Кипур, Шмини Ацерет, последний день Песаха и Шавуот
     * (в диаспоре у двух последних — второй день). null — в этот праздник Изкор не читают.
     */
    fun yizkorDay(o: HolidayOccurrence): LocalDate? = when (o.holiday) {
        Holiday.YOM_KIPPUR, Holiday.SHMINI_ATZERET -> o.start
        Holiday.PESACH_VII, Holiday.SHAVUOT -> o.end
        else -> null
    }

    /** Субботы в [from, toInclusive]. */
    fun shabbats(from: LocalDate, toInclusive: LocalDate): List<LocalDate> {
        val first = from.plusDays(((DayOfWeek.SATURDAY.value - from.dayOfWeek.value + 7) % 7).toLong())
        return generateSequence(first) { it.plusDays(7) }.takeWhile { !it.isAfter(toInclusive) }.toList()
    }
}

/** [afterNightfall] — канун выпал на субботу: свечи зажигают после выхода звёзд, не раньше [time]. */
data class CandleLighting(val time: Instant, val afterNightfall: Boolean)

object CandleLightingCalculator {
    /** За сколько минут до захода зажигают свечи: Иерусалим — 40, Хайфа — 30, остальные — 18. */
    fun minutesBeforeSunset(city: City): Int = when (city.id) {
        "il-jerusalem" -> 40
        "il-haifa" -> 30
        else -> 18
    }

    /**
     * Зажигание свечей накануне дня [day] (Шабата или первого дня праздника).
     * До захода время округляется вниз до минуты, после выхода звёзд — вверх. null — в этот день нет захода.
     */
    fun forDay(day: LocalDate, city: City): CandleLighting? {
        val eve = day.minusDays(1)
        val p = CitySunsetProvider(city)
        val sunset = p.sunsetOrNull(eve) ?: return null
        if (eve.dayOfWeek != DayOfWeek.SATURDAY) {
            return CandleLighting(sunset.minusSeconds(minutesBeforeSunset(city) * 60L).truncatedTo(ChronoUnit.MINUTES), false)
        }
        val nightfall = p.nightfallOrNull(eve)?.takeIf { it.isAfter(sunset) }
            ?: sunset.plusSeconds(SunsetHints.NIGHTFALL_FALLBACK_MINUTES * 60)
        return CandleLighting(nightfall.plusSeconds(59).truncatedTo(ChronoUnit.MINUTES), true)
    }
}
