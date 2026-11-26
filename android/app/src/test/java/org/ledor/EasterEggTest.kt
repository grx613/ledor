package org.ledor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.ledor.ui.about.EGG_STEPS
import org.ledor.ui.about.EggTaps
import org.ledor.ui.about.YetzerGame
import org.ledor.ui.theme.KalendarTheme
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.random.Random

/** ABT-6, D-57: пасхалка в «О программе». */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "ru-w400dp-h860dp-xxhdpi")
class EasterEggTest {
    @get:Rule val rule = createComposeRule()

    @Test fun sevenTapsInARowOpenIt() {
        val taps = EggTaps()
        assertEquals(listOf(6, 5, 4, 3, 2, 1, 0), (0 until 7).map { taps.tap(1_000L + it * 300) })
        // После открытия счёт начинается заново; пауза дольше двух секунд тоже его сбрасывает.
        assertEquals(6, taps.tap(4_000))
        assertEquals(5, taps.tap(4_500))
        assertEquals(6, taps.tap(7_000))
    }

    @Test fun yetzerIsBeatenWithSevenHits() {
        var closed = false
        rule.setContent { KalendarTheme { YetzerGame(onClose = { closed = true }, random = Random(7)) } }
        val excuses = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().resources.getStringArray(R.array.egg_excuses)
        assertEquals(EGG_STEPS, excuses.size)
        rule.onNodeWithText(excuses[0]).assertExists()
        repeat(3) { rule.onNodeWithTag("yetzer").performClick(); rule.waitForIdle() }
        rule.onNodeWithText(excuses[3]).assertExists()
        rule.onNodeWithText("3 / 7").assertExists()
        rule.onRoot().captureRoboImage("build/screens/ru/13c_egg.png")
        repeat(4) { rule.onNodeWithTag("yetzer").performClick(); rule.waitForIdle() }
        rule.onNodeWithTag("yetzer").assertDoesNotExist()
        rule.onNodeWithTag("egg_quote").assertExists()
        rule.onRoot().captureRoboImage("build/screens/ru/13d_egg_won.png")
        rule.onNodeWithTag("egg_done").performClick()
        assertTrue(closed)
    }

    @Config(qualifiers = "ru-w400dp-h860dp-night-xxhdpi")
    @Test fun yetzerIsVisibleOnDarkTheme() {
        rule.setContent { KalendarTheme { YetzerGame(onClose = {}, random = Random(7)) } }
        rule.onNodeWithTag("yetzer").assertExists()
        rule.onRoot().captureRoboImage("build/screens/ru-dark/13c_egg.png")
    }
}
