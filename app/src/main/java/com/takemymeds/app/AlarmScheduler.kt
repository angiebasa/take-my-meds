package com.takemymeds.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Wraps all AlarmManager scheduling/cancelling so the rest of the app never
 * has to build PendingIntents by hand.
 */
object AlarmScheduler {

    // Distinct, stable request codes so each PendingIntent can be found again
    // (to cancel it) without accidentally overwriting a different one.
    private fun requestCode(doseId: Int, isFollowUp: Boolean): Int =
        doseId * 10 + if (isFollowUp) 1 else 0

    private fun mainAlarmIntent(context: Context, doseId: Int, isFollowUp: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(Reminders.EXTRA_DOSE_ID, doseId)
            putExtra(Reminders.EXTRA_IS_FOLLOW_UP, isFollowUp)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(doseId, isFollowUp), intent, flags)
    }

    private fun existingAlarmIntent(context: Context, doseId: Int, isFollowUp: Boolean): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(doseId, isFollowUp), intent, flags)
    }

    private fun schedule(context: Context, pendingIntent: PendingIntent, triggerAtMillis: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canBeExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            // No permission for exact alarms yet: fall back to an approximate
            // alarm rather than not scheduling anything at all.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /** Next occurrence of [hour]:[minute], today if it hasn't passed yet, otherwise tomorrow. */
    fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DATE, 1)
        }
        return cal.timeInMillis
    }

    /** Schedules the next daily reminder for a dose (today if still upcoming, else tomorrow). */
    fun scheduleDose(context: Context, doseId: Int, hour: Int, minute: Int) {
        val pendingIntent = mainAlarmIntent(context, doseId, isFollowUp = false)
        schedule(context, pendingIntent, nextTriggerMillis(hour, minute))
    }

    /**
     * Schedules the single follow-up reminder for the given clock time. Called right after the
     * main reminder fires, so [nextTriggerMillis] naturally lands on "later today" when the
     * follow-up time is after the dose time, or rolls to tomorrow if it isn't.
     */
    fun scheduleFollowUp(context: Context, doseId: Int, hour: Int, minute: Int) {
        val pendingIntent = mainAlarmIntent(context, doseId, isFollowUp = true)
        schedule(context, pendingIntent, nextTriggerMillis(hour, minute))
    }

    /** Cancels a pending follow-up reminder for a dose, e.g. once it's marked "Taken". */
    fun cancelFollowUp(context: Context, doseId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = existingAlarmIntent(context, doseId, isFollowUp = true) ?: return
        am.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Cancels the daily reminder for a dose entirely (used when a dose is disabled). */
    fun cancelDose(context: Context, doseId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        existingAlarmIntent(context, doseId, isFollowUp = false)?.let {
            am.cancel(it)
            it.cancel()
        }
        cancelFollowUp(context, doseId)
    }

    /** Re-applies whatever is saved in SharedPreferences. Used on Save and after a reboot. */
    fun rescheduleAll(context: Context) {
        for (doseId in listOf(Reminders.DOSE_1, Reminders.DOSE_2)) {
            if (Reminders.isEnabled(context, doseId)) {
                scheduleDose(context, doseId, Reminders.hourFor(context, doseId), Reminders.minuteFor(context, doseId))
            } else {
                cancelDose(context, doseId)
            }
        }
    }
}
