package com.vag.mychime.preferences;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.vag.mychime.R;

import java.util.Calendar;

public class IntervalPickerDialogFragment extends PreferenceDialogFragmentCompat implements Preference.OnPreferenceChangeListener {
    private final String TAG = "IntervalPickerDialogFragment";

    private int intervalSize = 60;
    private String intervalUnit = "Seconds";
    private IntervalPickerDialogFragment picker = null;
    private String intervalDefinition;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        // Use the current time as the default values for the picker
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it
        picker = new IntervalPickerDialogFragment(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));

        return picker;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Log.d(TAG, "onDialogClosed");
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        return false;
    }
}
