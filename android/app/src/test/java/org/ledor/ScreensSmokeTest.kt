package org.ledor

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.calendar.HebrewMonth
import org.ledor.domain.event.*
import org.ledor.data.local.toEntity
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Смоук-тест: приложение запускается, экраны открываются; снимки — для визуальной проверки. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensSmokeTest(private val lang: String) {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) = rule.onRoot().captureRoboImage("build/screens/$lang/$name.png")
    private fun s(id: Int) = rule.activity.getString(id)

    private fun seed() {
        val th = kotlin.concurrent.thread { runBlocking { seedData() } }
        while (th.isAlive) { rule.mainClock.advanceTimeBy(50); org.robolectric.shadows.ShadowLooper.idleMainLooper() }
    }

    /** Имена в снимках — на языке интерфейса (снимки идут и на сайт). */
    private val names: List<String> get() = when (lang.substringBefore('-')) {
        "en" -> listOf("Sarah Cohen", "Grandpa Aaron", "David", "Michael & Leah", "Hanukkah party", "Celebration", "Cohen family")
        "he" -> listOf("שרה כהן", "סבא אהרן", "דוד", "מיכאל ולאה", "חנוכה בקהילה", "חגיגה", "משפחת כהן")
        "yi" -> listOf("שׂרה כּהן", "זיידע אַהרן", "דוד", "מיכאל און לאה", "חנוכּה אין שול", "שׂימחה", "משפּחה כּהן")
        "fr" -> listOf("Sarah Cohen", "Grand-père Aaron", "David", "Michel et Léa", "Hanoukka en famille", "Fête", "Famille Cohen")
        "es" -> listOf("Sara Cohen", "Abuelo Aarón", "David", "Miguel y Lea", "Janucá en familia", "Fiesta", "Familia Cohen")
        else -> listOf("Сара Коэн", "Дедушка Аарон", "Давид", "Миша и Лея", "Ханука в общине", "Праздник", "Семья Коэн")
    }

    private suspend fun seedData() {
        val app = ApplicationProvider.getApplicationContext<KalendarApp>()
        val c = app.container
        val n = names
        val ev = listOf(
            Event("1", "local", n[0], EventType.BIRTHDAY, null, true, CalendarKind.HEBREW,
                EventDate.fromGregorian(GregorianParts(1990, 4, 23), false), null),
            Event("2", "fam", n[1], EventType.YAHRZEIT, null, true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(5770, HebrewMonth.KISLEV, 20)), null),
            Event("3", "local", n[2], EventType.BIRTHDAY, null, true, CalendarKind.GREGORIAN,
                EventDate.fromGregorian(GregorianParts(2015, 6, 2), false), null, observeBoth = true),
            Event("4", "fam", n[3], EventType.WEDDING, null, true, CalendarKind.HEBREW,
                EventDate.fromGregorian(GregorianParts(2012, 12, 6), true), null),
            Event("5", "local", n[4], EventType.OTHER, n[5], true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(null, HebrewMonth.KISLEV, 25)), null),
        )
        // Общий список для витрины: уже «синхронизирован», чтобы тест не ходил в сеть.
        c.database.lists().upsert(org.ledor.data.local.ListEntity(
            id = "fam", kind = org.ledor.data.local.ListKind.CLOUD, name = n[6], role = org.ledor.data.local.ListRole.EDITOR,
            readPin = "4821", masterPin = "73920461", accessToken = "screenshot", lastSyncAt = System.currentTimeMillis(),
            colorIndex = 1, sortOrder = 1,
        ))
        c.settingsStore.setLastSyncAttempt(System.currentTimeMillis())
        ev.filter { it.listId == "local" }.forEach { c.eventRepository.save(it) }
        c.database.events().upsertAll(ev.filter { it.listId == "fam" }.map { it.toEntity(org.ledor.data.local.SyncState.SYNCED, rev = 1) })
        c.settingsStore.setOnboardingShown()
        // Витрина: без плашки «напоминания могут не прийти», вид по умолчанию — еврейский.
        c.settingsStore.setHealthWarnings(false)
        c.settingsStore.setGridMode(org.ledor.data.prefs.GridMode.HEBREW)
    }

    @Test fun launchAndScreens() {
        rule.mainClock.advanceTimeBy(300)
        shot("01_splash")
        rule.mainClock.advanceTimeBy(2500)
        rule.waitForIdle()
        // Диалог импорта показывается один раз за процесс (DataStore — синглтон на весь JVM тестов).
        if (rule.onAllNodesWithText(s(R.string.onboarding_title)).fetchSemanticsNodes().isNotEmpty()) {
            shot("02_onboarding")
            rule.onNodeWithText(s(R.string.no)).performClick()
        }
        seed()
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
        shot("03_calendar")
        rule.onNodeWithText(s(R.string.grid_gregorian)).performClick()
        rule.waitForIdle()
        shot("04_calendar_gregorian")
        rule.onNodeWithText(s(R.string.grid_hebrew)).performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(s(R.string.tab_events))[0].performClick()
        rule.waitForIdle()
        shot("05_events")
        rule.onNodeWithText(names[0]).performClick()
        rule.waitForIdle()
        shot("06_detail")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.tab_converter)).performClick()
        rule.waitForIdle()
        shot("07_converter")
        rule.onNodeWithText(s(R.string.conv_create_event)).performClick()
        rule.waitForIdle()
        shot("08_editor")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.tab_more)).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.settings)).performClick()
        rule.waitForIdle()
        shot("09_settings")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.manage_lists)).performClick()
        rule.waitForIdle()
        shot("10_lists")
        rule.onNodeWithText(names[6]).performClick()
        rule.waitForIdle()
        shot("14_list_share")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.create_list)).performClick()
        rule.waitForIdle()
        shot("11_list_create")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.feedback_title)).performClick()
        rule.waitForIdle()
        shot("12_feedback")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.about)).performClick()
        rule.waitForIdle()
        shot("13_about")
    }
}

@Config(qualifiers = "ru-w400dp-h860dp-xxhdpi") class ScreensRuTest : ScreensSmokeTest("ru")
@Config(qualifiers = "ru-w400dp-h860dp-night-xxhdpi") class ScreensRuDarkTest : ScreensSmokeTest("ru-dark")
@Config(qualifiers = "en-w400dp-h860dp-xxhdpi") class ScreensEnTest : ScreensSmokeTest("en")
@Config(qualifiers = "iw-ldrtl-w400dp-h860dp-xxhdpi") class ScreensHeTest : ScreensSmokeTest("he")
@Config(qualifiers = "ji-ldrtl-w400dp-h860dp-xxhdpi") class ScreensYiTest : ScreensSmokeTest("yi")
@Config(qualifiers = "fr-w400dp-h860dp-xxhdpi") class ScreensFrTest : ScreensSmokeTest("fr")
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi") class ScreensEsTest : ScreensSmokeTest("es")
