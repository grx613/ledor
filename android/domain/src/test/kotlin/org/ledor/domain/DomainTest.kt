package org.ledor.domain

import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.*
import org.ledor.domain.lists.ListNameRules
import org.ledor.domain.reminder.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.math.abs

class DomainTest {
    private val today = LocalDate.of(2026, 11, 24)
    private val policy = YahrzeitAdarPolicy.ADAR_I

    private fun event(
        date: EventDate, type: EventType = EventType.BIRTHDAY, observe: CalendarKind = CalendarKind.HEBREW,
        recurring: Boolean = EventDefaults.recurring(type),
    ) = Event("e1", "local", "Сара", type, if (type == EventType.OTHER) "X" else null, recurring, observe, date, null)

    @Test fun controlConversions() {
        assertEquals(HebrewDate(5750, HebrewMonth.NISAN, 28), HebrewCalendarMath.fromGregorian(LocalDate.of(1990, 4, 23)))
        assertEquals(HebrewDate(5750, HebrewMonth.NISAN, 29), HebrewCalendarMath.fromGregorian(LocalDate.of(1990, 4, 23), true))
        assertEquals(HebrewDate(5787, HebrewMonth.KISLEV, 14), HebrewCalendarMath.fromGregorian(today))
    }

    @Test fun nextBirthdayFromSpec() {
        val e = event(EventDate.fromGregorian(GregorianParts(1990, 4, 23), false))
        val occ = OccurrenceCalculator.next(e, today, policy)!!
        assertEquals(LocalDate.of(2027, 5, 5), occ.date)
        assertEquals(37, occ.yearsSince)
        assertEquals(162, ChronoUnit.DAYS.between(today, occ.date))
    }

    @Test fun gregorianObserve() {
        val e = event(EventDate.fromGregorian(GregorianParts(1990, 4, 30), false), observe = CalendarKind.GREGORIAN)
        assertEquals(LocalDate.of(2027, 4, 30), OccurrenceCalculator.next(e, today, policy)!!.date)
        val leap = event(EventDate.fromGregorian(GregorianParts(null, 2, 29), false), observe = CalendarKind.GREGORIAN)
        assertEquals(LocalDate.of(2027, 2, 28), OccurrenceCalculator.next(leap, today, policy)!!.date)
    }

    @Test fun todayIsOccurrence() {
        val e = event(EventDate.fromHebrew(HebrewParts(null, HebrewMonth.KISLEV, 14)))
        assertEquals(today, OccurrenceCalculator.next(e, today, policy)!!.date)
        assertNull(OccurrenceCalculator.next(e, today, policy)!!.yearsSince)
    }

    @Test fun oneOffPastIsNull() {
        val e = event(EventDate.fromGregorian(GregorianParts(2020, 1, 1), false), EventType.BAR_MITZVAH)
        assertNull(OccurrenceCalculator.next(e, today, policy))
    }

    @Test fun birthdayStartsNextYear() {
        val e = event(EventDate.fromGregorian(GregorianParts(2026, 12, 1), false), observe = CalendarKind.GREGORIAN)
        assertEquals(LocalDate.of(2027, 12, 1), OccurrenceCalculator.next(e, today, policy)!!.date)
        val w = event(EventDate.fromGregorian(GregorianParts(2026, 12, 1), false), EventType.WEDDING, CalendarKind.GREGORIAN)
        assertEquals(LocalDate.of(2026, 12, 1), OccurrenceCalculator.next(w, today, policy)!!.date)
        val far = event(EventDate.fromGregorian(GregorianParts(2030, 6, 1), false), EventType.WEDDING, CalendarKind.HEBREW)
        assertEquals(LocalDate.of(2030, 6, 1), OccurrenceCalculator.next(far, today, policy)!!.date)
    }

