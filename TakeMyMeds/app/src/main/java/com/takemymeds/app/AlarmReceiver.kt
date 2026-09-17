package com.takemymeds.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires both for the main daily reminder and for the single follow-up
 * reminder (distinguished by EXTRA_IS_FOLLOW_UP).
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val doseId = intent.getIntExtra(Reminders.EXTRA_DOSE_ID, -1)
        if (doseId == -1) return
        val isFollowUp = intent.getBooleanExtra(Reminders.EXTRA_IS_FOLLOW_UP, false)

        // If it was already marked "Taken" today (e.g. the person took it
        // before the follow-up fired, or even before the main alarm fired),
        // don't notify.
        if (!Reminders.wasTakenToday(context, doseId)) {
            NotificationHelper.showDoseNotification(context, doseId, isFollowUp)

            if (!isFollowUp && Reminders.followUpEnabled(context, doseId)) {
                AlarmScheduler.scheduleFollowUp(
                    context,
                    doseId,
                    Reminders.followUpHourFor(context, doseId),
                    Reminders.followUpMinuteFor(context, doseId)
                )
            }
        }

        // The main (non-follow-up) alarm always re-arms itself for tomorrow,
        // regardless of whether it notified just now.
        if (!isFollowUp && Reminders.isEnabled(context, doseId)) {
            AlarmScheduler.scheduleDose(
                context,
                doseId,
                Reminders.hourFor(context, doseId),
                Reminders.minuteFor(context, doseId)
            )
        }
    }
}
