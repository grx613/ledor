package org.ledor.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.MourningCalculator
import org.ledor.domain.event.MourningMilestone
import org.ledor.domain.event.YahrzeitAdarPolicy
import org.ledor.domain.holiday.CandleLightingCalculator
import org.ledor.domain.holiday.Holiday
import org.ledor.domain.holiday.Holidays
import org.ledor.domain.reminder.CityCatalog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.abs

/** Праздники и зажигание свечей — на эталонных векторах Hebcal (spec/golden/holidays.json, candles.json). */
class HolidaysTest {
    private fun rows(name: String): List<JsonObject> {
        val text = javaClass.getResourceAsStream("/$name")!!.bufferedReader().use { it.readText() }
        return Json.parseToJsonElement(text).jsonObject["rows"]!!.jsonArray.map { it.jsonObject }
    }
    private fun JsonObject.s(k: String) = this[k]!!.jsonPrimitive.content

    @Test
    fun holidaysMatchHebcal() {
        val rows = rows("holidays.json")
        assertTrue(rows.size > 600)
        val computed = HashMap<Pair<Int, Boolean>, Map<Holiday, Pair<LocalDate, Int>>>()
        for (r in rows) {
            val year = r["year"]!!.jsonPrimitive.int
            val il = r["il"]!!.jsonPrimitive.boolean
            val all = computed.getOrPut(year to il) { Holidays.inHebrewYear(year, il).associate { it.holiday to (it.start to it.days) } }
            val h = Holiday.valueOf(r.s("holiday"))
            assertEquals("$year il=$il $h", LocalDate.parse(r.s("start")) to r["days"]!!.jsonPrimitive.int, all[h])
        }
        // На рассвете начинаются только четыре малых поста; Йом Кипур и Девятое Ава — накануне вечером.
        assertEquals(setOf(Holiday.TZOM_GEDALIAH, Holiday.ASARA_BETEVET, Holiday.TAANIT_ESTHER, Holiday.TZOM_TAMMUZ),
            Holiday.entries.filter { it.dawn }.toSet())
        // Каждый праздник из enum есть в векторах.
        assertEquals(Holiday.entries.toSet(), rows.map { Holiday.valueOf(it.s("holiday")) }.toSet())
    }

    @Test
    fun betweenReturnsIntersecting() {
        val list = Holidays.between(LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 20), inIsrael = false)
        assertEquals(listOf(Holiday.CHANUKAH, Holiday.ASARA_BETEVET), list.map { it.holiday })
        assertEquals(LocalDate.of(2026, 12, 12), list[0].end)
        assertEquals(listOf(LocalDate.of(2026, 11, 28), LocalDate.of(2026, 12, 5)),
            Holidays.shabbats(LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 5)))
    }

    @Test
    fun yizkorDays() {
        fun day(year: Int, il: Boolean, h: Holiday) = Holidays.inHebrewYear(year, il).first { it.holiday == h }.let { Holidays.yizkorDay(it) }
        // 5787: Шавуот — 11 июня 2027 (пт); в диаспоре Изкор во второй день.
        assertEquals(LocalDate.of(2027, 6, 11), day(5787, true, Holiday.SHAVUOT))
        assertEquals(LocalDate.of(2027, 6, 12), day(5787, false, Holiday.SHAVUOT))
        // Последний день Песаха: 21 Нисана в Израиле, 22 Нисана в диаспоре.
        assertEquals(LocalDate.of(2027, 4, 28), day(5787, true, Holiday.PESACH_VII))
        assertEquals(LocalDate.of(2027, 4, 29), day(5787, false, Holiday.PESACH_VII))
        assertEquals(LocalDate.of(2026, 9, 21), day(5787, false, Holiday.YOM_KIPPUR))
        assertEquals(LocalDate.of(2026, 10, 3), day(5787, false, Holiday.SHMINI_ATZERET))
        assertEquals(4, Holidays.inHebrewYear(5787, false).count { Holidays.yizkorDay(it) != null })
    }

    @Test
    fun mourningFirstYear() {
        val p = YahrzeitAdarPolicy.ADAR_I
        // 5787 — високосный: смерть 10 Нисана 5786 → 11 месяцев — 10 Адара I, 12 месяцев — 10 Адара II, йорцайт — 10 Нисана.
        val a = MourningCalculator.firstYear(HebrewDate(5786, HebrewMonth.NISAN, 10), p)
        assertEquals(listOf(MourningMilestone.SHLOSHIM, MourningMilestone.ELEVEN_MONTHS, MourningMilestone.TWELVE_MONTHS, MourningMilestone.FIRST_YAHRZEIT),
            a.map { it.milestone })
        assertEquals(HebrewDate(5787, HebrewMonth.ADAR_I, 10), a[1].hebrewDate)
        assertEquals(HebrewDate(5787, HebrewMonth.ADAR_II, 10), a[2].hebrewDate)
        assertEquals(HebrewDate(5787, HebrewMonth.NISAN, 10), a[3].hebrewDate)
        assertEquals(a.map { it.date }.sorted(), a.map { it.date })
        // Обычный год: 12 месяцев совпадают с йорцайтом — отдельной вехи нет.
        val b = MourningCalculator.firstYear(HebrewDate(5785, HebrewMonth.TISHREI, 10), p)
        assertEquals(listOf(MourningMilestone.SHLOSHIM, MourningMilestone.ELEVEN_MONTHS, MourningMilestone.FIRST_YAHRZEIT), b.map { it.milestone })
        assertEquals(HebrewDate(5785, HebrewMonth.ELUL, 10), b[1].hebrewDate)
        assertEquals(HebrewDate(5786, HebrewMonth.TISHREI, 10), b[2].hebrewDate)
        // Шлошим — 30-й день: смерть 1 Тишрея 5787 (12.09.2026) → 30 Тишрея (11.10.2026).
        val c = MourningCalculator.firstYear(HebrewDate(5787, HebrewMonth.TISHREI, 1), p)[0]
        assertEquals(LocalDate.of(2026, 10, 11), c.date)
        assertEquals(HebrewDate(5787, HebrewMonth.TISHREI, 30), c.hebrewDate)
        // 30-е число, которого нет в следующем месяце, — последний день месяца.
        assertEquals(HebrewDate(5786, HebrewMonth.ADAR, 29), MourningCalculator.addMonths(HebrewDate(5786, HebrewMonth.SHEVAT, 30), 1))
    }

    @Test
    fun candleLightingMatchesHebcal() {
        val catalog = CityCatalog.loadFromResources()
        val rows = rows("candles.json")
        assertTrue(rows.size > 300)
        var saturdayNights = 0
        for (r in rows) {
            val city = catalog.byId(r.s("city"))!!
            val eve = LocalDate.parse(r.s("eve"))
            assertEquals(r["minutes"]!!.jsonPrimitive.int, CandleLightingCalculator.minutesBeforeSunset(city))
            val c = CandleLightingCalculator.forDay(eve.plusDays(1), city)!!
            assertEquals(eve.dayOfWeek == DayOfWeek.SATURDAY, c.afterNightfall)
            if (c.afterNightfall) saturdayNights++
            val got = c.time.atZone(ZoneId.of(city.zoneId)).toLocalTime()
            val want = LocalTime.parse(r.s("time"))
            assertTrue("${city.id} $eve: $got, Hebcal $want", abs(got.toSecondOfDay() - want.toSecondOfDay()) <= 120)
        }
        assertTrue(saturdayNights > 0)
    }
}
