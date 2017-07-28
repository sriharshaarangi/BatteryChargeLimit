package com.slash.batterychargelimit.settings;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.slash.batterychargelimit.R;

public class SettingsFragment extends PreferenceFragment {
    public static final String KEY_CONTROL_FILE = "control_file", KEY_TEMP_FAHRENHEIT = "temp_fahrenheit",
            KEY_IMMEDIATE_POWER_INTENT_HANDLING = "immediate_power_intent_handling",
            KEY_ENFORCE_CHARGE_LIMIT = "enforce_charge_limit";
    private static boolean visible = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.white));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        visible = true;
    }

    @Override
    public void onStop() {
        visible = false;
        super.onStop();
    }

    public static boolean settingsVisible() {
        return visible;
    }
}