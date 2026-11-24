package org.ledor.domain

import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.AnniversaryCalculator
import org.ledor.domain.event.AnniversaryRule
import org.ledor.domain.event.HebrewParts
import org.ledor.domain.event.YahrzeitAdarPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GoldenVectorsTest {

    private fun rows(file: String): List<JsonArray> {
        val text = javaClass.getResourceAsStream("/$file")!!.bufferedReader().use { it.readText() }
        return Json.parseToJsonElement(text).jsonObject["rows"]!!.jsonArray.map { it.jsonArray }
    }

    private fun JsonArray.s(i: Int) = this[i].jsonPrimitive.content
    private fun JsonArray.i(i: Int) = this[i].jsonPrimitive.int

    @Test
    fun conversions() {
        val rows = rows("conversions.json")
        assertEquals(1040, rows.size)
        rows.forEachIndexed { n, r ->
            val g = LocalDate.parse(r.s(0))
            val month = HebrewMonth.valueOf(r.s(2))
            val h = HebrewCalendarMath.fromGregorian(g)
            assertEquals("row $n $r", r.i(1), h.year)
            assertEquals("row $n $r", month, h.month)
            assertEquals("row $n $r", r.i(3), h.day)
            assertEquals("row $n $r", r.i(4), HebrewCalendarMath.kosherJavaMonth(month))
            assertEquals("row $n $r", g, HebrewCalendarMath.toGregorian(h))
        }
    }

    @Test
    fun years() {
        val rows = rows("years.json")
        rows.forEachIndexed { n, r ->
            val y = r.i(0)
            assertEquals("row $n", r[1].jsonPrimitive.boolean, HebrewCalendarMath.isLeapYear(y))
            assertEquals("row $n", r.i(2), HebrewCalendarMath.daysInYear(y))
            assertEquals("row $n", r.i(3), HebrewCalendarMath.daysInMonth(y, HebrewMonth.CHESHVAN))
            assertEquals("row $n", r.i(4), HebrewCalendarMath.daysInMonth(y, HebrewMonth.KISLEV))
            assertEquals("row $n", LocalDate.parse(r.s(5)), HebrewCalendarMath.firstDayOf(y, HebrewMonth.TISHREI))
            assertEquals(if (r[1].jsonPrimitive.boolean) 13 else 12, HebrewCalendarMath.monthsOfYear(y).size)
        }
    }

    @Test
    fun anniversaries() {
        val rows = rows("anniversaries.json")
        assertEquals(3724, rows.size)
        rows.forEachIndexed { n, r ->
            val rule = AnniversaryRule.valueOf(r.s(0))
            val origin = HebrewParts(r.i(1), HebrewMonth.valueOf(r.s(2)), r.i(3))
            val res = AnniversaryCalculator.hebrewAnniversary(rule, origin, r.i(4), YahrzeitAdarPolicy.ADAR_I)
            assertEquals("row $n $r", HebrewMonth.valueOf(r.s(5)), res.month)
            assertEquals("row $n $r", r.i(6), res.day)
            assertEquals("row $n $r", LocalDate.parse(r.s(7)), HebrewCalendarMath.toGregorian(res))
        }
    }
}
