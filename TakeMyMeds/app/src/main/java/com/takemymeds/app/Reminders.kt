package com.takemymeds.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Shared constants and small helpers used across the app.
 *
 * Two doses are supported, identified by DOSE_1 and DOSE_2. Everything that
 * needs to be remembered between app restarts / reboots (chosen times,
 * whether a dose is enabled, whether it was already marked "taken" today,
 * the follow-up time) lives in a single SharedPreferences file.
 */
object Reminders {

    const val PREFS_NAME = "take_my_meds_prefs"

    const val DOSE_1 = 1
    const val DOSE_2 = 2

    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_IS_FOLLOW_UP = "extra_is_follow_up"

    const val DEFAULT_DOSE_1_HOUR = 8
    const val DEFAULT_DOSE_1_MINUTE = 0
    const val DEFAULT_DOSE_2_HOUR = 20
    const val DEFAULT_DOSE_2_MINUTE = 0

    // Default follow-up time is simply 30 minutes after the default dose time.
    const val DEFAULT_DOSE_1_FOLLOW_UP_HOUR = 8
    const val DEFAULT_DOSE_1_FOLLOW_UP_MINUTE = 30
    const val DEFAULT_DOSE_2_FOLLOW_UP_HOUR = 20
    const val DEFAULT_DOSE_2_FOLLOW_UP_MINUTE = 30

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayString(): String = dateFormat.format(System.currentTimeMillis())

    fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context, doseId: Int): Boolean =
        prefs(context).getBoolean("dose${doseId}_enabled", true)

    fun hourFor(context: Context, doseId: Int): Int {
        val default = if (doseId == DOSE_1) DEFAULT_DOSE_1_HOUR else DEFAULT_DOSE_2_HOUR
        return prefs(context).getInt("dose${doseId}_hour", default)
    }

    fun minuteFor(context: Context, doseId: Int): Int {
        val default = if (doseId == DOSE_1) DEFAULT_DOSE_1_MINUTE else DEFAULT_DOSE_2_MINUTE
        return prefs(context).getInt("dose${doseId}_minute", default)
    }

    /** Whether the single follow-up reminder is turned on for this dose. */
    fun followUpEnabled(context: Context, doseId: Int): Boolean =
        prefs(context).getBoolean("dose${doseId}_followup_enabled", true)

    fun followUpHourFor(context: Context, doseId: Int): Int {
        val default = if (doseId == DOSE_1) DEFAULT_DOSE_1_FOLLOW_UP_HOUR else DEFAULT_DOSE_2_FOLLOW_UP_HOUR
        return prefs(context).getInt("dose${doseId}_followup_hour", default)
    }

    fun followUpMinuteFor(context: Context, doseId: Int): Int {
        val default = if (doseId == DOSE_1) DEFAULT_DOSE_1_FOLLOW_UP_MINUTE else DEFAULT_DOSE_2_FOLLOW_UP_MINUTE
        return prefs(context).getInt("dose${doseId}_followup_minute", default)
    }

    fun wasTakenToday(context: Context, doseId: Int): Boolean =
        prefs(context).getString("dose${doseId}_taken_date", "") == todayString()

    fun markTakenToday(context: Context, doseId: Int) {
        prefs(context).edit().putString("dose${doseId}_taken_date", todayString()).apply()
    }

    fun doseLabel(doseId: Int): String = if (doseId == DOSE_1) "Dose 1" else "Dose 2"
}
