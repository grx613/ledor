package org.ledor

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.data.local.AppDatabase
import org.ledor.data.local.toDomain
import org.ledor.data.local.toEntity
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewDate
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.EventType
import org.ledor.domain.event.Occurrence
import org.ledor.ui.editor.EditorState
import org.ledor.ui.editor.Repeat
import org.ledor.ui.format.DateFormatters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

/** B-41: «каждый месяц» — база на телефоне, редактор, тексты. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "ru")
class MonthlyAppTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Обновление с базы версии 1 (0.6.9 и раньше): события на месте, у всех «каждый год». Если миграция не совпадёт
     * со схемой, Room бросит исключение при открытии — то есть приложение упало бы у всех при обновлении.
     */
    @Test fun migrationFromVersion1KeepsEvents() = runBlocking {
        ctx.deleteDatabase("kalendar.db")
        val schema = File("schemas/org.ledor.data.local.AppDatabase/1.json").readText()
        val json = kotlinx.serialization.json.Json.parseToJsonElement(schema).let { it as kotlinx.serialization.json.JsonObject }["database"]!!
            as kotlinx.serialization.json.JsonObject
        val str = { e: kotlinx.serialization.json.JsonElement -> (e as kotlinx.serialization.json.JsonPrimitive).content }
        val file = ctx.getDatabasePath("kalendar.db").apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            (json["entities"] as kotlinx.serialization.json.JsonArray).forEach { ent ->
                val o = ent as kotlinx.serialization.json.JsonObject
                val table = str(o["tableName"]!!)
                db.execSQL(str(o["createSql"]!!).replace("\${TABLE_NAME}", table))
                (o["indices"] as? kotlinx.serialization.json.JsonArray)?.forEach { i ->
                    db.execSQL(str((i as kotlinx.serialization.json.JsonObject)["createSql"]!!).replace("\${TABLE_NAME}", table))
                }
            }
            (json["setupQueries"] as kotlinx.serialization.json.JsonArray).forEach { db.execSQL(str(it)) }
            db.execSQL("INSERT INTO lists (id, kind, name, role, state, last_synced_rev, notify_enabled, color_index, sort_order, created_at) VALUES ('local', 'LOCAL', '', 'OWNER', 'ACTIVE', 0, 1, 0, 0, 0)")
            db.execSQL("INSERT INTO events (id, list_id, rev, sync_state, updated_at, title, type, custom_label, recurring, observe_by, input_calendar, g_year, g_month, g_day, after_sunset, h_year, h_month, h_day, note) " +
                "VALUES ('e1', 'local', 0, 'SYNCED', 0, 'Сара', 'BIRTHDAY', NULL, 1, 'HEBREW', 'GREGORIAN', 1990, 4, 23, 0, 5750, 'NISAN', 28, NULL)")
            db.version = 1
        }
        val room = AppDatabase.build(ctx)
        try {
            val events = room.events().getVisible()
            assertEquals(listOf("e1"), events.map { it.id })
            assertFalse(events.single().monthly)
            assertEquals("Сара", events.single().toDomain().title)
            // После миграции ежемесячное событие сохраняется и читается.
            val e = events.single().toDomain().copy(id = "m1", type = EventType.OTHER, customLabel = "Квартплата", monthly = true,
                observeBy = CalendarKind.GREGORIAN)
            room.events().upsert(e.toEntity("SYNCED"))
            assertTrue(room.events().get("m1")!!.toDomain().isMonthly)
        } finally {
            room.close()
            ctx.deleteDatabase("kalendar.db")
        }
    }

    @Test fun editorRepeatMonthly() {
        val s = EditorState(type = EventType.OTHER, customLabel = "Квартплата", title = "Квартира", recurring = true, monthly = true,
            input = CalendarKind.GREGORIAN, gDay = 31, gMonth = 1, gYear = "2027", afterSunset = true, observeBy = CalendarKind.HEBREW, observeBoth = true)
        assertEquals(Repeat.MONTHLY, s.repeat)
        val e = s.toEvent()
        assertTrue(e.isMonthly)
        assertEquals(CalendarKind.GREGORIAN, e.observeBy)
        assertFalse(e.observeBoth)
        assertFalse(e.date.afterSunset)
        // Не «Другое» — каждый месяц не действует.
        val b = s.copy(type = EventType.BIRTHDAY, customLabel = "")
        assertEquals(Repeat.YEARLY, b.repeat)
        assertFalse(b.toEvent().monthly)
        assertEquals(Repeat.ONCE, s.copy(recurring = false).repeat)
    }

    @Test fun monthlyTexts() {
        val f = DateFormatters(ctx)
        val o = Occurrence("m", LocalDate.of(2026, 12, 10), HebrewDate(5787, HebrewMonth.KISLEV, 20), null, startsEveBefore = false, monthly = true)
        assertEquals(listOf("чт, 10 декабря 2026"), f.occurrenceDates(o))
        assertEquals("чт, 10 декабря 2026", f.occurrenceDatesInline(o))
        assertEquals("каждый месяц, 10-го", f.monthlyDay(10))
    }
}
