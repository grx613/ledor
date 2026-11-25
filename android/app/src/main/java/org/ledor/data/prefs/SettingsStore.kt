package org.ledor.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import org.ledor.domain.event.YahrzeitAdarPolicy
import org.ledor.domain.reminder.EveMode
import org.ledor.domain.reminder.ReminderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalTime

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class GridMode { GREGORIAN, HEBREW }

/** Встроенный список «Еврейские праздники» (B-25). */
data class HolidaySettings(
    /** Показывать праздники в календаре и в событиях. */
    val enabled: Boolean = true,
    /** Напоминать накануне вечером. */
    val notify: Boolean = true,
    /** Шабат: напоминание в пятницу со временем зажигания свечей. */
    val shabbat: Boolean = false,
)

data class AppSettings(
    val onboardingImportShown: Boolean = false,
    val reminders: ReminderSettings = ReminderSettings(),
    val cityId: String? = null,
    val healthWarningsEnabled: Boolean = true,
    val weekStartsOnSunday: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val gridMode: GridMode = GridMode.HEBREW,
    val adarPolicy: YahrzeitAdarPolicy = YahrzeitAdarPolicy.ADAR_I,
    val lastListId: String? = null,
    val holidays: HolidaySettings = HolidaySettings(),
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object K {
        val onboarding = booleanPreferencesKey("onboardingImportShown")
        val eveEnabled = booleanPreferencesKey("eveEnabled")
        val eveMode = stringPreferencesKey("eveMode")
        val eveMinutes = intPreferencesKey("eveMinutesBeforeSunset")
        val eveFixed = intPreferencesKey("eveFixedTimeMinutes")
        val dayEnabled = booleanPreferencesKey("dayEnabled")
        val dayTime = intPreferencesKey("dayTimeMinutes")
        val cityId = stringPreferencesKey("cityId")
        val health = booleanPreferencesKey("healthWarningsEnabled")
        val watermark = longPreferencesKey("reminderWatermark")
        val weekSunday = booleanPreferencesKey("weekStartsOnSunday")
        val theme = stringPreferencesKey("themeMode")
        val grid = stringPreferencesKey("calendarGridMode")
        val adar = stringPreferencesKey("yahrzeitAdarPolicy")
        val lastSync = longPreferencesKey("lastSyncAttemptAt")
        val notices = stringPreferencesKey("pendingNotices")
        val lastList = stringPreferencesKey("lastListId")
        val holidays = booleanPreferencesKey("holidaysEnabled")
        val holidaysNotify = booleanPreferencesKey("holidaysNotify")
        val shabbat = booleanPreferencesKey("shabbatCandles")
    }

    private val store get() = context.dataStore

    private fun LocalTime.toMinutes() = hour * 60 + minute
    private fun Int.toTime() = LocalTime.of(this / 60, this % 60)

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = ReminderSettings()
        AppSettings(
            onboardingImportShown = p[K.onboarding] ?: false,
            reminders = ReminderSettings(
                eveEnabled = p[K.eveEnabled] ?: d.eveEnabled,
                eveMode = p[K.eveMode]?.let { runCatching { EveMode.valueOf(it) }.getOrNull() } ?: d.eveMode,
                eveMinutesBeforeSunset = p[K.eveMinutes] ?: d.eveMinutesBeforeSunset,
                eveFixedTime = p[K.eveFixed]?.toTime() ?: d.eveFixedTime,
                dayEnabled = p[K.dayEnabled] ?: d.dayEnabled,
                dayTime = p[K.dayTime]?.toTime() ?: d.dayTime,
            ),
            cityId = p[K.cityId],
            healthWarningsEnabled = p[K.health] ?: true,
            weekStartsOnSunday = p[K.weekSunday] ?: true,
            themeMode = p[K.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            gridMode = p[K.grid]?.let { runCatching { GridMode.valueOf(it) }.getOrNull() } ?: GridMode.HEBREW,
            adarPolicy = p[K.adar]?.let { runCatching { YahrzeitAdarPolicy.valueOf(it) }.getOrNull() } ?: YahrzeitAdarPolicy.ADAR_I,
            lastListId = p[K.lastList],
            holidays = HolidaySettings(p[K.holidays] ?: true, p[K.holidaysNotify] ?: true, p[K.shabbat] ?: false),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setOnboardingShown() = store.edit { it[K.onboarding] = true }
    suspend fun setReminders(r: ReminderSettings) = store.edit {
        it[K.eveEnabled] = r.eveEnabled; it[K.eveMode] = r.eveMode.name
        it[K.eveMinutes] = r.eveMinutesBeforeSunset; it[K.eveFixed] = r.eveFixedTime.toMinutes()
        it[K.dayEnabled] = r.dayEnabled; it[K.dayTime] = r.dayTime.toMinutes()
    }
    suspend fun setCity(id: String?) = store.edit { if (id == null) it.remove(K.cityId) else it[K.cityId] = id }
    suspend fun setHealthWarnings(v: Boolean) = store.edit { it[K.health] = v }
    suspend fun setWeekStartsOnSunday(v: Boolean) = store.edit { it[K.weekSunday] = v }
    suspend fun setTheme(v: ThemeMode) = store.edit { it[K.theme] = v.name }
    suspend fun setGridMode(v: GridMode) = store.edit { it[K.grid] = v.name }
    suspend fun setAdarPolicy(v: YahrzeitAdarPolicy) = store.edit { it[K.adar] = v.name }
    suspend fun setLastListId(v: String) = store.edit { it[K.lastList] = v }
    suspend fun setHolidays(v: HolidaySettings) = store.edit {
        it[K.holidays] = v.enabled; it[K.holidaysNotify] = v.notify; it[K.shabbat] = v.shabbat
    }

    suspend fun watermark(): Long? = store.data.first()[K.watermark]
    suspend fun setWatermark(v: Long) = store.edit { it[K.watermark] = v }

    suspend fun lastSyncAttempt(): Long = store.data.first()[K.lastSync] ?: 0
    suspend fun setLastSyncAttempt(v: Long) = store.edit { it[K.lastSync] = v }

    val notices: Flow<List<String>> = store.data.map { p ->
        p[K.notices]?.let { runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }
    suspend fun pushNotice(text: String) = store.edit {
        val cur = it[K.notices]?.let { s -> runCatching { Json.decodeFromString<List<String>>(s) }.getOrNull() } ?: emptyList()
        it[K.notices] = Json.encodeToString(cur + text)
    }
    suspend fun clearNotices() = store.edit { it.remove(K.notices) }
}
