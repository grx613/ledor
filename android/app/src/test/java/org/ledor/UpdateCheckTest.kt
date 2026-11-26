package org.ledor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.ui.about.SITE_URL
import org.ledor.ui.about.UpdateBlock
import org.ledor.ui.theme.KalendarTheme
import org.ledor.update.AppVersion
import org.ledor.update.InstallSource
import org.ledor.update.UpdateChecker
import org.ledor.update.UpdateState
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

/** ABT-5, D-56: проверка обновления по кнопке и куда ведёт «Обновить». */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ru-w400dp-h300dp-xxhdpi")
class UpdateCheckTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    /** Сайт подменён: [body] = null — нет связи. Запоминает запрошенный адрес. */
    private var requested: String? = null
    private fun checker(body: String?, code: Int = 200) = UpdateChecker(ctx, SITE_URL, OkHttpClient.Builder().addInterceptor(Interceptor { chain ->
        requested = chain.request().url.toString()
        if (body == null) throw IOException("нет сети")
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }).build())

    @Test fun versionsAreComparedAsNumbers() {
        assertTrue(AppVersion.isNewer("0.6.6", "0.6.5"))
        assertTrue(AppVersion.isNewer("0.6.10", "0.6.9"))
        assertTrue(AppVersion.isNewer("1.0", "0.9.9"))
        assertTrue(AppVersion.isNewer("0.6.6", "0.6.5-debug"))
        assertFalse(AppVersion.isNewer("0.6.5", "0.6.5"))
        assertFalse(AppVersion.isNewer("0.6.5", "0.6.5.0"))
        assertFalse(AppVersion.isNewer("0.6.4", "0.6.5"))
    }

    @Test fun versionFileIsParsedStrictly() {
        assertEquals("0.6.6", AppVersion.parseLatest("""{"android":{"version":"0.6.6","apk":"ledor-0.6.6.apk"}}"""))
        assertNull(AppVersion.parseLatest("""{"android":{"version":"../evil"}}"""))
        assertNull(AppVersion.parseLatest("""{"ios":{"version":"1.0"}}"""))
        assertNull(AppVersion.parseLatest("<html>502</html>"))
    }

    @Test fun checkTellsNewerSameOrFailed() = runBlocking {
        val newer = checker("""{"android":{"version":"0.6.6"}}""")
        assertEquals(UpdateState.Available("0.6.6"), newer.check("0.6.5"))
        assertEquals("$SITE_URL/version.json", requested)
        assertEquals(UpdateState.UpToDate, newer.check("0.6.6"))
        assertEquals(UpdateState.UpToDate, newer.check("0.7.0"))
        assertEquals(UpdateState.Failed, checker(null).check("0.6.5"))
        assertEquals(UpdateState.Failed, checker("oops", 502).check("0.6.5"))
    }

    /** Копия из Google Play обновляется только через Google Play; копия с сайта — скачивает APK с сайта. */
    @Test fun updateGoesWhereTheAppCameFrom() {
        val c = checker(null)
        assertEquals(InstallSource.SITE, c.installSource)
        assertEquals(listOf("$SITE_URL/ledor-0.6.6.apk"), c.updateIntents("0.6.6").map { it.dataString })

        shadowOf(ctx.packageManager).setInstallSourceInfo(ctx.packageName, "com.android.vending", "com.android.vending")
        assertEquals(InstallSource.GOOGLE_PLAY, c.installSource)
        val play = c.updateIntents("0.6.6")
        assertEquals("market://details?id=${ctx.packageName}", play.first().dataString)
        assertEquals("com.android.vending", play.first().`package`)
        assertEquals("https://play.google.com/store/apps/details?id=${ctx.packageName}", play.last().dataString)
        assertTrue(play.none { it.dataString.orEmpty().contains(".apk") })
    }

    /** Приложение само пакеты не ставит: этого разрешения в манифесте быть не должно (правила Google Play). */
    @Test fun appDoesNotInstallPackagesItself() {
        val requested = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        assertFalse(Manifest.permission.REQUEST_INSTALL_PACKAGES in requested.orEmpty())
    }

    @Test fun availableUpdateIsShown() {
        rule.setContent { KalendarTheme { Surface { UpdateBlock(checker(null), UpdateState.Available("0.6.6")) } } }
        rule.onNodeWithTag("update_button").assertExists()
        rule.onRoot().captureRoboImage("build/screens/ru/13b_update_available.png")
    }
}
