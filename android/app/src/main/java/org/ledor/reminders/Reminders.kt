package org.ledor.reminders

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.ledor.KalendarApp
import org.ledor.MainActivity
import org.ledor.R
import org.ledor.data.local.AppDatabase
import org.ledor.data.local.toDomain
import org.ledor.data.holiday.HOLIDAYS_LIST_ID
import org.ledor.data.holiday.HolidayEvents
import org.ledor.data.holiday.MilestoneEvents
import org.ledor.data.prefs.SettingsStore
import org.ledor.domain.calendar.CalendarKind
import org.ledor.domain.event.Event
import org.ledor.domain.event.EventType
import org.ledor.domain.calendar.HebrewCalendarMath
import org.ledor.domain.reminder.CityCatalog
import org.ledor.domain.reminder.CitySunsetProvider
import org.ledor.domain.reminder.DefaultSunsetProvider
import org.ledor.domain.reminder.PlannedReminder
import org.ledor.domain.reminder.ReminderKind
import org.ledor.domain.reminder.ReminderPlanner
import org.ledor.domain.reminder.SunsetProvider
import org.ledor.ui.format.DateFormatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface AlarmGateway {
    fun canScheduleExact(): Boolean
    fun set(triggerAtMillis: Long, exact: Boolean)
    fun cancel()
}

class AndroidAlarmGateway(private val context: Context) : AlarmGateway {
    private val am = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context, 1001, Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()

    override fun set(triggerAtMillis: Long, exact: Boolean) {
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending())
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending())
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending())
        }
    }

    override fun cancel() = am.cancel(pending())
}

/** «Цепочка из одного будильника» (docs/05-reminders-battery.md §2). */
class ReminderScheduler(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val cities: () -> CityCatalog,
    private val alarms: AlarmGateway,
    private val notifier: NotificationFactory,
    private val holidays: HolidayEvents,
    private val milestones: MilestoneEvents,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutex = Mutex()

    suspend fun rearm() = mutex.withLock {
        val now = clock.instant()
        val zone = ZoneId.systemDefault()
        val s = settings.current()
        val notifyLists = db.lists().getAll().filter { it.notifyEnabled }.map { it.id }.toSet()
        val allEvents = db.events().getVisible()
        val own = allEvents.filter { it.listId in notifyLists }.map { it.toDomain() }
        // Вехи первого года (шлошим, 11 и 12 месяцев) напоминают как обычные события того же списка (B-26).
        val events = own + milestones.build(own, now.atZone(zone).toLocalDate(), s.adarPolicy)
        val sunset = sunsetProvider(s.cityId, zone)
        val watermark = settings.watermark() ?: now.toEpochMilli().also { settings.setWatermark(it) }

        val today = now.atZone(zone).toLocalDate()
        // Праздники и Шабат: только напоминание накануне вечером — в сам день телефон не беспокоит (B-25).
        val holidayEvents = if (!s.holidays.notify) emptyList() else holidays.build(
            s.holidays, cities().byId(s.cityId), from = today.minusDays(2), to = today.plusDays(400),
            shabbatFrom = today.minusDays(2), shabbatTo = today.plusDays(400), nextDays = false,
            yizkor = allEvents.any { it.type == EventType.YAHRZEIT.name })
        val eveOnly = s.reminders.copy(eveEnabled = true, dayEnabled = false)
        fun plan(from: Instant, to: Instant) = (ReminderPlanner.plan(events, s.reminders, sunset, zone, s.adarPolicy, from, to) +
            ReminderPlanner.plan(holidayEvents, eveOnly, sunset, zone, s.adarPolicy, from, to)).sortedBy { it.triggerAt }

        val due = plan(Instant.ofEpochMilli(watermark), now)
        val byId = (events + holidayEvents).associateBy { it.id }
        for (r in due) {
            val fresh = when (r.kind) {
                ReminderKind.EVE -> today.isBefore(r.occurrenceDate)
                ReminderKind.DAY -> today == r.occurrenceDate
            }
            val e = byId[r.eventId]
            if (fresh && e != null) notifier.show(e, r)
        }
        settings.setWatermark(now.toEpochMilli())

        val next = plan(now, now.plus(Duration.ofDays(400))).firstOrNull()
        if (next == null) alarms.cancel() else alarms.set(next.triggerAt.toEpochMilli(), alarms.canScheduleExact())
    }

    fun sunsetProvider(cityId: String?, zone: ZoneId): SunsetProvider =
        cities().byId(cityId)?.let { CitySunsetProvider(it) } ?: DefaultSunsetProvider(zone)
}

