package com.vag.mychime.preferences

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat
import java.util.Calendar

class TimePickerDialogFragment : PreferenceDialogFragmentCompat(),
    TimePickerDialog.OnTimeSetListener {
    private val tag = "TimePickerDialogFragment"

    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePickerDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(tag, "onCreateDialog")
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        picker = TimePickerDialog(requireActivity(), this, hour, minute, DateFormat.is24HourFormat(activity))
        return picker as TimePickerDialog
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        Log.d(tag, "onTimeSet hour $hour minute $minute")
        lastHour = hour
        lastMinute = minute

        val time = String.format("%02d:%02d", lastHour, lastMinute)
        if (callChangeListener(time)) {
            (preference as? TimePickerPreference)?.let {
                it.summary = time
                it.persistString(time)
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            Log.d(tag, "onDialogClosed positive")
        }
    }
}