    @Test fun mitzvah() {
        val birth = HebrewParts.of(HebrewCalendarMath.fromGregorian(LocalDate.of(2015, 6, 2)))
        val hints = MitzvahCalculator.mitzvahHints(birth, today)
        assertEquals(LocalDate.of(2028, 6, 9), hints.first { it.type == EventType.BAR_MITZVAH }.date)
        assertEquals(LocalDate.of(2027, 6, 20), hints.first { it.type == EventType.BAT_MITZVAH }.date)
        val old = HebrewParts.of(HebrewCalendarMath.fromGregorian(LocalDate.of(2013, 3, 10)))
        assertTrue(MitzvahCalculator.mitzvahHints(old, today).isEmpty())
    }

    @Test fun yahrzeitAdarIIPolicy() {
        val r = AnniversaryCalculator.hebrewAnniversary(AnniversaryRule.YAHRZEIT,
            HebrewParts(5785, HebrewMonth.ADAR, 10), 5787, YahrzeitAdarPolicy.ADAR_II)
        assertEquals(HebrewMonth.ADAR_II, r.month)
    }

    @Test fun validation() {
        val ok = event(EventDate.fromGregorian(GregorianParts(1990, 4, 23), false))
        assertTrue(EventValidator.validate(ok).isEmpty())
        assertTrue(ValidationError.TITLE_EMPTY in EventValidator.validate(ok.copy(title = " ")))
        val badHeb = ok.copy(date = EventDate.fromHebrew(HebrewParts(5786, HebrewMonth.CHESHVAN, 30)))
        assertTrue(ValidationError.DATE_INVALID in EventValidator.validate(badHeb))
        val noYear = ok.copy(date = EventDate.fromGregorian(GregorianParts(null, 4, 23), false))
        assertTrue(ValidationError.OBSERVE_NEEDS_YEAR in EventValidator.validate(noYear))
        assertTrue(ValidationError.ONE_OFF_NEEDS_YEAR in EventValidator.validate(noYear.copy(recurring = false, observeBy = CalendarKind.GREGORIAN)))
        assertTrue(ValidationError.CUSTOM_LABEL_REQUIRED in EventValidator.validate(ok.copy(type = EventType.OTHER)))
    }

    @Test fun listNames() {
        assertEquals("семья коэн", ListNameRules.key("  Семья   Коэн "))
        assertFalse(ListNameRules.isValid("ab"))
    }

    @Test fun cities() {
        val c = CityCatalog.loadFromResources()
        assertTrue(c.cities.size > 100)
        assertEquals(c.cities.size, c.cities.map { it.id }.toSet().size)
        c.cities.forEach { ZoneId.of(it.zoneId); assertTrue(it.lat in -90.0..90.0 && it.lon in -180.0..180.0) }
        assertEquals("il-jerusalem", c.search("иерус").first().id)
    }

    private fun assertSunset(cityId: String, date: LocalDate, hh: Int, mm: Int) {
        val city = CityCatalog.loadFromResources().byId(cityId)!!
        val t = CitySunsetProvider(city).sunset(date).atZone(ZoneId.of(city.zoneId)).toLocalTime()
        val diff = abs(Duration.between(LocalTime.of(hh, mm), t).toMinutes())
        assertTrue("$cityId $date: $t", diff <= 6)
    }

    @Test fun sunsets() {
        assertSunset("il-jerusalem", LocalDate.of(2026, 6, 21), 19, 47)
        assertSunset("il-jerusalem", LocalDate.of(2026, 12, 21), 16, 40)
        assertSunset("ru-moscow", LocalDate.of(2026, 6, 21), 21, 17)
        assertSunset("ru-moscow", LocalDate.of(2026, 12, 21), 15, 58)
        assertSunset("us-new-york", LocalDate.of(2026, 6, 21), 20, 30)
        assertSunset("us-new-york", LocalDate.of(2026, 12, 21), 16, 32)
        val polar = City("x", "x", "x", "x", "NO", 78.2, 15.6, "Arctic/Longyearbyen")
        val t = CitySunsetProvider(polar).sunset(LocalDate.of(2026, 6, 21)).atZone(ZoneId.of("Arctic/Longyearbyen")).toLocalTime()
        assertEquals(LocalTime.of(18, 0), t)
    }

