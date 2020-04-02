package com.slash.batterychargelimit.activities

import android.content.*
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.Constants.AUTO_RESET_STATS
import com.slash.batterychargelimit.Constants.CHARGE_LIMIT_ENABLED
import com.slash.batterychargelimit.Constants.DISABLE_CHARGE_NOW
import com.slash.batterychargelimit.Constants.LIMIT
import com.slash.batterychargelimit.Constants.LIMIT_BY_VOLTAGE
import com.slash.batterychargelimit.Constants.MIN
import com.slash.batterychargelimit.Constants.CHARGE_BELOW_LOWER_LIMIT_ONLY
import com.slash.batterychargelimit.Constants.NOTIFICATION_SOUND
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.Constants.SETTINGS_VERSION
import com.slash.batterychargelimit.ForegroundService
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.Utils
import com.slash.batterychargelimit.fragments.AboutFragment
import com.slash.batterychargelimit.receivers.EnableWidgetIntentReceiver
import com.slash.batterychargelimit.settings.CtrlFileHelper
import com.slash.batterychargelimit.settings.PrefsFragment
import eu.chainfire.libsuperuser.Shell
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {
    private val minPicker by lazy(LazyThreadSafetyMode.NONE) { findViewById<NumberPicker>(R.id.min_picker) }
    private val minText by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.min_text) }
    private val maxPicker by lazy(LazyThreadSafetyMode.NONE) { findViewById<NumberPicker>(R.id.max_picker) }
    private val maxText by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.max_text) }
    private val settings by lazy(LazyThreadSafetyMode.NONE) {getSharedPreferences(SETTINGS, 0)}
    private val statusText by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.status) }
    private val batteryInfo by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.battery_info) }
    private val enableSwitch by lazy(LazyThreadSafetyMode.NONE) { findViewById<Switch>(R.id.enable_switch) }
    private val disableChargeSwitch by lazy(LazyThreadSafetyMode.NONE) { findViewById<Switch>(R.id.disable_charge_switch) }
    private val limitByVoltageSwitch by lazy(LazyThreadSafetyMode.NONE) { findViewById<Switch>(R.id.limit_by_voltage) }
    private val customThresholdEditView by lazy(LazyThreadSafetyMode.NONE) { findViewById<EditText>(R.id.voltage_threshold) }
    private val currentThresholdTextView by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.current_voltage_threshold) }
    private val defaultThresholdTextView by lazy(LazyThreadSafetyMode.NONE) { findViewById<TextView>(R.id.default_voltage_threshold) }
    private var initComplete = false
    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private lateinit var currentThreshold : String
    private val mHandler = MainHandler(this)
    private lateinit var prefs: SharedPreferences

    private class MainHandler(activity:MainActivity):Handler() {
        private val mActivity by lazy(LazyThreadSafetyMode.NONE) {WeakReference(activity)}
        override fun handleMessage(msg:Message) {
            val activity = mActivity.get()
            if (activity != null) {
                when(msg.what) {
                    MSG_UPDATE_VOLTAGE_THRESHOLD -> {
                        val voltage = msg.data.getString(VOLTAGE_THRESHOLD)
                        activity.currentThreshold = voltage!!
                        activity.currentThresholdTextView.text = voltage
                        if (activity.settings.getString(Constants.DEFAULT_VOLTAGE_LIMIT, null) == null) {
                            activity.settings.edit().putString(Constants.DEFAULT_VOLTAGE_LIMIT, voltage).apply()
                            activity.defaultThresholdTextView.text = voltage
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Utils.getPrefs(this)
        Utils.setTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Exit immediately if no root support
        if (!Shell.SU.available()) {
            showNoRootDialog()
            return
        }
        Utils.refreshSu()
        updateSettingsVersion()
        checkForControlFiles()
        whitelistIfFirstStart()
        loadUi()
        //The onCreate() process was not stopped via return, UI elements should be available
        initComplete = true
    }

    private fun showNoRootDialog() {
        Toast.makeText(this, R.string.root_denied, Toast.LENGTH_SHORT)
        AlertDialog.Builder(this@MainActivity)
                .setMessage(R.string.root_denied)
                .setCancelable(false)
                .setPositiveButton(R.string.ok) { _, _ -> finish() }.create().show()
    }

    private fun checkForControlFiles() {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        if (!prefs.contains(PrefsFragment.KEY_CONTROL_FILE)) {
            CtrlFileHelper.validateFiles(this, Runnable {
                var found = false
                for (cf in Utils.getCtrlFiles(this@MainActivity)) {
                    if (cf.isValid) {
                        Utils.setCtrlFile(this@MainActivity, cf)
                        found = true
                        break
                    }
                }
                if (!found) {
                    AlertDialog.Builder(this@MainActivity)
                            .setMessage(R.string.device_not_supported)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok) { _, _ -> finish() }.create().show()
                }
            })
        }
    }

    private fun updateSettingsVersion() {
        val settingsVersion = prefs.getInt(SETTINGS_VERSION, 0)
        var versionCode = 0
        try {
            @Suppress("DEPRECATION")
            versionCode = packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.wtf(TAG, e)
        }

        if (settingsVersion < versionCode) {
            when (settingsVersion) {
                0, 1, 2, 3, 4 -> {
                    if (settings.contains("limit_reached")) {
                        settings.edit().remove("limit_reached").apply()
                    }
                    if (settings.contains("recharge_threshold")) {
                        val limit = settings.getInt(LIMIT, Constants.DEFAULT_LIMIT_PC)
                        val diff = settings.getInt("recharge_threshold", limit - 2)
                        settings.edit().putInt(MIN, limit - diff).remove("recharge_threshold").apply()
                    }
                }
                5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> if (settings.contains("recharge_threshold")) {
                    val limit = settings.getInt(LIMIT, Constants.DEFAULT_LIMIT_PC)
                    val diff = settings.getInt("recharge_threshold", limit - 2)
                    settings.edit().putInt(MIN, limit - diff).remove("recharge_threshold").apply()
                }
                16, 17, 18, 19, 20, 21, 22, 23 -> {
                    if (settings.contains(NOTIFICATION_SOUND)) {
                        val notificatonSound = settings.getBoolean(NOTIFICATION_SOUND, false)
                        prefs.edit().putBoolean(PrefsFragment.KEY_NOTIFICATION_SOUND, notificatonSound).apply()
                        settings.edit().remove(NOTIFICATION_SOUND).apply()
                    }
                    if (settings.contains(AUTO_RESET_STATS)) {
                        val autoResetStats = settings.getBoolean(AUTO_RESET_STATS, false)
                        prefs.edit().putBoolean(PrefsFragment.KEY_AUTO_RESET_STATS, autoResetStats).apply()
                    }
                }
            }// settings upgrade for future version(s)
            // update the settings version
            prefs.edit().putInt(SETTINGS_VERSION, versionCode).apply()
        }
    }

    private fun whitelistIfFirstStart() {
        if (!prefs.getBoolean(getString(R.string.previously_started), false)) {
            // whitelist App for Doze Mode
            Utils.suShell.addCommand("dumpsys deviceidle whitelist +com.slash.batterychargelimit",
                    0) { _, _, _ ->
                prefs.edit().putBoolean(getString(R.string.previously_started), true).apply()
            }
        }
    }

    private fun loadUi() {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                PrefsFragment.KEY_TEMP_FAHRENHEIT -> updateBatteryInfo(baseContext.registerReceiver(null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!)
            }
        }

        customThresholdEditView.setOnEditorActionListener { _, actionId, _ ->
            var handled = false
            if (actionId == EditorInfo.IME_ACTION_GO) {
                hideKeybord()
                customThresholdEditView.clearFocus()
                handled = true
            }
            handled
        }

        Utils.getCurrentVoltageThresholdAsync(this@MainActivity, mHandler)

        currentThreshold = settings.getString(Constants.DEFAULT_VOLTAGE_LIMIT, "4300")!!

        customThresholdEditView.setText(settings.getString(Constants.CUSTOM_VOLTAGE_LIMIT, "NA"))
        defaultThresholdTextView.text = settings.getString(Constants.DEFAULT_VOLTAGE_LIMIT, "NA")

        customThresholdEditView.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newThreshold = customThresholdEditView.text.toString()
                if (Utils.isValidVoltageThreshold(newThreshold, currentThreshold)) {
                    settings.edit().putString(Constants.CUSTOM_VOLTAGE_LIMIT, newThreshold).apply()
                    Utils.setVoltageThreshold(null, true, this@MainActivity , mHandler)
                }
            }
        }

        val isChargeLimitEnabled = settings.getBoolean(CHARGE_LIMIT_ENABLED, false)

        if (isChargeLimitEnabled && Utils.isPhonePluggedIn(this)) {
            this.startService(Intent(this, ForegroundService::class.java))
        }

        val resetBatteryStatsButton = findViewById<Button>(R.id.reset_battery_stats)
//        val autoResetSwitch = findViewById(R.id.auto_stats_reset) as CheckBox
//        val notificationSound = findViewById(R.id.notification_sound) as CheckBox
        var chargeBelowLowerLimitOnly = findViewById(R.id.charge_below_lower_limit) as Switch

//        autoResetSwitch.isChecked = settings.getBoolean(AUTO_RESET_STATS, false)
//        notificationSound.isChecked = settings.getBoolean(NOTIFICATION_SOUND, false)
        chargeBelowLowerLimitOnly.isChecked = settings.getBoolean(CHARGE_BELOW_LOWER_LIMIT_ONLY, false)
        maxPicker.minValue = Constants.MIN_ALLOWED_LIMIT_PC
        maxPicker.maxValue = Constants.MAX_ALLOWED_LIMIT_PC
        minPicker.minValue = 0

        enableSwitch.setOnCheckedChangeListener(switchListener)
        disableChargeSwitch.setOnCheckedChangeListener(switchListener)
        limitByVoltageSwitch.setOnCheckedChangeListener(switchListener)
        maxPicker.setOnValueChangedListener { _, _, max ->
            Utils.setLimit(max, settings)
            maxText.text = getString(R.string.limit, max)
            val min = settings.getInt(MIN, max - 2)
            minPicker.maxValue = max
            minPicker.value = min
            updateMinText(min)
            if (!ForegroundService.isRunning) {
                Utils.startServiceIfLimitEnabled(this)
            }
        }

        minPicker.setOnValueChangedListener { _, _, min ->
            settings.edit().putInt(MIN, min).apply()
            updateMinText(min)
        }
        resetBatteryStatsButton.setOnClickListener { Utils.resetBatteryStats(this@MainActivity) }
//        autoResetSwitch.setOnCheckedChangeListener { _, isChecked ->
//            settings.edit().putBoolean(AUTO_RESET_STATS, isChecked).apply() }
//        notificationSound.setOnCheckedChangeListener { _, isChecked ->
//            settings.edit().putBoolean(NOTIFICATION_SOUND, isChecked).apply() }
        chargeBelowLowerLimitOnly.setOnCheckedChangeListener { _, isChecked ->
            settings.edit().putBoolean(CHARGE_BELOW_LOWER_LIMIT_ONLY, isChecked).apply() }


        setStatusCTRLFileData()
    }

    private fun hideKeybord () {
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (inputManager.isAcceptingText) {
            inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    //OnCheckedChangeListener for Switch elements
    private val switchListener = object : CompoundButton.OnCheckedChangeListener {
        val context: Context = this@MainActivity
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            when(buttonView.id) {
                R.id.enable_switch -> {
                    settings.edit().putBoolean(CHARGE_LIMIT_ENABLED, isChecked).apply()
                    if (isChecked) {
                        Utils.startServiceIfLimitEnabled(context)
                        disableSwitches(listOf(disableChargeSwitch, limitByVoltageSwitch))
                    } else {
                        Utils.stopService(context)
                        enableSwitches(listOf(disableChargeSwitch, limitByVoltageSwitch))
                    }
                    EnableWidgetIntentReceiver.updateWidget(context, isChecked)
                }
                R.id.disable_charge_switch -> {
                    if (isChecked) {
                        Utils.changeState(this@MainActivity, Utils.CHARGE_OFF)
                        settings.edit().putBoolean(DISABLE_CHARGE_NOW, true).apply()
                        disableSwitches(listOf(enableSwitch, limitByVoltageSwitch))
                    } else {
                        Utils.changeState(this@MainActivity, Utils.CHARGE_ON)
                        settings.edit().putBoolean(DISABLE_CHARGE_NOW, false).apply()
                        enableSwitches(listOf(enableSwitch, limitByVoltageSwitch))
                    }
                }
                R.id.limit_by_voltage -> {
                    if (isChecked) {
                        Utils.setVoltageThreshold(settings.getString(Constants.CUSTOM_VOLTAGE_LIMIT, Constants.DEFAULT_VOLTAGE_THRESHOLD_MV),
                                false, this@MainActivity, mHandler)
                        settings.edit().putBoolean(LIMIT_BY_VOLTAGE, true).apply()
                        disableSwitches(listOf(enableSwitch, disableChargeSwitch))
                    } else {
                        Utils.setVoltageThreshold(settings.getString(Constants.DEFAULT_VOLTAGE_LIMIT, "4300"),
                                false, this@MainActivity, mHandler)
                        settings.edit().putBoolean(LIMIT_BY_VOLTAGE, false).apply()
                        enableSwitches(listOf(enableSwitch, disableChargeSwitch))
                    }
                }
            }
        }
    }

    fun disableSwitches(switches: List<Switch>) {
        for (switch in switches) {
            switch.isEnabled = false
            switch.setTextColor(getColorFromAttr(R.attr.secondaryText, this))
        }
    }

    fun enableSwitches(switches: List<Switch>) {
        for (switch in switches) {
            switch.isEnabled = true
            switch.setTextColor(getColorFromAttr(R.attr.primaryText, this))
        }
    }

    private fun getColorFromAttr(attr: Int, context: Context): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    //to update battery status on UI
    private val charging = object : BroadcastReceiver() {
        private var previousStatus = BatteryManager.BATTERY_STATUS_UNKNOWN

        override fun onReceive(context: Context, intent: Intent) {
            val currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            if (currentStatus != previousStatus) {
                previousStatus = currentStatus
                when (currentStatus) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> {
                        statusText.setText(R.string.charging)
                        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkGreen))
                    }
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> {
                        statusText.setText(R.string.discharging)
                        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange))
                    }
                    BatteryManager.BATTERY_STATUS_FULL -> {
                        statusText.setText(R.string.full)
                        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.darkGreen))
                    }
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> {
                        statusText.setText(R.string.not_charging)
                        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.orange))
                    }
                    else -> {
                        statusText.setText(R.string.unknown)
                        statusText.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red))
                    }
                }
            }
            updateBatteryInfo(intent)
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        batteryInfo.text = String.format(" (%s)", Utils.getBatteryInfo(this, intent,
                prefs.getBoolean(PrefsFragment.KEY_TEMP_FAHRENHEIT, false)))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> if (!AboutFragment.aboutVisible()) {
                supportActionBar!!.title = getString(R.string.about)
                supportFragmentManager.popBackStack()
                supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AboutFragment())
                        .addToBackStack(null).commit()
            }
            R.id.action_settings -> if (!PrefsFragment.settingsVisible()) {
                supportActionBar!!.title = getString(R.string.action_settings)
                CtrlFileHelper.validateFiles(this, Runnable {
                    supportFragmentManager.popBackStack()
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, PrefsFragment())
                            .addToBackStack(null).commit()
                })
            }
        }
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportActionBar!!.title = getString(R.string.app_name)
    }

    public override fun onStop() {
        if (initComplete) {
            unregisterReceiver(charging)
        }
        super.onStop()
    }

    public override fun onStart() {
        super.onStart()
        if (initComplete) {
            registerReceiver(charging, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            // the limits could have been changed by an Intent, so update the UI here
            updateUi()
        }
    }

    override fun onDestroy() {
        Utils.getPrefs(baseContext)
                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        // technically not necessary, but it prevents inlining of this required field
        // see end of https://developer.android.com/guide/topics/ui/settings.html#Listening
        preferenceChangeListener = null
        super.onDestroy()
    }

    private fun updateMinText(min: Int) {
        when (min) {
            0 -> minText.setText(R.string.no_recharge)
            else -> minText.text = getString(R.string.recharge_below, min)
        }
    }

    private fun updateUi() {
        enableSwitch.isChecked = settings.getBoolean(CHARGE_LIMIT_ENABLED, false)
        disableChargeSwitch.isChecked = settings.getBoolean(DISABLE_CHARGE_NOW, false)
        limitByVoltageSwitch.isChecked = settings.getBoolean(LIMIT_BY_VOLTAGE, false)
        val max = settings.getInt(LIMIT, 80)
        val min = settings.getInt(MIN, max - 2)
        maxPicker.value = max
        maxText.text = getString(R.string.limit, max)
        minPicker.maxValue = max
        minPicker.value = min
        updateMinText(min)
    }

    fun setStatusCTRLFileData() {
        val statusCTRLData = findViewById<TextView>(R.id.status_ctrl_data)
        statusCTRLData.text = String.format("%s, %s, %s",
                Utils.getCtrlFileData(this),
                Utils.getCtrlEnabledData(this),
                Utils.getCtrlDisabledData(this)
        )
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        const val MSG_UPDATE_VOLTAGE_THRESHOLD = 1
        const val VOLTAGE_THRESHOLD = "voltageThreshold"
    }
}
