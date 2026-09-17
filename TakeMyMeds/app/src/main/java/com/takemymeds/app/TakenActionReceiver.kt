package com.takemymeds.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/** Handles the "Taken" action button on a dose notification. */
class TakenActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getIntExtra(Reminders.EXTRA_DOSE_ID, -1)
        if (doseId == -1) return

        Reminders.markTakenToday(context, doseId)
        NotificationHelper.cancelDoseNotification(context, doseId)
        AlarmScheduler.cancelFollowUp(context, doseId)

        Toast.makeText(
            context.applicationContext,
            "${Reminders.doseLabel(doseId)} marked as taken",
            Toast.LENGTH_SHORT
        ).show()
    }
}
