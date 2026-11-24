package org.ledor.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.ledor.KalendarApp
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = (applicationContext as KalendarApp).container
        val report = c.syncEngine.syncAll()
        c.reminderScheduler.rearm()
        return if (report.networkError && runAttemptCount < 5) Result.retry() else Result.success()
    }
}

class SyncScheduler(private val context: Context) {
    private val wm get() = WorkManager.getInstance(context)
    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun update(hasCloudLists: Boolean) {
        if (hasCloudLists) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            wm.enqueueUniquePeriodicWork("daily-sync", ExistingPeriodicWorkPolicy.KEEP, req)
        } else {
            wm.cancelUniqueWork("daily-sync")
        }
    }

    fun pushSoon() {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(net)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniqueWork("push-sync", ExistingWorkPolicy.REPLACE, req)
    }
}
