package org.ledor.domain

import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.*
import org.ledor.domain.reminder.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

/** B-41: «каждый месяц» — в тот же день каждого месяца; если такого дня нет — в последний день месяца. */
class MonthlyTest {
    private val today = LocalDate.of(2026, 11, 27)
    private val policy = YahrzeitAdarPolicy.ADAR_I

    private fun monthly(year: Int?, month: Int, day: Int, type: EventType = EventType.OTHER) = Event(
        "m1", "local", "Квартира", type, if (type == EventType.OTHER) "Квартплата" else null, true,
        CalendarKind.GREGORIAN, EventDate.fromGregorian(GregorianParts(year, month, day), false), null, monthly = true,
    )

    @Test fun monthlyDateClampsToMonthLength() {
        assertEquals(LocalDate.of(2027, 2, 28), MonthlyRule.date(31, YearMonth.of(2027, 2)))
        assertEquals(LocalDate.of(2028, 2, 29), MonthlyRule.date(31, YearMonth.of(2028, 2)))
        assertEquals(LocalDate.of(2027, 4, 30), MonthlyRule.date(31, YearMonth.of(2027, 4)))
        assertEquals(LocalDate.of(2027, 3, 31), MonthlyRule.date(31, YearMonth.of(2027, 3)))
        assertEquals(LocalDate.of(2027, 2, 10), MonthlyRule.date(10, YearMonth.of(2027, 2)))
    }

    @Test fun nextIsThisMonthOrTheNext() {
        val e = monthly(2025, 1, 10)
        val next = OccurrenceCalculator.next(e, today, policy)!!
        assertEquals(LocalDate.of(2026, 12, 10), next.date)
        assertEquals(CalendarKind.GREGORIAN, next.calendar)
        assertTrue(next.monthly)
        assertNull(next.yearsSince)
        assertFalse(next.startsEveBefore)
        // Сегодня — день платежа: ближайшее — сегодня.
        assertEquals(LocalDate.of(2026, 12, 10), OccurrenceCalculator.next(e, LocalDate.of(2026, 12, 10), policy)!!.date)
        assertEquals(LocalDate.of(2027, 1, 10), OccurrenceCalculator.next(e, LocalDate.of(2026, 12, 11), policy)!!.date)
        // Одна дата, как у события по одному календарю.
        assertEquals(listOf(next), OccurrenceCalculator.nextEach(e, today, policy))
    }

    @Test fun day31InShortMonths() {
        val e = monthly(2026, 1, 31)
        val dates = OccurrenceCalculator.between(e, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), policy).map { it.date }
        assertEquals(12, dates.size)
        assertEquals(LocalDate.of(2027, 2, 28), dates[1])
        assertEquals(LocalDate.of(2027, 4, 30), dates[3])
        assertEquals(LocalDate.of(2027, 12, 31), dates[11])
        assertEquals(LocalDate.of(2027, 2, 28), OccurrenceCalculator.next(e, LocalDate.of(2027, 2, 1), policy)!!.date)
        assertEquals(LocalDate.of(2027, 3, 31), OccurrenceCalculator.next(e, LocalDate.of(2027, 3, 1), policy)!!.date)
    }

    @Test fun startsFromTheFirstDate() {
        // Первый раз — 15 февраля 2027: раньше наступлений нет.
        val e = monthly(2027, 2, 15)
        assertEquals(LocalDate.of(2027, 2, 15), OccurrenceCalculator.next(e, today, policy)!!.date)
        val dates = OccurrenceCalculator.between(e, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 30), policy).map { it.date }
        assertEquals(listOf(LocalDate.of(2027, 2, 15), LocalDate.of(2027, 3, 15), LocalDate.of(2027, 4, 15)), dates)
    }

    @Test fun withoutYearEveryMonth() {
        val e = monthly(null, 6, 5)
        assertEquals(LocalDate.of(2026, 12, 5), OccurrenceCalculator.next(e, today, policy)!!.date)
        val dates = OccurrenceCalculator.between(e, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), policy).map { it.date }
        assertEquals(listOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 2, 5), LocalDate.of(2026, 3, 5)), dates)
    }

    @Test fun monthlyOnlyForOtherRecurringGregorian() {
        assertTrue(monthly(2026, 1, 10).isMonthly)
        // Не «Другое», одноразовое или без григорианской даты — ежегодное/одноразовое, как раньше.
        val birthday = monthly(2026, 1, 10, EventType.BIRTHDAY)
        assertFalse(birthday.isMonthly)
        assertEquals(LocalDate.of(2027, 1, 10), OccurrenceCalculator.next(birthday, today, policy)!!.date)
        val oneOff = monthly(2027, 1, 10).copy(recurring = false)
        assertFalse(oneOff.isMonthly)
        assertEquals(LocalDate.of(2027, 1, 10), OccurrenceCalculator.next(oneOff, today, policy)!!.date)
        assertNull(OccurrenceCalculator.next(oneOff, LocalDate.of(2027, 1, 11), policy))
    }

    @Test fun remindersEveryMonth() {
        val zone = ZoneId.of("Europe/Moscow")
        val e = monthly(2025, 1, 10)
        val from = today.atStartOfDay(zone).toInstant()
        val plan = ReminderPlanner.plan(listOf(e), ReminderSettings(), DefaultSunsetProvider(zone), zone, policy, from, from.plus(Duration.ofDays(80)))
        // Декабрь, январь, февраль — накануне и в день.
        assertEquals(6, plan.size)
        assertEquals(listOf(LocalDate.of(2026, 12, 10), LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 10)),
            plan.map { it.occurrenceDate }.distinct())
        assertTrue(plan.all { it.calendar == CalendarKind.GREGORIAN && it.yearsSince == null })
    }
}
