package org.ledor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.Occurrence
import org.ledor.ui.format.DateFormatters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** ALL-3, D-55: первой идёт главная дата — та, что отмечается и не меняется из года в год. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru")
class OccurrenceDatesTest {
    private val f = DateFormatters(ApplicationProvider.getApplicationContext<Context>())

    /** День рождения 30 апреля 1984 = 28 Нисана 5744, «по обоим календарям»: два наступления в 2027 году. */
    private val gregorian = Occurrence("e", LocalDate.of(2027, 4, 30), HebrewDate(5787, HebrewMonth.NISAN, 23), 43, startsEveBefore = false)
    private val hebrew = Occurrence("e", LocalDate.of(2027, 5, 5), HebrewDate(5787, HebrewMonth.NISAN, 28), 43, startsEveBefore = true)

    @Test fun mainDateIsTheObservedOne() {
        assertEquals(listOf("пт, 30 апреля 2027", "23 Нисана 5787"), f.occurrenceDates(gregorian))
        assertEquals(listOf("28 Нисана 5787", "ср, 5 мая 2027"), f.occurrenceDates(hebrew))
    }

    @Test fun inlineKeepsTheSameOrder() {
        assertEquals("пт, 30 апреля 2027 (23 Нисана 5787)", f.occurrenceDatesInline(gregorian))
        assertEquals("28 Нисана 5787 (ср, 5 мая 2027)", f.occurrenceDatesInline(hebrew))
    }
}
