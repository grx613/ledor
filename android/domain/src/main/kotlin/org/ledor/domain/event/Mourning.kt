package org.ledor.domain.event

import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.calendar.HebrewDate
import java.time.LocalDate

/** Вехи первого года после смерти (B-26). Имена — контракт (iOS). */
enum class MourningMilestone { SHLOSHIM, ELEVEN_MONTHS, TWELVE_MONTHS, FIRST_YAHRZEIT }

data class MilestoneDate(val milestone: MourningMilestone, val date: LocalDate, val hebrewDate: HebrewDate)

/** Правила — docs/03-calendar-domain.md §13. Считается от дня смерти; обычаи общин различаются. */
object MourningCalculator {
    private val M = HebrewCalendarMath

    /** Та же дата через [months] еврейских месяцев; если такого дня в месяце нет — последний день месяца. */
    fun addMonths(d: HebrewDate, months: Int): HebrewDate {
        var year = d.year
        var month = d.month
        repeat(months) { M.nextMonth(year, month).let { (y, m) -> year = y; month = m } }
        return HebrewDate(year, month, minOf(d.day, M.daysInMonth(year, month)))
    }

    /**
     * Вехи по возрастанию даты; последняя — первый йорцайт.
     * Шлошим — 30-й день, считая день смерти первым. 11 и 12 месяцев — еврейские месяцы подряд, поэтому в високосный год
     * 12 месяцев заканчиваются на месяц раньше йорцайта; если даты совпадают, отдельной вехи TWELVE_MONTHS нет.
     */
    fun firstYear(death: HebrewDate, policy: YahrzeitAdarPolicy): List<MilestoneDate> {
        fun of(m: MourningMilestone, h: HebrewDate) = MilestoneDate(m, M.toGregorian(h), h)
        val shloshim = M.toGregorian(death).plusDays(29)
        val yahrzeit = of(MourningMilestone.FIRST_YAHRZEIT,
            AnniversaryCalculator.hebrewAnniversary(AnniversaryRule.YAHRZEIT, HebrewParts.of(death), death.year + 1, policy))
        val twelve = of(MourningMilestone.TWELVE_MONTHS, addMonths(death, 12))
        return listOfNotNull(
            MilestoneDate(MourningMilestone.SHLOSHIM, shloshim, M.fromGregorian(shloshim)),
            of(MourningMilestone.ELEVEN_MONTHS, addMonths(death, 11)),
            twelve.takeIf { it.date != yahrzeit.date },
            yahrzeit,
        )
    }
}
