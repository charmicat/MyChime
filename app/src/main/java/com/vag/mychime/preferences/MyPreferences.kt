package com.vag.mychime.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.vag.mychime.R
import androidx.preference.PreferenceManager

class MyPreferences : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var onCfgChangedCB: OnConfigurationChangedListener? = null
    private lateinit var settings: SharedPreferences

    private val tag = "MyPreferences"

    private var hasVibration = false

    interface OnConfigurationChangedListener {
        fun onConfigurationChanged()
    }

    init {
        Log.d(tag, "MyPreferences ctr")
    }

    private fun findAndAssertPreference(key: String): Preference {
        return requireNotNull(findPreference(key))
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(tag, "onCreatePreferences rootKey: $rootKey")

        settings = PreferenceManager.getDefaultSharedPreferences(context)
        hasVibration = settings.getBoolean("hasVibration", false)
        Log.d(tag, "onCreatePreferences ${settings.all}")
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val pcSpeak = findAndAssertPreference("speak") as PreferenceCategory
        val pcChime = findAndAssertPreference("chime") as PreferenceCategory
        val pcVibration = findAndAssertPreference("vibration") as PreferenceCategory
        val enableSpeak = findAndAssertPreference("enableSpeak") as SwitchPreferenceCompat
        val enableChime = findAndAssertPreference("enableChime") as SwitchPreferenceCompat
        val enableVibration = findAndAssertPreference("enableVibration") as SwitchPreferenceCompat
        val tpStartSpeak = findAndAssertPreference("speakStartTime") as TimePickerPreference
        val tpEndSpeak = findAndAssertPreference("speakEndTime") as TimePickerPreference
        val tpStartChime = findAndAssertPreference("chimeStartTime") as TimePickerPreference
        val tpEndChime = findAndAssertPreference("chimeEndTime") as TimePickerPreference
        val tpStartVibration = findAndAssertPreference("vibrationStartTime") as TimePickerPreference
        val tpEndVibration = findAndAssertPreference("vibrationEndTime") as TimePickerPreference

        enableSpeak.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue.toString().toBoolean()
            if (!enabled) {
                pcSpeak.removePreference(tpEndSpeak)
                pcSpeak.removePreference(tpStartSpeak)
            }
            true
        }

        val speakEnableList = findAndAssertPreference("speakOn") as ListPreference
        if (speakEnableList.value == null) {
            speakEnableList.setValueIndex(0)
        }
        if (!enableSpeak.isChecked || speakEnableList.value != "speakTimeRange") {
            pcSpeak.removePreference(tpEndSpeak)
            pcSpeak.removePreference(tpStartSpeak)
        }
        speakEnableList.setOnPreferenceChangeListener { _, newValue ->
            val value = newValue.toString()
            if (value != "speakTimeRange") {
                pcSpeak.removePreference(tpEndSpeak)
                pcSpeak.removePreference(tpStartSpeak)
            } else {
                pcSpeak.addPreference(tpEndSpeak)
                pcSpeak.addPreference(tpStartSpeak)
            }
            true
        }

        enableChime.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue.toString().toBoolean()
            if (!enabled) {
                pcChime.removePreference(tpEndChime)
                pcChime.removePreference(tpStartChime)
            }
            true
        }
        val chimeEnableList = findAndAssertPreference("chimeOn") as ListPreference
        if (chimeEnableList.value == null) {
            chimeEnableList.setValueIndex(0)
        }
        if (!enableChime.isChecked || chimeEnableList.value != "chimeTimeRange") {
            pcChime.removePreference(tpEndChime)
            pcChime.removePreference(tpStartChime)
        }
        chimeEnableList.setOnPreferenceChangeListener { preference, newValue ->
            val value = newValue.toString()
            if (value != "chimeTimeRange") {
                pcChime.removePreference(tpEndChime)
                pcChime.removePreference(tpStartChime)
            } else {
                pcChime.addPreference(tpEndChime)
                pcChime.addPreference(tpStartChime)
            }
            true
        }

        val clockTypeList = findAndAssertPreference("clockType") as ListPreference
        if (clockTypeList.value == null) {
            clockTypeList.setValueIndex(0)
        }
        clockTypeList.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }

        if (!hasVibration) {
            Log.d(tag, "onCreatePreferences: device doesn't support vibration. Disabling controls")
            pcVibration.setTitle(R.string.vibrationCpt)
            pcVibration.setSummary(R.string.vibrationNotSupportedCpt)
            pcVibration.removeAll()
        } else {
            val vibrationEnableList = findAndAssertPreference("vibrationOn") as ListPreference
            if (vibrationEnableList.value == null) {
                vibrationEnableList.setValueIndex(0)
            }
            if (!enableVibration.isChecked || vibrationEnableList.value != "vibrationTimeRange") {
                pcVibration.removePreference(tpEndVibration)
                pcVibration.removePreference(tpStartVibration)
            }
            vibrationEnableList.setOnPreferenceChangeListener { _, newValue ->
                val value = newValue.toString()
                if (value != "vibrationTimeRange") {
                    pcVibration.removePreference(tpEndVibration)
                    pcVibration.removePreference(tpStartVibration)
                } else {
                    pcVibration.addPreference(tpEndVibration)
                    pcVibration.addPreference(tpStartVibration)
                }
                true
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(tag, "got attached to ${context.packageName}")

        onCfgChangedCB = try {
            activity as OnConfigurationChangedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnConfigurationSavedListener")
        }
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        Log.d(tag, "onSharedPreferenceChanged: key $key")
        Log.d(tag, "onSharedPreferenceChanged: ${settings.all}")
        onCfgChangedCB?.onConfigurationChanged()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun savePreferences(key: String, value: String) {
        val activity = activity ?: return
        val myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        myPreferences.edit().putString(key, value).apply()
    }

    private fun restorePreferences(key: String): String {
        val activity = activity ?: return ""
        val myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        return if (myPreferences.contains(key)) myPreferences.getString(key, "") ?: "" else ""
    }

    companion object {
        private const val SHARED_PREFERENCES = "mychimeprefs"
    }
}
