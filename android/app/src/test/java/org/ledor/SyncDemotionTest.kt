package org.ledor

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.data.local.AppDatabase
import org.ledor.data.local.ListEntity
import org.ledor.data.local.ListKind
import org.ledor.data.local.ListRole
import org.ledor.data.remote.SupabaseRpc
import org.ledor.data.repo.SyncEngine
import org.robolectric.RobolectricTestRunner

/** B-40: Мастер-ПИН сменили на другом телефоне — этот стал читателем, забыл старый Мастер-ПИН и сказал об этом. */
@RunWith(RobolectricTestRunner::class)
class SyncDemotionTest {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()
    private val db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    @After fun close() = db.close()

    @Test fun editorBecomesReader() = runBlocking {
        db.lists().upsert(ListEntity(id = "L", kind = ListKind.CLOUD, name = "Община", role = ListRole.EDITOR,
            readPin = "493817", masterPin = "12345678", accessToken = "t"))
        val rpc = SupabaseRpc("https://example.org", "key", OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("")
                .body("""{"ok":true,"list":{"id":"L","name":"Община","revision":3},"role":"READER","read_pin":"0770","events":[]}"""
                    .toResponseBody("application/json".toMediaType())).build()
        }).build())
        val settings = (ctx as KalendarApp).container.settingsStore
        SyncEngine(db, rpc, settings, deletedNotice = { it }, afterSync = {}, demotedNotice = { "demoted:$it" }).syncList("L")

        val l = db.lists().get("L")!!
        assertEquals(ListRole.READER, l.role)
        assertEquals("0770", l.readPin)
        assertNull(l.masterPin)
        assertTrue("demoted:Община" in settings.notices.first())
    }
}
