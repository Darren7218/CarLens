package com.carlens.platform.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.carlens.domain.model.ReminderDelivery
import com.carlens.domain.model.ReminderTargetType
import com.carlens.domain.repository.ReminderRepository
import com.carlens.domain.repository.SettingsRepository
import com.carlens.domain.usecase.ReminderCandidate
import com.carlens.platform.receiver.SnoozeReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val reminderRepo: ReminderRepository,
    private val clock: Clock
) {
    companion object {
        const val CHANNEL_ID = "carlens_reminders"
        const val CHANNEL_NAME = "Vehicle Reminders"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Maintenance and renewal notifications for your vehicles"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    suspend fun showReminderNotification(candidate: ReminderCandidate) {
        val appSettings = settings.current()

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val notifId = candidate.key.hashCode()

        // Main content Intent (opens MainActivity)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("target_type", candidate.targetType.name)
            putExtra("target_id", candidate.targetId)
            putExtra("vehicle_id", candidate.vehicleId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (appSettings.hideNotificationDetails) {
            "Vehicle Reminder"
        } else {
            "${candidate.vehicleName}: ${candidate.title}"
        }

        val text = if (appSettings.hideNotificationDetails) {
            "An item is due for attention. Tap to view."
        } else {
            candidate.detail
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        // If maintenance, add Snooze action (7 days)
        if (candidate.targetType == ReminderTargetType.MAINTENANCE) {
            val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                putExtra(SnoozeReceiver.EXTRA_RULE_ID, candidate.targetId)
                putExtra(SnoozeReceiver.EXTRA_NOTIF_ID, notifId)
                putExtra(SnoozeReceiver.EXTRA_DAYS, 7L)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notifId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_recent_history,
                "Snooze 7 days",
                snoozePendingIntent
            )
        }

        try {
            notificationManager.notify(notifId, builder.build())
            // Record delivery to prevent repeated notifications for the same cycle/stage
            reminderRepo.recordDelivery(
                ReminderDelivery(
                    key = candidate.key,
                    targetType = candidate.targetType,
                    targetId = candidate.targetId,
                    dueCycle = candidate.dueCycle,
                    stage = candidate.stage,
                    deliveredAt = clock.millis()
                )
            )
        } catch (_: SecurityException) {
            // Notification permission denied on Android 13+
        }
    }
}
