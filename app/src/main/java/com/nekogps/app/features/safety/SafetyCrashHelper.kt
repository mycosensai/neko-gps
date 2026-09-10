package com.nekogps.app.features.safety

/**
 * Extracted UI-list helpers for [SafetyActivity] so the activity
 * stays below the TooManyFunctions threshold.
 *
 * Public API preserved: every method that callers depend on still
 * exists on SafetyActivity and delegates here.
 */
class SafetyCrashHelper(private val activity: SafetyActivity) {

    fun bindCrashAndFatigueListeners() {
        activity.crashMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                activity.crashDetectionManager.startMonitoring()
            } else {
                activity.crashDetectionManager.stopMonitoring()
            }
        }
        activity.fatigueMonitorSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                activity.fatigueDetectionManager.startMonitoring()
            } else {
                activity.fatigueDetectionManager.stopMonitoring()
            }
        }
        activity.breakIntervalSeekBar.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean
                ) {
                    if (fromUser) {
                        val minutes = progress + BREAK_INTERVAL_SEEK_OFFSET_MINUTES
                    activity.fatigueDetectionManager.setBreakIntervalMinutes(minutes)
                    activity.breakIntervalText.text = activity.getString(R.string.fatigue_break_interval, minutes)
                }
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) = Unit
        })
    }

    fun updateCrashStatus() {
        if (activity.crashDetectionManager.isMonitoring()) {
            activity.crashStatusText.text = activity.getString(R.string.crash_detection_active)
            activity.crashStatusText.setTextColor(
                android.content.ContextCompat.getColor(activity, R.color.success_green)
            )
        } else {
            activity.crashStatusText.text = activity.getString(R.string.crash_detection_inactive)
            activity.crashStatusText.setTextColor(
                android.content.ContextCompat.getColor(activity, R.color.text_secondary)
            )
        }
    }

    fun updateDrivingTimeDisplay() {
        val minutes = activity.fatigueDetectionManager.getTotalDrivingMinutes()
        val hours = minutes / MINUTES_PER_HOUR
        val mins = minutes % MINUTES_PER_HOUR
        activity.drivingTimeText.text = if (hours > 0) {
            activity.getString(R.string.fatigue_driving_time_hours, hours, mins)
        } else {
            activity.getString(R.string.fatigue_driving_time_minutes, mins)
        }
    }
}

private const val BREAK_INTERVAL_SEEK_OFFSET_MINUTES = 30
private const val MINUTES_PER_HOUR = 60
