package org.ledor.domain.calendar

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.LocalDate

/** 14 значений. Строковые имена — контракт (БД, Supabase, iOS, сайт). */
enum class HebrewMonth {
    NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL, TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT,
    ADAR, ADAR_I, ADAR_II;

    /** Максимальный возможный день месяца, когда год неизвестен. */
    val maxDayWithoutYear: Int
        get() = when (this) {
            IYAR, TAMMUZ, ELUL, TEVET, ADAR, ADAR_II -> 29
            else -> 30
        }

    companion object {
        /** Все 14 значений в порядке года от Тишрея (для выбора без года). */
        val ORDER_WITHOUT_YEAR: List<HebrewMonth> = listOf(
            TISHREI, CHESHVAN, KISLEV, TEVET, SHEVAT, ADAR, ADAR_I, ADAR_II,
            NISAN, IYAR, SIVAN, TAMMUZ, AV, ELUL,
        )

        fun parse(code: String?): HebrewMonth? = code?.let { c -> entries.firstOrNull { it.name == c } }
    }
}

enum class CalendarKind { HEBREW, GREGORIAN }

data class HebrewDate(val year: Int, val month: HebrewMonth, val day: Int)

/** Единственное место, где используется KosherJava для дат. */
object HebrewCalendarMath {
    const val MIN_GREGORIAN_YEAR = 1600
    const val MAX_GREGORIAN_YEAR = 2200
    const val MIN_HEBREW_YEAR = 5360
    const val MAX_HEBREW_YEAR = 5960

    fun isHebrewYearSupported(year: Int) = year in MIN_HEBREW_YEAR..MAX_HEBREW_YEAR
    fun isGregorianYearSupported(year: Int) = year in MIN_GREGORIAN_YEAR..MAX_GREGORIAN_YEAR

    fun isLeapYear(year: Int): Boolean = JewishDate.isJewishLeapYear(year)
    fun daysInYear(year: Int): Int = JewishDate.getDaysInJewishYear(year)
    fun isCheshvanLong(year: Int): Boolean = daysInYear(year) % 10 == 5
    fun isKislevShort(year: Int): Boolean = daysInYear(year) % 10 == 3

    fun isMonthInYear(month: HebrewMonth, year: Int): Boolean = when (month) {
        HebrewMonth.ADAR -> !isLeapYear(year)
        HebrewMonth.ADAR_I, HebrewMonth.ADAR_II -> isLeapYear(year)
        else -> true
    }

    fun daysInMonth(year: Int, month: HebrewMonth): Int {
        require(isMonthInYear(month, year)) { "$month is not in year $year" }
        return when (month) {
            HebrewMonth.IYAR, HebrewMonth.TAMMUZ, HebrewMonth.ELUL, HebrewMonth.TEVET,
            HebrewMonth.ADAR, HebrewMonth.ADAR_II -> 29
            HebrewMonth.CHESHVAN -> if (isCheshvanLong(year)) 30 else 29
            HebrewMonth.KISLEV -> if (isKislevShort(year)) 29 else 30
            else -> 30
        }
    }

    fun monthsOfYear(year: Int): List<HebrewMonth> = buildList {
        add(HebrewMonth.TISHREI); add(HebrewMonth.CHESHVAN); add(HebrewMonth.KISLEV)
        add(HebrewMonth.TEVET); add(HebrewMonth.SHEVAT)
        if (isLeapYear(year)) { add(HebrewMonth.ADAR_I); add(HebrewMonth.ADAR_II) } else add(HebrewMonth.ADAR)
        add(HebrewMonth.NISAN); add(HebrewMonth.IYAR); add(HebrewMonth.SIVAN)
        add(HebrewMonth.TAMMUZ); add(HebrewMonth.AV); add(HebrewMonth.ELUL)
    }

    fun isValid(year: Int, month: HebrewMonth, day: Int): Boolean =
        isHebrewYearSupported(year) && isMonthInYear(month, year) && day in 1..daysInMonth(year, month)

    fun kosherJavaMonth(month: HebrewMonth): Int = when (month) {
        HebrewMonth.NISAN -> 1; HebrewMonth.IYAR -> 2; HebrewMonth.SIVAN -> 3; HebrewMonth.TAMMUZ -> 4
        HebrewMonth.AV -> 5; HebrewMonth.ELUL -> 6; HebrewMonth.TISHREI -> 7; HebrewMonth.CHESHVAN -> 8
        HebrewMonth.KISLEV -> 9; HebrewMonth.TEVET -> 10; HebrewMonth.SHEVAT -> 11
        HebrewMonth.ADAR, HebrewMonth.ADAR_I -> 12; HebrewMonth.ADAR_II -> 13
    }

    fun fromKosherJavaMonth(year: Int, month: Int): HebrewMonth = when (month) {
        1 -> HebrewMonth.NISAN; 2 -> HebrewMonth.IYAR; 3 -> HebrewMonth.SIVAN; 4 -> HebrewMonth.TAMMUZ
        5 -> HebrewMonth.AV; 6 -> HebrewMonth.ELUL; 7 -> HebrewMonth.TISHREI; 8 -> HebrewMonth.CHESHVAN
        9 -> HebrewMonth.KISLEV; 10 -> HebrewMonth.TEVET; 11 -> HebrewMonth.SHEVAT
        12 -> if (isLeapYear(year)) HebrewMonth.ADAR_I else HebrewMonth.ADAR
        13 -> HebrewMonth.ADAR_II
        else -> throw IllegalArgumentException("month $month")
    }

    /** По дневной (гражданской) дате. */
    fun fromGregorian(date: LocalDate): HebrewDate {
        val jd = JewishDate(date)
        return HebrewDate(jd.jewishYear, fromKosherJavaMonth(jd.jewishYear, jd.jewishMonth), jd.jewishDayOfMonth)
    }

    fun fromGregorian(date: LocalDate, afterSunset: Boolean): HebrewDate =
        fromGregorian(if (afterSunset) date.plusDays(1) else date)

    /** Дневная гражданская дата. */
    fun toGregorian(date: HebrewDate): LocalDate {
        require(isValid(date.year, date.month, date.day)) { "Invalid hebrew date $date" }
        return JewishDate(date.year, kosherJavaMonth(date.month), date.day).localDate
    }

    fun hebrewYearOf(date: LocalDate): Int = JewishDate(date).jewishYear

    /** Первый день еврейского месяца как гражданская дата. */
    fun firstDayOf(year: Int, month: HebrewMonth): LocalDate = toGregorian(HebrewDate(year, month, 1))

    /** Следующий месяц еврейского календаря (с переходом года в Тишрей). */
    fun nextMonth(year: Int, month: HebrewMonth): Pair<Int, HebrewMonth> {
        val months = monthsOfYear(year)
        val i = months.indexOf(month)
        if (month == HebrewMonth.ELUL) return (year + 1) to HebrewMonth.TISHREI
        return year to months[i + 1]
    }

    fun previousMonth(year: Int, month: HebrewMonth): Pair<Int, HebrewMonth> {
        if (month == HebrewMonth.TISHREI) return (year - 1) to HebrewMonth.ELUL
        val months = monthsOfYear(year)
        return year to months[months.indexOf(month) - 1]
    }
}
