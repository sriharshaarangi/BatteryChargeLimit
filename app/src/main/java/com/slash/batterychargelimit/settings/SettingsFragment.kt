package com.slash.batterychargelimit.settings

import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.SharedMethods
import com.slash.batterychargelimit.activities.CustomCtrlFileData

class SettingsFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        val customCtrlFileDataSwitch:SwitchPreference = findPreference("custom_ctrl_file_data") as SwitchPreference
        val ctrlFilePreference:ControlFilePreference = findPreference("control_file") as ControlFilePreference
        val ctrlFileSetupPreference:Preference = findPreference("custom_ctrl_file_setup") as Preference

        ctrlFilePreference.setOnPreferenceClickListener {
            val settings = view.context.getSharedPreferences(Constants.SETTINGS, 0)
            if (!settings.getBoolean("has_opened_ctrl_file", false)) {
                AlertDialog.Builder(view.context)
                        .setTitle(R.string.control_file_heads_up_title)
                        .setMessage(R.string.control_file_heads_up_desc)
                        .setCancelable(false)
                        .setPositiveButton(R.string.control_understand) { _, _ ->
                            settings.edit().putBoolean("has_opened_ctrl_file", true).apply()
                        }.create().show()
            }
            true
        }

        customCtrlFileDataSwitch.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                ctrlFilePreference.isEnabled = false
                ctrlFileSetupPreference.isEnabled = true
            } else {
                if (!newValue) {
                    ctrlFilePreference.isEnabled = true
                    ctrlFileSetupPreference.isEnabled = false
                }
            }
            true
        }

        ctrlFileSetupPreference.setOnPreferenceClickListener {
            AlertDialog.Builder(view.context)
                    .setTitle(R.string.control_file_alert_title)
                    .setMessage(R.string.control_file_alert_desc)
                    .setCancelable(false)
                    .setPositiveButton(R.string.control_understand) { _, _ ->
                        val CtrlFileIntent = Intent(view.context, CustomCtrlFileData::class.java)
                        startActivity(CtrlFileIntent)
                    }.create().show()
            true
        }

        if (customCtrlFileDataSwitch.isChecked) {
            ctrlFilePreference.isEnabled = false
            ctrlFileSetupPreference.isEnabled = true
        } else {
            ctrlFilePreference.isEnabled = true
            ctrlFileSetupPreference.isEnabled = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(activity, android.R.color.white))
        return view
    }

    override fun onStart() {
        super.onStart()
        visible = true
    }

    override fun onStop() {
        visible = false
        super.onStop()
    }

    override fun onDetach() {
        super.onDetach()
        val statusCTRLData = activity.findViewById(R.id.status_ctrl_data) as TextView
        statusCTRLData.text = SharedMethods.getCtrlFileData(activity) + ", " + SharedMethods.getCtrlEnabledData(activity) + ", " + SharedMethods.getCtrlDisabledData(activity)
    }

    companion object {
        const val KEY_CONTROL_FILE = "control_file"
        const val KEY_TEMP_FAHRENHEIT = "temp_fahrenheit"
        const val KEY_IMMEDIATE_POWER_INTENT_HANDLING = "immediate_power_intent_handling"
        const val KEY_ENFORCE_CHARGE_LIMIT = "enforce_charge_limit"
        const val KEY_ALWAYS_WRITE_CF = "always_write_cf"

        private var visible = false

        fun settingsVisible(): Boolean {
            return visible
        }
    }
}