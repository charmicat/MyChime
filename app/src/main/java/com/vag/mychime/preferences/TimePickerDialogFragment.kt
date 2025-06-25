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
    private val TAG = "TimePickerDialogFragment"
    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePickerDialog? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog")
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        picker = TimePickerDialog(requireActivity(), this, hour, minute,
            DateFormat.is24HourFormat(requireActivity()))
        return picker as TimePickerDialog
    }

    override fun onTimeSet(view: TimePicker, hour: Int, minute: Int) {
        Log.d(TAG, "onTimeSet hour $hour minute $minute")
        lastHour = hour
        lastMinute = minute
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            Log.d(TAG, "onCreate")
        }
    }
}
