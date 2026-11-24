package org.ledor

import android.app.Application
import android.content.Context
import org.ledor.data.local.AppDatabase
import org.ledor.data.prefs.SettingsStore
import org.ledor.data.remote.SupabaseRpc
import org.ledor.data.repo.ChangeListener
import org.ledor.data.repo.EventRepository
import org.ledor.data.repo.ListRepository
import org.ledor.data.repo.SyncEngine
import org.ledor.domain.reminder.CityCatalog
import org.ledor.reminders.AndroidAlarmGateway
import org.ledor.reminders.NotificationFactory
import org.ledor.reminders.ReminderHealthChecker
import org.ledor.reminders.ReminderScheduler
import org.ledor.sync.SyncScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Ручное внедрение зависимостей: всё лениво, ничего тяжёлого на главном потоке. */
class AppContainer(private val context: Context) {
    /** Сбой фоновой задачи не должен ронять приложение. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e -> android.util.Log.w("Kalendar", "background task failed", e) })

    val database by lazy { AppDatabase.build(context) }
    val settingsStore by lazy { SettingsStore(context) }
    val rpc by lazy { SupabaseRpc(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) }
    val cityCatalog by lazy { CityCatalog.loadFromResources() }
    val notifications by lazy { NotificationFactory(context) }
    val healthChecker by lazy { ReminderHealthChecker(context) }
    val syncScheduler by lazy { SyncScheduler(context) }

    private val rearmRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val changeListener = ChangeListener { cloudListId ->
        rearmRequests.tryEmit(Unit)
        if (cloudListId != null) runCatching { syncScheduler.pushSoon() }
    }

    val reminderScheduler by lazy {
        ReminderScheduler(database, settingsStore, { cityCatalog }, AndroidAlarmGateway(context), notifications)
    }

    val syncEngine by lazy {
        SyncEngine(database, rpc, settingsStore,
            deletedNotice = { name -> context.getString(R.string.notice_list_deleted, name) },
            afterSync = { rearmRequests.tryEmit(Unit) })
    }

    val eventRepository by lazy { EventRepository(database, changeListener) }
    val listRepository by lazy { ListRepository(database, rpc, syncEngine, changeListener) }

    @OptIn(FlowPreview::class)
    fun start() {
        notifications.createChannels()
        appScope.launch {
            merge(
                rearmRequests,
                settingsStore.settings.map { Triple(it.reminders, it.cityId, it.adarPolicy) }.distinctUntilChanged().drop(1).map { },
                database.lists().observeAll().map { l -> l.map { it.id to it.notifyEnabled } }.distinctUntilChanged().drop(1).map { },
            ).debounce(500).collect { runCatching { reminderScheduler.rearm() } }
        }
        appScope.launch {
            database.lists().observeAll().map { l -> l.any { it.isCloud } }.distinctUntilChanged()
                .collect { runCatching { syncScheduler.update(it) } }
        }
        appScope.launch { runCatching { reminderScheduler.rearm() } }
        appScope.launch {
            val last = settingsStore.lastSyncAttempt()
            if (rpc.isConfigured && System.currentTimeMillis() - last > 15 * 60_000L) runCatching { syncEngine.syncAll() }
        }
    }
}

class KalendarApp : Application(), androidx.work.Configuration.Provider {
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) = super.attachBaseContext(org.ledor.ui.locale.AppLocale.wrap(base))

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}
