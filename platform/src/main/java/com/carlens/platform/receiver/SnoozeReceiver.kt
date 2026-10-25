package com.carlens.platform.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.carlens.domain.repository.MaintenanceRepository
import com.carlens.domain.usecase.snoozeUntil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@AndroidEntryPoint
class SnoozeReceiver : BroadcastReceiver() {

    @Inject lateinit var maintenanceRepo: MaintenanceRepository
    @Inject lateinit var clock: Clock

    companion object {
        const val EXTRA_RULE_ID = "extra_rule_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_DAYS = "extra_days"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val days = intent.getLongExtra(EXTRA_DAYS, 7L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val until = snoozeUntil(clock, days)
                maintenanceRepo.snooze(ruleId, until)
                NotificationManagerCompat.from(context).cancel(notifId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
