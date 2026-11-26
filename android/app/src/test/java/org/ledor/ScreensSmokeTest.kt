package org.ledor

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
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

    /**
     * Имена в снимках — на языке интерфейса (снимки идут и на сайт).
     * 0–4 события, 5 — название праздника, 6–8 — общие списки, 9 — бар-мицва, 10 — день рождения в общине.
     */
    private val names: List<String> get() = when (lang.substringBefore('-')) {
        "en" -> listOf("Sarah Cohen", "Grandpa Aaron", "David", "Michael & Leah", "Hanukkah party", "Celebration",
            "Cohen family", "Chabad of New York", "Beth Shalom community", "Yossi Levin", "Rabbi Shlomo")
        "he" -> listOf("שרה כהן", "סבא אהרן", "דוד", "מיכאל ולאה", "מסיבת חנוכה", "חגיגה",
            "משפחת כהן", "חב״ד ניו יורק", "קהילת בית שלום", "יוסי לוין", "הרב שלמה")
        "yi" -> listOf("שׂרה כּהן", "זיידע אַהרן", "דוד", "מיכאל און לאה", "חנוכּה־מסיבה", "שׂימחה",
            "משפּחה כּהן", "חב״ד ניו־יאָרק", "קהילה בית שלום", "יאָסי לעווין", "הרבֿ שלמה")
        "fr" -> listOf("Sarah Cohen", "Grand-père Aaron", "David", "Michel et Léa", "Fête de Hanoukka", "Fête",
            "Famille Cohen", "Habad de New York", "Communauté Beth Shalom", "Yossi Lévine", "Rav Chlomo")
        "es" -> listOf("Sara Cohen", "Abuelo Aarón", "David", "Miguel y Lea", "Fiesta de Janucá", "Fiesta",
            "Familia Cohen", "Jabad de Nueva York", "Comunidad Bet Shalom", "Yosi Levin", "Rabino Shlomó")
        else -> listOf("Сара Коэн", "Дедушка Аарон", "Давид", "Миша и Лея", "Ханукальный вечер", "Праздник",
            "Семья Коэн", "Община Хабад Нью-Йорк", "Община «Бейт Шалом»", "Йоси Левин", "Рав Шломо")
    }

    private suspend fun seedData() {
        val app = ApplicationProvider.getApplicationContext<KalendarApp>()
        val c = app.container
        val n = names
        val ev = listOf(
            // День рождения в ближайшие дни — чтобы строка была на первом экране списка (LazyColumn).
            Event("1", "local", n[0], EventType.BIRTHDAY, null, true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(5750, HebrewMonth.KISLEV, 19)), null),
            Event("2", "fam", n[1], EventType.YAHRZEIT, null, true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(5770, HebrewMonth.KISLEV, 20)), null),
            Event("3", "fam", n[2], EventType.BIRTHDAY, null, true, CalendarKind.GREGORIAN,
                EventDate.fromGregorian(GregorianParts(2015, 6, 2), false), null, observeBoth = true),
            Event("4", "fam", n[3], EventType.WEDDING, null, true, CalendarKind.HEBREW,
                EventDate.fromGregorian(GregorianParts(2012, 12, 6), true), null),
            Event("5", "chabad", n[4], EventType.OTHER, n[5], true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(null, HebrewMonth.KISLEV, 25)), null),
            Event("6", "chabad", n[9], EventType.BAR_MITZVAH, null, false, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(5787, HebrewMonth.TEVET, 2)), null),
            Event("7", "shalom", n[10], EventType.BIRTHDAY, null, true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts(5730, HebrewMonth.KISLEV, 17)), null),
        )
        // Общие списки для витрины: уже «синхронизированы», чтобы тест не ходил в сеть.
        fun cloud(id: String, name: String, role: String, color: Int, order: Int) = org.ledor.data.local.ListEntity(
            id = id, kind = org.ledor.data.local.ListKind.CLOUD, name = name, role = role,
            readPin = "4821", masterPin = if (role == org.ledor.data.local.ListRole.EDITOR) "73920461" else null,
            accessToken = "screenshot", lastSyncAt = System.currentTimeMillis(), colorIndex = color, sortOrder = order,
        )
        c.database.lists().upsert(cloud("fam", n[6], org.ledor.data.local.ListRole.EDITOR, 1, 1))
        c.database.lists().upsert(cloud("chabad", n[7], org.ledor.data.local.ListRole.READER, 2, 2))
        c.database.lists().upsert(cloud("shalom", n[8], org.ledor.data.local.ListRole.READER, 4, 3))
        c.settingsStore.setLastSyncAttempt(System.currentTimeMillis())
        ev.filter { it.listId == "local" }.forEach { c.eventRepository.save(it) }
        c.database.events().upsertAll(ev.filter { it.listId != "local" }.map { it.toEntity(org.ledor.data.local.SyncState.SYNCED, rev = 1) })
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
        // «По обоим календарям» (D-46): две строки — по григорианскому (одна дата) и по еврейскому (обе даты).
        rule.onAllNodesWithText(names[6])[0].performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(names[2]).assertCountEquals(2)
        shot("05b_events_both")
        // В карточке — по блоку на каждую из двух ближайших дат.
        rule.onAllNodesWithText(names[2])[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.observe_gregorian)).assertExists()
        rule.onNodeWithText(s(R.string.observe_hebrew)).assertExists()
        shot("06b_detail_both")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.all_lists)).performClick()
        rule.waitForIdle()
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
        // Подсказка «до или после захода»: время 19:40 и первый город каталога → «после захода».
        rule.onNodeWithText(s(R.string.sunset_hint_open)).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.sunset_hint_set_time)).performClick()
        val picker = org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.TimePickerDialog
        picker.updateTime(19, 40)
        picker.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        rule.onNodeWithText(s(R.string.sunset_hint_set_city)).performClick()
        rule.waitForIdle()
        val city = ApplicationProvider.getApplicationContext<KalendarApp>().container.cityCatalog.cities.first()
        rule.onNodeWithText(when (s(R.string.locale_tag)) { "ru" -> city.nameRu; "he", "yi" -> city.nameHe; else -> city.nameEn }).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.sunset_hint_local)).performScrollTo()
        rule.waitForIdle()
        shot("08b_sunset_hint")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        // Подсказка изменила событие — редактор спросит, сохранять ли.
        if (rule.onAllNodesWithText(s(R.string.dont_save)).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithText(s(R.string.dont_save)).performClick()
            rule.waitForIdle()
        }
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
        // B-27: из личного списка — «Перенести события в облачный список…»: выбор списка или новый.
        rule.onAllNodesWithText(s(R.string.list_personal))[0].performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.move_to_cloud)).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(s(R.string.create_list))[0].assertExists()
        shot("17_move_to_cloud")
        rule.onNodeWithText(s(R.string.cancel)).performClick()
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        // Встроенный список праздников: с городом видно время зажигания свечей, включён и Шабат.
        val container = (rule.activity.application as KalendarApp).container
        kotlinx.coroutines.runBlocking {
            container.settingsStore.setCity("il-jerusalem")
            container.settingsStore.setHolidays(org.ledor.data.prefs.HolidaySettings(shabbat = true))
        }
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.holidays_title)).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.holiday_chanukah)).assertExists()
        rule.onNodeWithText(s(R.string.shabbat)).assertExists()
        shot("15_holidays")
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
        // Первый год после смерти: веха «Шлошим» в ближайших событиях ведёт в карточку йорцайта с блоком «Первый год».
        val death = org.ledor.domain.calendar.HebrewCalendarMath.fromGregorian(java.time.LocalDate.now().minusDays(28))
        kotlinx.coroutines.runBlocking {
            container.eventRepository.save(Event("8", "local", names[1], EventType.YAHRZEIT, null, true, CalendarKind.HEBREW,
                EventDate.fromHebrew(HebrewParts.of(death)), null))
        }
        rule.activity.onBackPressedDispatcher.onBackPressed()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.tab_calendar)).performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText(s(R.string.milestone_shloshim)).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText(s(R.string.milestone_shloshim)).performClick()
        rule.waitForIdle()
        rule.onNodeWithText(s(R.string.first_year_title)).assertExists()
        rule.onNodeWithText(s(R.string.milestone_first_yahrzeit)).assertExists()
        shot("16_first_year")
    }
}

@Config(qualifiers = "ru-w400dp-h860dp-xxhdpi") class ScreensRuTest : ScreensSmokeTest("ru")
@Config(qualifiers = "ru-w400dp-h860dp-night-xxhdpi") class ScreensRuDarkTest : ScreensSmokeTest("ru-dark")
@Config(qualifiers = "en-w400dp-h860dp-xxhdpi") class ScreensEnTest : ScreensSmokeTest("en")
@Config(qualifiers = "iw-ldrtl-w400dp-h860dp-xxhdpi") class ScreensHeTest : ScreensSmokeTest("he")
@Config(qualifiers = "ji-ldrtl-w400dp-h860dp-xxhdpi") class ScreensYiTest : ScreensSmokeTest("yi")
@Config(qualifiers = "fr-w400dp-h860dp-xxhdpi") class ScreensFrTest : ScreensSmokeTest("fr")
@Config(qualifiers = "es-w400dp-h860dp-xxhdpi") class ScreensEsTest : ScreensSmokeTest("es")
