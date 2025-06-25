package com.vag.mychime.preferences

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.widget.TimePicker
import androidx.preference.DialogPreference

class TimePickerPreference(ctxt: Context, attrs: AttributeSet) : DialogPreference(ctxt, attrs) {
    private val TAG = "TimePickerPreference"
    private var lastHour = 0
    private var lastMinute = 0
    private var picker: TimePicker? = null
    private var time: String? = null
    private val ctx: Context = ctxt

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

    init {
        Log.d(TAG, "TimePickerPreference")
        positiveButtonText = "Set"
        negativeButtonText = "Cancel"
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
        Log.d(TAG, "onGetDefaultValue")
        return a.getString(index)
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        time = null
        Log.d(TAG, "onSetInitialValue")
        time = if (defaultValue == null) {
            getPersistedString("00:00")
        } else {
            getPersistedString(defaultValue.toString())
        }
        lastHour = getHour(time!!)
        lastMinute = getMinute(time!!)
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

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(`in`: Parcel) = SavedState(`in`)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        Log.d(TAG, "onSaveInstanceState")
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            return superState
        }
        val myState = SavedState(superState)
        myState.value = time
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d(TAG, "onRestoreInstanceState")
        if (state == null || state.javaClass != SavedState::class.java) {
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        picker?.hour = getHour(myState.value!!)
        picker?.minute = getMinute(myState.value!!)
        summary = time
    }
}
