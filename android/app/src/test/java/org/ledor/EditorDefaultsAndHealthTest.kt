package org.ledor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.data.local.LOCAL_LIST_ID
import org.ledor.data.local.ListEntity
import org.ledor.data.local.ListKind
import org.ledor.data.local.ListRole
import org.ledor.data.local.ListState
import org.ledor.reminders.HealthIssue
import org.ledor.reminders.ReminderHealthChecker
import org.ledor.ui.editor.defaultListId
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditorDefaultsAndHealthTest {
    private fun list(id: String, kind: String, role: String, order: Int, state: String = ListState.ACTIVE) =
        ListEntity(id = id, kind = kind, name = id, role = role, state = state, sortOrder = order)

    private val local = list(LOCAL_LIST_ID, ListKind.LOCAL, ListRole.OWNER, 0)

    /** EVT-2, B-30: есть облачный список с правом редактирования — он по умолчанию; нет — личный. */
    @Test fun defaultListForNewEvent() {
        val reader = list("reader", ListKind.CLOUD, ListRole.READER, 1)
        val fam = list("fam", ListKind.CLOUD, ListRole.EDITOR, 2)
        val shul = list("shul", ListKind.CLOUD, ListRole.OWNER, 3)
        val lost = list("lost", ListKind.CLOUD, ListRole.EDITOR, 1, ListState.ACCESS_LOST)

        assertEquals(LOCAL_LIST_ID, defaultListId(listOf(local), null))
        assertEquals(LOCAL_LIST_ID, defaultListId(listOf(local, reader, lost), "reader"))
        assertEquals("fam", defaultListId(listOf(local, reader, fam), null))
        // Последним пользовались личным — всё равно облачный.
        assertEquals("fam", defaultListId(listOf(local, fam, shul), LOCAL_LIST_ID))
        // Несколько облачных — последний использованный, иначе первый по порядку.
        assertEquals("shul", defaultListId(listOf(local, fam, shul), "shul"))
        assertEquals("fam", defaultListId(listOf(local, fam, shul), "gone"))
    }

    /** B-31: встроенный список праздников по умолчанию выключен, напоминания о нём включатся вместе с ним. */
    @Test fun holidaysAreOffByDefault() {
        val app = ApplicationProvider.getApplicationContext<KalendarApp>()
        val holidays = kotlinx.coroutines.runBlocking { app.container.settingsStore.current() }.holidays
        assertEquals(org.ledor.data.prefs.HolidaySettings(enabled = false, notify = true, shabbat = false), holidays)
    }

    /** D-51: «не экономить батарею» — системное окно с одной кнопкой; запасной путь — страница приложения. */
    @Test fun batteryFixAsksSystemDialogFirst() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val intents = ReminderHealthChecker(ctx).fixIntents(HealthIssue.BATTERY_OPTIMIZED)
        assertEquals(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, intents.first().action)
        assertEquals("package:${ctx.packageName}", intents.first().dataString)
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intents.last().action)
        // Без этого разрешения в манифесте система окно не покажет.
        val requested = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        assertTrue(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS in requested.orEmpty())
    }
}
