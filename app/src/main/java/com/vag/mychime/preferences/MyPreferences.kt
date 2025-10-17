package com.vag.mychime.preferences

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.preference.ListPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.vag.mychime.R

class MyPreferences : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    interface OnConfigurationChangedListener {
        fun onConfigurationChanged()
    }

    private var onCfgChangedCB: OnConfigurationChangedListener? = null
    private lateinit var settings: SharedPreferences

    private val tag = "MyPreferences"
    private var hasVibration = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        Log.d(tag, "onCreatePreferences rootKey: $rootKey")

        settings = PreferenceManager.getDefaultSharedPreferences(context)
        hasVibration = settings.getBoolean("hasVibration", false)
        Log.d(tag, "onCreatePreferences ${settings.all}")
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val speakCategory = findPreferenceCategory("speak")
        val chimeCategory = findPreferenceCategory("chime")
        val vibrationCategory = findPreferenceCategory("vibration")
        val enableSpeak = findSwitchPreference("enableSpeak")
        val enableChime = findSwitchPreference("enableChime")
        val enableVibration = findSwitchPreference("enableVibration")
        val speakStart = findTimePreference("speakStartTime")
        val speakEnd = findTimePreference("speakEndTime")
        val chimeStart = findTimePreference("chimeStartTime")
        val chimeEnd = findTimePreference("chimeEndTime")
        val vibrationStart = findTimePreference("vibrationStartTime")
        val vibrationEnd = findTimePreference("vibrationEndTime")

        enableSpeak.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue.toString().toBoolean()
            if (!enabled) {
                speakCategory.removePreference(speakEnd)
                speakCategory.removePreference(speakStart)
            }
            true
        }

        val speakEnableList = findListPreference("speakOn")
        if (speakEnableList.value == null) {
            speakEnableList.setValueIndex(0)
        }
        if (!enableSpeak.isChecked || speakEnableList.value != "speakTimeRange") {
            speakCategory.removePreference(speakEnd)
            speakCategory.removePreference(speakStart)
        }
        speakEnableList.setOnPreferenceChangeListener { _, newValue ->
            if (newValue != "speakTimeRange") {
                speakCategory.removePreference(speakEnd)
                speakCategory.removePreference(speakStart)
            } else {
                speakCategory.addPreference(speakEnd)
                speakCategory.addPreference(speakStart)
            }
            true
        }

        enableChime.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue.toString().toBoolean()
            if (!enabled) {
                chimeCategory.removePreference(chimeEnd)
                chimeCategory.removePreference(chimeStart)
            }
            true
        }

        val chimeEnableList = findListPreference("chimeOn")
        if (chimeEnableList.value == null) {
            chimeEnableList.setValueIndex(0)
        }
        if (!enableChime.isChecked || chimeEnableList.value != "chimeTimeRange") {
            chimeCategory.removePreference(chimeEnd)
            chimeCategory.removePreference(chimeStart)
        }
        chimeEnableList.setOnPreferenceChangeListener { _, newValue ->
            if (newValue != "chimeTimeRange") {
                chimeCategory.removePreference(chimeEnd)
                chimeCategory.removePreference(chimeStart)
            } else {
                chimeCategory.addPreference(chimeEnd)
                chimeCategory.addPreference(chimeStart)
            }
            true
        }

        val clockTypeList = findListPreference("clockType")
        if (clockTypeList.value == null) {
            clockTypeList.setValueIndex(0)
        }
        clockTypeList.summary = clockTypeList.value
        clockTypeList.setOnPreferenceChangeListener { preference, newValue ->
            preference.summary = newValue.toString()
            true
        }

        if (!hasVibration) {
            Log.d(tag, "onCreatePreferences: device doesn't support vibration. Disabling controls")
            vibrationCategory.setTitle(R.string.vibrationCpt)
            vibrationCategory.setSummary(R.string.vibrationNotSupportedCpt)
            vibrationCategory.removeAll()
        } else {
            val vibrationEnableList = findListPreference("vibrationOn")
            if (vibrationEnableList.value == null) {
                vibrationEnableList.setValueIndex(0)
            }
            if (!enableVibration.isChecked || vibrationEnableList.value != "vibrationTimeRange") {
                vibrationCategory.removePreference(vibrationEnd)
                vibrationCategory.removePreference(vibrationStart)
            }
            vibrationEnableList.setOnPreferenceChangeListener { _, newValue ->
                if (newValue != "vibrationTimeRange") {
                    vibrationCategory.removePreference(vibrationEnd)
                    vibrationCategory.removePreference(vibrationStart)
                } else {
                    vibrationCategory.addPreference(vibrationEnd)
                    vibrationCategory.addPreference(vibrationStart)
                }
                true
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(tag, "got attached to ${context.packageName}")

        onCfgChangedCB = activity as? OnConfigurationChangedListener
            ?: throw ClassCastException("$activity must implement OnConfigurationChangedListener")
    }

    override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String) {
        Log.d(tag, "onSharedPreferenceChanged: key $key")
        Log.d(tag, "onSharedPreferenceChanged: ${settings.all}")
        onCfgChangedCB?.onConfigurationChanged()
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun findPreferenceCategory(key: String): PreferenceCategory =
        requireNotNull(findPreference<PreferenceCategory>(key))

    private fun findSwitchPreference(key: String): SwitchPreferenceCompat =
        requireNotNull(findPreference<SwitchPreferenceCompat>(key))

    private fun findListPreference(key: String): ListPreference =
        requireNotNull(findPreference<ListPreference>(key))

    private fun findTimePreference(key: String): TimePickerPreference =
        requireNotNull(findPreference<TimePickerPreference>(key))

    private fun savePreferences(key: String, value: String) {
        val activity = activity ?: return
        val myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        myPreferences.edit().putString(key, value).apply()
    }

    private fun restorePreferences(key: String): String {
        val activity = activity ?: return ""
        val myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
        return myPreferences.getString(key, "") ?: ""
    }

    companion object {
        private const val SHARED_PREFERENCES = "mychimeprefs"
    }
}
