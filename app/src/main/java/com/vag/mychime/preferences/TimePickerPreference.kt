package com.vag.mychime.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.widget.TimePicker
import androidx.preference.DialogPreference

class TimePickerPreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {
    private val tag = "TimePickerPreference"

    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePicker? = null
    private var time: String = "00:00"

    init {
        Log.d(tag, "TimePickerPreference")
        positiveButtonText = "Set"
        negativeButtonText = "Cancel"
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        Log.d(tag, "onGetDefaultValue")
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        Log.d(tag, "onSetInitialValue")
        time = if (defaultValue == null) {
            getPersistedString("00:00")
        } else {
            getPersistedString(defaultValue.toString())
        }

        lastHour = getHour(time)
        lastMinute = getMinute(time)

        summary = time
    }

    override fun onSaveInstanceState(): Parcelable? {
        Log.d(tag, "onSaveInstanceState")
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            return superState
        }

        return SavedState(superState).also { state ->
            state.value = time
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d(tag, "onRestoreInstanceState")
        if (state == null || state.javaClass != SavedState::class.java) {
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)

        picker?.hour = getHour(myState.value)
        picker?.minute = getMinute(myState.value)

        summary = time
    }

    private class SavedState : BaseSavedState {
        var value: String = ""

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            value = source.readString() ?: ""
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }

    companion object {
        fun getHour(time: String): Int {
            val pieces = time.split(":")
            return pieces[0].toInt()
        }

        fun getMinute(time: String): Int {
            val pieces = time.split(":")
            return pieces[1].toInt()
        }
    }
}
