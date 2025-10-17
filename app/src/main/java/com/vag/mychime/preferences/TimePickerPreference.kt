package com.vag.mychime.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.widget.TimePicker
import androidx.preference.DialogPreference

class TimePickerPreference(context: Context, attrs: AttributeSet) : DialogPreference(context, attrs) {

    private val tag = "TimePickerPreference"

    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePicker? = null
    private var time: String? = null

    init {
        Log.d(tag, "TimePickerPreference")
        positiveButtonText = context.getString(android.R.string.ok)
        negativeButtonText = context.getString(android.R.string.cancel)
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        Log.d(tag, "onGetDefaultValue")
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        time = null
        Log.d(tag, "onSetInitialValue")
        time = if (defaultValue == null) {
            getPersistedString("00:00")
        } else {
            getPersistedString(defaultValue.toString())
        }

        lastHour = getHour(time ?: "00:00")
        lastMinute = getMinute(time ?: "00:00")

        summary = time
    }

    override fun onSaveInstanceState(): Parcelable? {
        Log.d(tag, "onSaveInstanceState")
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            return superState
        }

        val myState = SavedState(superState)
        myState.value = time
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d(tag, "onRestoreInstanceState")
        if (state == null || state.javaClass != SavedState::class.java) {
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)

        picker?.hour = getHour(myState.value ?: "00:00")
        picker?.minute = getMinute(myState.value ?: "00:00")

        summary = time
    }

    private class SavedState : BaseSavedState {
        var value: String? = null

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            value = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(value)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
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
