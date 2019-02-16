package com.slash.batterychargelimit.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.widget.ListView

import androidx.annotation.AttrRes
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.activities.CustomCtrlFileDataActivity
import androidx.fragment.app.DialogFragment
import com.slash.batterychargelimit.activities.MainActivity

class PrefsFragment : PreferenceFragmentCompat() {

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var dialogFragment: DialogFragment? = null
        if (preference is ControlFilePreference) {
            dialogFragment = ControlFileDialogFragmentCompat.newInstance(preference.key)
        }

        if (dialogFragment != null) {
            val settings = view!!.context.getSharedPreferences(Constants.SETTINGS, 0)
            if (!settings.getBoolean("has_opened_ctrl_file", false)) {
                AlertDialog.Builder(view!!.context)
                        .setTitle(R.string.control_file_heads_up_title)
                        .setMessage(R.string.control_file_heads_up_desc)
                        .setCancelable(false)
                        .setPositiveButton(R.string.control_understand) { _, _ ->
                            settings.edit().putBoolean("has_opened_ctrl_file", true).apply()
                            dialogFragment.setTargetFragment(this, 0)
                            dialogFragment.show(this.fragmentManager!!, ControlFileDialogFragmentCompat::class.java.simpleName)
                        }.create().show()
            } else {
                dialogFragment.setTargetFragment(this, 0)
                dialogFragment.show(this.fragmentManager!!, ControlFileDialogFragmentCompat::class.java.simpleName)
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        setHasOptionsMenu(true)

        val theme: ListPreference = findPreference("theme") as ListPreference
        val customCtrlFileDataSwitch:SwitchPreference = findPreference("custom_ctrl_file_data") as SwitchPreference
        val ctrlFilePreference:ControlFilePreference = findPreference("control_file") as ControlFilePreference
        val ctrlFileSetupPreference:Preference = findPreference("custom_ctrl_file_setup") as Preference

        theme.setOnPreferenceChangeListener { preference, newValue ->
            if (preference is ListPreference) {
                activity!!.recreate()
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
            AlertDialog.Builder(view!!.context)
                    .setTitle(R.string.control_file_alert_title)
                    .setMessage(R.string.control_file_alert_desc)
                    .setCancelable(false)
                    .setPositiveButton(R.string.control_understand) { _, _ ->
                        val CtrlFileIntent = Intent(view!!.context, CustomCtrlFileDataActivity::class.java)
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view!!.setBackgroundColor(view.context.getColorFromAttr(R.attr.cardColor))
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
        (activity as MainActivity).setStatusCTRLFileData()
    }

    companion object {
        const val KEY_CONTROL_FILE = "control_file"
        const val KEY_TEMP_FAHRENHEIT = "temp_fahrenheit"
        const val KEY_IMMEDIATE_POWER_INTENT_HANDLING = "immediate_power_intent_handling"
        const val KEY_NOTIFICATION_SOUND = "notification_sound"
        const val KEY_AUTO_RESET_STATS = "auto_reset_stats"
        const val KEY_ENFORCE_CHARGE_LIMIT = "enforce_charge_limit"
        const val KEY_ALWAYS_WRITE_CF = "always_write_cf"
        const val KEY_DISABLE_AUTO_RECHARGE = "disable_auto_recharge"
        const val KEY_THEME = "theme"

        private var visible = false

        fun settingsVisible(): Boolean {
            return visible
        }
    }
}