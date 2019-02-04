package com.slash.batterychargelimit.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.support.annotation.AttrRes
import android.support.v7.app.AlertDialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.Utils
import com.slash.batterychargelimit.activities.CustomCtrlFileData

class SettingsFragment : PreferenceFragment() {

    private lateinit var menu: Menu
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)

        setHasOptionsMenu(true)

        val darkTheme:SwitchPreference = findPreference("dark_theme") as SwitchPreference
        val customCtrlFileDataSwitch:SwitchPreference = findPreference("custom_ctrl_file_data") as SwitchPreference
        val ctrlFilePreference:ControlFilePreference = findPreference("control_file") as ControlFilePreference
        val ctrlFileSetupPreference:Preference = findPreference("custom_ctrl_file_setup") as Preference

        darkTheme.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                activity.setTheme(R.style.AppTheme_Dark_NoActionBar)
            }
            else {
                activity.setTheme(R.style.AppTheme_Light_NoActionBar)
            }
            activity.recreate()
            true
        }

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
        view.setBackgroundColor(view.context.getColorFromAttr(R.attr.card_color))
        return view
    }

    fun Context.getColorFromAttr(
            @AttrRes attrColor: Int,
            typedValue: TypedValue = TypedValue(),
            resolveRefs: Boolean = true
    ): Int {
        theme.resolveAttribute(attrColor, typedValue, resolveRefs)
        return typedValue.data
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.action_settings)
        item.isVisible = false
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
        statusCTRLData.text = Utils.getCtrlFileData(activity) + ", " + Utils.getCtrlEnabledData(activity) + ", " + Utils.getCtrlDisabledData(activity)
    }

    companion object {
        const val KEY_CONTROL_FILE = "control_file"
        const val KEY_TEMP_FAHRENHEIT = "temp_fahrenheit"
        const val KEY_IMMEDIATE_POWER_INTENT_HANDLING = "immediate_power_intent_handling"
        const val KEY_ENFORCE_CHARGE_LIMIT = "enforce_charge_limit"
        const val KEY_ALWAYS_WRITE_CF = "always_write_cf"
        const val KEY_ENABLE_AUTO_RECHARGE = "enable_auto_recharge"

        private var visible = false

        fun settingsVisible(): Boolean {
            return visible
        }
    }
}