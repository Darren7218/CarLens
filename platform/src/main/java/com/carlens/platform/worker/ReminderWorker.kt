package com.carlens.platform.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.carlens.domain.repository.ReminderRepository
import com.carlens.domain.usecase.CollectDueReminders
import com.carlens.platform.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val collectDueReminders: CollectDueReminders,
    private val reminderRepo: ReminderRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val candidates = collectDueReminders()
        for (candidate in candidates) {
            val alreadyDelivered = reminderRepo.wasDelivered(candidate.key)
            if (!alreadyDelivered) {
                notificationHelper.showReminderNotification(candidate)
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "carlens_reminder_worker"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            // Run check once every 12 hours
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
