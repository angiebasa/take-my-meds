package com.takemymeds.app

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private var dose1Hour = Reminders.DEFAULT_DOSE_1_HOUR
    private var dose1Minute = Reminders.DEFAULT_DOSE_1_MINUTE
    private var dose2Hour = Reminders.DEFAULT_DOSE_2_HOUR
    private var dose2Minute = Reminders.DEFAULT_DOSE_2_MINUTE

    private var dose1FollowUpHour = Reminders.DEFAULT_DOSE_1_FOLLOW_UP_HOUR
    private var dose1FollowUpMinute = Reminders.DEFAULT_DOSE_1_FOLLOW_UP_MINUTE
    private var dose2FollowUpHour = Reminders.DEFAULT_DOSE_2_FOLLOW_UP_HOUR
    private var dose2FollowUpMinute = Reminders.DEFAULT_DOSE_2_FOLLOW_UP_MINUTE

    private lateinit var dose1Enabled: CheckBox
    private lateinit var dose2Enabled: CheckBox
    private lateinit var dose1TimeButton: Button
    private lateinit var dose2TimeButton: Button

    private lateinit var dose1FollowUpEnabled: CheckBox
    private lateinit var dose2FollowUpEnabled: CheckBox
    private lateinit var dose1FollowUpTimeButton: Button
    private lateinit var dose2FollowUpTimeButton: Button

    private lateinit var statusText: TextView
    private lateinit var exactAlarmButton: Button

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dose1Enabled = findViewById(R.id.dose1Enabled)
        dose2Enabled = findViewById(R.id.dose2Enabled)
        dose1TimeButton = findViewById(R.id.dose1TimeButton)
        dose2TimeButton = findViewById(R.id.dose2TimeButton)

        dose1FollowUpEnabled = findViewById(R.id.dose1FollowUpEnabled)
        dose2FollowUpEnabled = findViewById(R.id.dose2FollowUpEnabled)
        dose1FollowUpTimeButton = findViewById(R.id.dose1FollowUpTimeButton)
        dose2FollowUpTimeButton = findViewById(R.id.dose2FollowUpTimeButton)

        statusText = findViewById(R.id.statusText)
        exactAlarmButton = findViewById(R.id.exactAlarmButton)

        loadFromPrefs()
        updateTimeButtonLabels()

        dose1TimeButton.setOnClickListener {
            pickTime(dose1Hour, dose1Minute) { h, m ->
                dose1Hour = h; dose1Minute = m; updateTimeButtonLabels()
            }
        }
        dose2TimeButton.setOnClickListener {
            pickTime(dose2Hour, dose2Minute) { h, m ->
                dose2Hour = h; dose2Minute = m; updateTimeButtonLabels()
            }
        }
        dose1FollowUpTimeButton.setOnClickListener {
            pickTime(dose1FollowUpHour, dose1FollowUpMinute) { h, m ->
                dose1FollowUpHour = h; dose1FollowUpMinute = m; updateTimeButtonLabels()
            }
        }
        dose2FollowUpTimeButton.setOnClickListener {
            pickTime(dose2FollowUpHour, dose2FollowUpMinute) { h, m ->
                dose2FollowUpHour = h; dose2FollowUpMinute = m; updateTimeButtonLabels()
            }
        }

        findViewById<Button>(R.id.saveButton).setOnClickListener { saveAndSchedule() }

        exactAlarmButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updateExactAlarmButtonVisibility()
        updateStatusText()
    }

    private fun loadFromPrefs() {
        dose1Enabled.isChecked = Reminders.isEnabled(this, Reminders.DOSE_1)
        dose2Enabled.isChecked = Reminders.isEnabled(this, Reminders.DOSE_2)
        dose1Hour = Reminders.hourFor(this, Reminders.DOSE_1)
        dose1Minute = Reminders.minuteFor(this, Reminders.DOSE_1)
        dose2Hour = Reminders.hourFor(this, Reminders.DOSE_2)
        dose2Minute = Reminders.minuteFor(this, Reminders.DOSE_2)

        dose1FollowUpEnabled.isChecked = Reminders.followUpEnabled(this, Reminders.DOSE_1)
        dose2FollowUpEnabled.isChecked = Reminders.followUpEnabled(this, Reminders.DOSE_2)
        dose1FollowUpHour = Reminders.followUpHourFor(this, Reminders.DOSE_1)
        dose1FollowUpMinute = Reminders.followUpMinuteFor(this, Reminders.DOSE_1)
        dose2FollowUpHour = Reminders.followUpHourFor(this, Reminders.DOSE_2)
        dose2FollowUpMinute = Reminders.followUpMinuteFor(this, Reminders.DOSE_2)
    }

    private fun pickTime(hour: Int, minute: Int, onPicked: (Int, Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, h, m -> onPicked(h, m) },
            hour,
            minute,
            DateFormat.is24HourFormat(this)
        ).show()
    }

    private fun updateTimeButtonLabels() {
        dose1TimeButton.text = formatTime(dose1Hour, dose1Minute)
        dose2TimeButton.text = formatTime(dose2Hour, dose2Minute)
        dose1FollowUpTimeButton.text = formatTime(dose1FollowUpHour, dose1FollowUpMinute)
        dose2FollowUpTimeButton.text = formatTime(dose2FollowUpHour, dose2FollowUpMinute)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val fmt = if (DateFormat.is24HourFormat(this)) "HH:mm" else "h:mm a"
        return DateFormat.format(fmt, cal).toString()
    }

    /** True if [followHour]:[followMinute] is later in the day than [doseHour]:[doseMinute]. */
    private fun isLaterSameDay(doseHour: Int, doseMinute: Int, followHour: Int, followMinute: Int): Boolean {
        val doseMinutes = doseHour * 60 + doseMinute
        val followMinutes = followHour * 60 + followMinute
        return followMinutes > doseMinutes
    }

    private fun saveAndSchedule() {
        Reminders.prefs(this).edit()
            .putBoolean("dose${Reminders.DOSE_1}_enabled", dose1Enabled.isChecked)
            .putBoolean("dose${Reminders.DOSE_2}_enabled", dose2Enabled.isChecked)
            .putInt("dose${Reminders.DOSE_1}_hour", dose1Hour)
            .putInt("dose${Reminders.DOSE_1}_minute", dose1Minute)
            .putInt("dose${Reminders.DOSE_2}_hour", dose2Hour)
            .putInt("dose${Reminders.DOSE_2}_minute", dose2Minute)
            .putBoolean("dose${Reminders.DOSE_1}_followup_enabled", dose1FollowUpEnabled.isChecked)
            .putBoolean("dose${Reminders.DOSE_2}_followup_enabled", dose2FollowUpEnabled.isChecked)
            .putInt("dose${Reminders.DOSE_1}_followup_hour", dose1FollowUpHour)
            .putInt("dose${Reminders.DOSE_1}_followup_minute", dose1FollowUpMinute)
            .putInt("dose${Reminders.DOSE_2}_followup_hour", dose2FollowUpHour)
            .putInt("dose${Reminders.DOSE_2}_followup_minute", dose2FollowUpMinute)
            .apply()

        AlarmScheduler.rescheduleAll(this)

        Toast.makeText(this, "Reminders scheduled", Toast.LENGTH_SHORT).show()
        updateStatusText()
        warnIfFollowUpNotLaterSameDay()
    }

    /** Follow-up times earlier in the clock than their dose still work, they just land on the
     * next day instead of later the same day — flag that so it's not a surprise. */
    private fun warnIfFollowUpNotLaterSameDay() {
        val warnings = mutableListOf<String>()
        if (dose1FollowUpEnabled.isChecked && !isLaterSameDay(dose1Hour, dose1Minute, dose1FollowUpHour, dose1FollowUpMinute)) {
            warnings.add("Dose 1")
        }
        if (dose2FollowUpEnabled.isChecked && !isLaterSameDay(dose2Hour, dose2Minute, dose2FollowUpHour, dose2FollowUpMinute)) {
            warnings.add("Dose 2")
        }
        if (warnings.isNotEmpty()) {
            Toast.makeText(
                this,
                "${warnings.joinToString(" & ")} follow-up time is not later than the dose time, so it will fire the next day",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateExactAlarmButtonVisibility() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !(getSystemService(ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        exactAlarmButton.visibility = if (needsPermission) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun updateStatusText() {
        val d1 = if (Reminders.wasTakenToday(this, Reminders.DOSE_1)) "taken today" else "not taken yet today"
        val d2 = if (Reminders.wasTakenToday(this, Reminders.DOSE_2)) "taken today" else "not taken yet today"
        statusText.text = "Dose 1: $d1\nDose 2: $d2"
    }
}