    @Test fun plannerDefaults() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val e = event(EventDate.fromHebrew(HebrewParts(null, HebrewMonth.KISLEV, 20)))
        val from = ZonedDateTime.of(today, LocalTime.MIDNIGHT, zone).toInstant()
        val plan = ReminderPlanner.plan(listOf(e), ReminderSettings(), DefaultSunsetProvider(zone), zone, policy,
            from, from.plus(Duration.ofDays(30)))
        val d = HebrewCalendarMath.toGregorian(HebrewDate(5787, HebrewMonth.KISLEV, 20))
        assertEquals(2, plan.size)
        assertEquals(ZonedDateTime.of(d.minusDays(1), LocalTime.of(15, 0), zone).toInstant(), plan[0].triggerAt)
        assertEquals(ReminderKind.EVE, plan[0].kind)
        assertEquals(ZonedDateTime.of(d, LocalTime.NOON, zone).toInstant(), plan[1].triggerAt)
        val off = ReminderPlanner.plan(listOf(e), ReminderSettings(eveEnabled = false), DefaultSunsetProvider(zone), zone,
            policy, plan[0].triggerAt, from.plus(Duration.ofDays(30)))
        assertEquals(listOf(ReminderKind.DAY), off.map { it.kind })
        val fixed = ReminderPlanner.plan(listOf(e), ReminderSettings(eveMode = EveMode.FIXED_TIME, eveFixedTime = LocalTime.of(19, 30), dayEnabled = false),
            DefaultSunsetProvider(zone), zone, policy, from, from.plus(Duration.ofDays(30)))
        assertEquals(ZonedDateTime.of(d.minusDays(1), LocalTime.of(19, 30), zone).toInstant(), fixed.single().triggerAt)
    }

    @Test fun observeBoth() {
        val d = EventDate.fromGregorian(GregorianParts(1990, 4, 23), false)
        val e = event(d).copy(observeBoth = true)
        val next = OccurrenceCalculator.next(e, today, policy)!!
        assertEquals(LocalDate.of(2027, 4, 23), next.date)
        assertEquals(CalendarKind.GREGORIAN, next.calendar)
        // В списке «Ближайшие» — обе даты: и по обычному, и по еврейскому календарю.
        val each = OccurrenceCalculator.nextEach(e, today, policy)
        assertEquals(listOf(LocalDate.of(2027, 4, 23), LocalDate.of(2027, 5, 5)), each.map { it.date })
        assertEquals(listOf(CalendarKind.GREGORIAN, CalendarKind.HEBREW), each.map { it.calendar })
        // По одному календарю — одна дата, та же, что у next().
        val single = e.copy(observeBoth = false)
        assertEquals(listOf(OccurrenceCalculator.next(single, today, policy)), OccurrenceCalculator.nextEach(single, today, policy))
        // Одна из двух дат уже прошла в этом году — в списке она же в следующем году, порядок по дате.
        val later = OccurrenceCalculator.nextEach(e, LocalDate.of(2027, 4, 24), policy)
        assertEquals(listOf(LocalDate.of(2027, 5, 5), LocalDate.of(2028, 4, 23)), later.map { it.date })
        assertEquals(listOf(CalendarKind.HEBREW, CalendarKind.GREGORIAN), later.map { it.calendar })
        val year = OccurrenceCalculator.between(e, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), policy)
        assertEquals(listOf(LocalDate.of(2027, 4, 23), LocalDate.of(2027, 5, 5)), year.map { it.date })
        assertEquals(listOf(CalendarKind.GREGORIAN, CalendarKind.HEBREW), year.map { it.calendar })
        // Совпавшие даты — одно наступление.
        val all = OccurrenceCalculator.between(e, LocalDate.of(1991, 1, 1), LocalDate.of(2150, 12, 31), policy)
        assertEquals(all.size, all.map { it.date }.distinct().size)
        // Без года «по обоим» нельзя.
        val noYear = event(EventDate.fromGregorian(GregorianParts(null, 4, 23), false), observe = CalendarKind.GREGORIAN).copy(observeBoth = true)
        assertTrue(ValidationError.OBSERVE_NEEDS_YEAR in EventValidator.validate(noYear))
        // Напоминания: накануне и в день для каждой из двух дат.
        val zone = ZoneId.of("Europe/Moscow")
        val from = LocalDate.of(2027, 4, 1).atStartOfDay(zone).toInstant()
        val plan = ReminderPlanner.plan(listOf(e), ReminderSettings(), DefaultSunsetProvider(zone), zone, policy, from, from.plus(Duration.ofDays(60)))
        assertEquals(4, plan.size)
        assertEquals(setOf(CalendarKind.GREGORIAN, CalendarKind.HEBREW), plan.map { it.calendar }.toSet())
    }

    @Test fun gematriya() {
        assertEquals("תש״ן", org.ledor.domain.calendar.Gematriya.format(5750))
        assertEquals("תשפ״ז", org.ledor.domain.calendar.Gematriya.format(5787))
        assertEquals("כ״ח", org.ledor.domain.calendar.Gematriya.format(28))
        assertEquals("ט״ו", org.ledor.domain.calendar.Gematriya.format(15))
        assertEquals("ט״ז", org.ledor.domain.calendar.Gematriya.format(16))
        assertEquals("א׳", org.ledor.domain.calendar.Gematriya.format(1))
        assertEquals("י״ד", org.ledor.domain.calendar.Gematriya.format(14))
        assertEquals("ל׳", org.ledor.domain.calendar.Gematriya.format(30))
    }

    @Test fun sunsetHint() {
        fun near(expected: LocalTime, actual: LocalTime, minutes: Long) =
            assertTrue("$actual vs $expected", abs(ChronoUnit.MINUTES.between(expected, actual)) <= minutes)
        val jerusalem = City("j", "j", "Jerusalem", "j", "IL", 31.778, 35.235, "Asia/Jerusalem")
        val d = LocalDate.of(2026, 12, 21)
        val before = SunsetHints.evaluate(d, LocalTime.of(15, 0), jerusalem)!!
        assertEquals(SunsetVerdict.BEFORE, before.verdict)
        near(LocalTime.of(16, 40), before.sunset, 2)
        near(LocalTime.of(17, 18), before.nightfall, 8)
        assertEquals(SunsetVerdict.TWILIGHT, SunsetHints.evaluate(d, LocalTime.of(16, 50), jerusalem)!!.verdict)
        assertEquals(SunsetVerdict.AFTER, SunsetHints.evaluate(d, LocalTime.of(18, 0), jerusalem)!!.verdict)
        assertEquals(SunsetVerdict.BEFORE, SunsetHints.evaluate(d, LocalTime.of(1, 30), jerusalem)!!.verdict)

        // Исторический часовой пояс: в апреле 1990 в Москве летнее время UTC+4, заход около 20:50.
        val moscow = City("m", "m", "Moscow", "m", "RU", 55.7558, 37.6173, "Europe/Moscow")
        val m = SunsetHints.evaluate(LocalDate.of(1990, 4, 23), LocalTime.of(19, 40), moscow)!!
        assertEquals(SunsetVerdict.BEFORE, m.verdict)
        near(LocalTime.of(20, 50), m.sunset, 6)

        // Белые ночи: до 8,5° солнце не опускается — сумерки 72 минуты, после полуночи уже «до захода».
        val spb = City("s", "s", "Saint Petersburg", "s", "RU", 59.9343, 30.3351, "Europe/Moscow")
        val june = LocalDate.of(2026, 6, 21)
        val white = SunsetHints.evaluate(june, LocalTime.of(23, 0), spb)!!
        assertEquals(SunsetVerdict.TWILIGHT, white.verdict)
        assertEquals(72, ChronoUnit.MINUTES.between(white.sunset, white.nightfall))
        assertEquals(SunsetVerdict.BEFORE, SunsetHints.evaluate(june.plusDays(1), LocalTime.of(0, 30), spb)!!.verdict)

        // Полярный день: захода нет.
        val polar = City("x", "x", "x", "x", "NO", 78.2, 15.6, "Arctic/Longyearbyen")
        assertNull(SunsetHints.evaluate(june, LocalTime.of(20, 0), polar))
    }
}