class NotificationFactory(private val baseContext: Context) {
    companion object {
        const val CH_EVE = "reminders_eve"
        const val CH_DAY = "reminders_day"
        const val EXTRA_EVENT_ID = "eventId"
    }

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_EVE, context.getString(R.string.channel_eve), NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_DAY, context.getString(R.string.channel_day), NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun allowed(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Тексты — на языке приложения (на Android < 13 он может отличаться от системного). */
    private val context get() = org.ledor.ui.locale.AppLocale.wrap(baseContext)

    fun show(e: Event, r: PlannedReminder) {
        if (!allowed()) return
        val f = DateFormatters(context)
        val type = f.typeLabel(e, r.yearsSince)
        val hebrew = HebrewCalendarMath.fromGregorian(r.occurrenceDate)
        val age = f.agePhrase(e.type, r.yearsSince)
        val (title, text) = when {
            // Пост «с рассвета» начинается завтра утром, остальные праздники — сегодня вечером.
            e.listId == HOLIDAYS_LIST_ID -> context.getString(
                if (HolidayEvents.holidayOf(e.id)?.dawn == true) R.string.notif_holiday_tomorrow_title else R.string.notif_holiday_eve_title, e.title) to
                listOfNotNull(context.getString(R.string.notif_eve_starts, f.hebrewDate(hebrew)), HolidayEvents.notificationLine(e)).joinToString(" ")
            r.kind == ReminderKind.DAY -> context.getString(R.string.notif_day_title, type, e.title) to
                listOfNotNull("${f.hebrewDate(hebrew)} · ${f.gregorian(r.occurrenceDate)}.", age).joinToString(" ")
            r.calendar == CalendarKind.GREGORIAN && e.recurring -> context.getString(R.string.notif_eve_greg_title, type, e.title) to
                listOfNotNull(f.gregorian(r.occurrenceDate) + ".", age).joinToString(" ")
            e.type == EventType.YAHRZEIT -> context.getString(R.string.notif_eve_yahrzeit_title, e.title) to
                listOfNotNull(context.getString(R.string.notif_eve_starts, f.hebrewDate(hebrew)), context.getString(R.string.notif_candle), age).joinToString(" ")
            else -> context.getString(R.string.notif_eve_title, type, e.title) to
                listOfNotNull(context.getString(R.string.notif_eve_starts, f.hebrewDate(hebrew)), age).joinToString(" ")
        }
        postNotification(
            id = (e.id + r.occurrenceDate + r.kind).hashCode(),
            channel = if (r.kind == ReminderKind.EVE) CH_EVE else CH_DAY,
            title = title, text = text, eventId = e.id,
        )
    }

    fun showTest() {
        if (!allowed()) return
        postNotification(42, CH_DAY, context.getString(R.string.notif_test_title), context.getString(R.string.notif_test_text), null)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun postNotification(id: Int, channel: String, title: String, text: String, eventId: String?) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            eventId?.let { putExtra(EXTRA_EVENT_ID, it) }
        }
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_magen_david)
            .setColor(0xFF0B3FAE.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setGroup("reminders")
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = rearmAsync(context)
}

class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = rearmAsync(context)
}

private fun BroadcastReceiver.rearmAsync(context: Context) {
    val pending = goAsync()
    val app = context.applicationContext as KalendarApp
    CoroutineScope(Dispatchers.Default).launch {
        try {
            withTimeout(9_000) { app.container.reminderScheduler.rearm() }
        } catch (_: Exception) {
        } finally {
            pending.finish()
        }
    }
}

enum class HealthIssue { NOTIFICATIONS_OFF, EXACT_ALARMS_DENIED, BACKGROUND_RESTRICTED, BATTERY_OPTIMIZED }

class ReminderHealthChecker(private val context: Context) {
    fun check(): List<HealthIssue> = buildList {
        val nm = NotificationManagerCompat.from(context)
        val permOk = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val channelsOff = context.getSystemService(NotificationManager::class.java).notificationChannels
            .any { it.importance == NotificationManager.IMPORTANCE_NONE }
        if (!permOk || !nm.areNotificationsEnabled() || channelsOff) add(HealthIssue.NOTIFICATIONS_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) add(HealthIssue.EXACT_ALARMS_DENIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.getSystemService(ActivityManager::class.java).isBackgroundRestricted) add(HealthIssue.BACKGROUND_RESTRICTED)
        if (!context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)) add(HealthIssue.BATTERY_OPTIMIZED)
    }

    private val pkgUri get() = Uri.parse("package:${context.packageName}")

    fun fixIntents(issue: HealthIssue): List<Intent> = when (issue) {
        HealthIssue.NOTIFICATIONS_OFF -> listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
        HealthIssue.EXACT_ALARMS_DENIED -> listOf(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkgUri))
        HealthIssue.BACKGROUND_RESTRICTED -> emptyList()
        HealthIssue.BATTERY_OPTIMIZED -> listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        )
    } + Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)

    fun manufacturerHint(): Int? {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            "samsung" in m -> R.string.hint_samsung
            listOf("xiaomi", "redmi", "poco").any { it in m } -> R.string.hint_xiaomi
            listOf("huawei", "honor").any { it in m } -> R.string.hint_huawei
            listOf("oppo", "realme", "oneplus").any { it in m } -> R.string.hint_oppo
            "vivo" in m -> R.string.hint_vivo
            else -> null
        }
    }
}

/** Последняя дата, до которой ещё актуален тестовый ориентир «сегодня» — используется для предпросмотра. */
fun previewEveTrigger(scheduler: ReminderScheduler, cityId: String?, s: org.ledor.domain.reminder.ReminderSettings): Pair<Instant, Instant> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val sp = scheduler.sunsetProvider(cityId, zone)
    return sp.sunset(today) to ReminderPlanner.eveTrigger(today.plusDays(1), s, sp, zone)
}
