package org.ledor.ui.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Язык приложения (B-6). Пусто — как в системе.
 * Android 13+: системный выбор языка приложения (LocaleManager, он же «Язык приложения» в настройках телефона).
 * Android 8–12: тег хранится в SharedPreferences, контекст оборачивается в attachBaseContext.
 */
object AppLocale {
    /** Языки интерфейса: тег → самоназвание (не переводится). */
    val SUPPORTED = listOf(
        "en" to "English",
        "he" to "עברית",
        "ru" to "Русский",
        "yi" to "ייִדיש",
        "fr" to "Français",
        "es" to "Español",
    )

    private const val PREFS = "app_locale"
    private const val KEY = "tag"

    fun current(context: Context): String =
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.takeIf { !it.isEmpty }?.get(0)?.let(::normalize).orEmpty()
        } else context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()

    fun set(activity: Activity, tag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
            activity.recreate()
        }
    }

    /** Для Android < 13: контекст с выбранным языком (Activity, Application, уведомления). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base
        val tag = base.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    /** Старые коды Java (iw, ji) → современные (he, yi). */
    private fun normalize(l: Locale): String = when (l.language) { "iw" -> "he"; "ji" -> "yi"; else -> l.language }
}
