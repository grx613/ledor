package org.ledor.update

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Откуда установлено приложение: от этого зависит, куда ведёт кнопка «Обновить» (D-56). */
enum class InstallSource { GOOGLE_PLAY, SITE }

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String) : UpdateState
    data object Failed : UpdateState
}

object AppVersion {
    private val valid = Regex("""\d+(\.\d+){1,3}""")
    private val leading = Regex("""^\d+(\.\d+)*""")

    fun isValid(version: String) = valid.matches(version)

    /** «0.6.10» новее «0.6.9»: сравниваются числа, а не строки. Хвост вроде «-debug» у текущей версии отбрасывается. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = parts(latest)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val d = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (d != 0) return d > 0
        }
        return false
    }

    private fun parts(version: String) = leading.find(version)?.value.orEmpty().split('.').mapNotNull { it.toIntOrNull() }

    /** Номер последней версии из файла сайта `version.json`: `{"android":{"version":"0.6.6"}}`. null — файл не тот. */
    fun parseLatest(json: String): String? = runCatching {
        Json.parseToJsonElement(json).jsonObject["android"]?.jsonObject?.get("version")?.jsonPrimitive?.content
    }.getOrNull()?.takeIf(::isValid)
}

/**
 * Проверка обновления по кнопке в «О приложении» (ABT-5, D-56). Сама по себе, без нажатия, в сеть не ходит.
 * Обновляет приложение не оно само: копия из Google Play открывает страницу в Google Play, копия с сайта —
 * загрузку нового APK в браузере. Разрешение на установку пакетов не нужно.
 */
class UpdateChecker(private val context: Context, private val siteUrl: String, client: OkHttpClient? = null) {
    private val http = client ?: OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build()

    suspend fun check(currentVersion: String): UpdateState {
        val latest = latestVersion() ?: return UpdateState.Failed
        return if (AppVersion.isNewer(latest, currentVersion)) UpdateState.Available(latest) else UpdateState.UpToDate
    }

    private suspend fun latestVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$siteUrl/version.json").cacheControl(CacheControl.FORCE_NETWORK).build()
            http.newCall(request).execute().use { resp -> if (resp.isSuccessful) AppVersion.parseLatest(resp.body.string()) else null }
        }.getOrNull()
    }

    val installSource: InstallSource
        get() {
            val pm = context.packageManager
            val installer = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) pm.getInstallSourceInfo(context.packageName).installingPackageName
                else @Suppress("DEPRECATION") pm.getInstallerPackageName(context.packageName)
            }.getOrNull()
            return if (installer == PLAY_STORE) InstallSource.GOOGLE_PLAY else InstallSource.SITE
        }

    /** Что открыть по кнопке «Обновить»: пробовать по порядку, пока одно не откроется. */
    fun updateIntents(version: String, source: InstallSource = installSource): List<Intent> = when (source) {
        InstallSource.GOOGLE_PLAY -> listOf(
            Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri()).setPackage(PLAY_STORE),
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()),
        )
        InstallSource.SITE -> listOf(Intent(Intent.ACTION_VIEW, "$siteUrl/ledor-$version.apk".toUri()))
    }.onEach { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    private companion object { const val PLAY_STORE = "com.android.vending" }
}
