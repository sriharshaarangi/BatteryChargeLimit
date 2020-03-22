package com.slash.batterychargelimit

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.slash.batterychargelimit.Constants.CHARGE_LIMIT_ENABLED
import com.slash.batterychargelimit.Constants.CHARGE_OFF_KEY
import com.slash.batterychargelimit.Constants.CHARGE_ON_KEY
import com.slash.batterychargelimit.Constants.CHARGING_CHANGE_TOLERANCE_MS
import com.slash.batterychargelimit.Constants.DEFAULT_DISABLED
import com.slash.batterychargelimit.Constants.DEFAULT_ENABLED
import com.slash.batterychargelimit.Constants.DEFAULT_FILE
import com.slash.batterychargelimit.Constants.FILE_KEY
import com.slash.batterychargelimit.Constants.LIMIT
import com.slash.batterychargelimit.Constants.MIN
import com.slash.batterychargelimit.Constants.NOTIFICATION_LIVE
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.activities.MainActivity
import com.slash.batterychargelimit.settings.PrefsFragment
import eu.chainfire.libsuperuser.Shell
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object Utils {
    private val TAG = Utils::class.java.simpleName
    const val CHARGE_ON = 0
    const val CHARGE_OFF = 1

    // remember pending state change
    private var changePending: Long = 0

    // remember initialization
    private var cfInitialized: Boolean = false

    /**
     * Inform the BatteryReceiver instance(es) to ignore events for CHARGING_CHANGE_TOLERANCE_MS,
     * in order to let the state change settle.
     */
    private fun setChangePending() {
        // update changePending to prevent concurrent state changes before execution
        changePending = System.currentTimeMillis()
    }

    /**
     * Returns whether some change happened at most CHARGING_CHANGE_TOLERANCE_MS ago.
     *
     * @return Whether state change is pending
     */
    fun isChangePending(tolerance: Long): Boolean {
        return System.currentTimeMillis() <= changePending + tolerance
    }

    var suShell: Shell.Interactive = Shell.Builder().setWantSTDERR(false).useSU().open()

    fun refreshSu() {
        suShell = Shell.Builder().setWantSTDERR(false).useSU().open()
    }

    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun changeState(context: Context, chargeMode: Int) {
        val preferences = getPrefs(context)
        val alwaysWrite = preferences.getBoolean(PrefsFragment.KEY_ALWAYS_WRITE_CF, false)

        val file = getCtrlFileData(context)
        val newState = if (chargeMode == CHARGE_ON) {
            getCtrlEnabledData(context)
        } else {
            getCtrlDisabledData(context)
        }

        val switchCommands: Array<String>
        if (cfInitialized) {
            switchCommands = arrayOf("echo \"$newState\" > $file")
        } else {
            cfInitialized = true
            switchCommands = arrayOf("mount -o rw,remount $file", "chmod u+w $file",
                    "echo \"$newState\" > $file")
        }

        if (alwaysWrite) {
            suShell.addCommand(switchCommands)
        } else {
            suShell.addCommand("cat $file", 0) { _, _, output ->
                if (output.size == 0 || output[0] != newState) {
                    setChangePending()
                    suShell.addCommand(switchCommands)
                }
            }
        }
    }

    private var ctrlFiles: List<ControlFile>? = null
    fun getCtrlFiles(context: Context): List<ControlFile> {
        if (ctrlFiles == null) {
            try {
                val r = InputStreamReader(context.resources.openRawResource(R.raw.control_files),
                        Charset.forName("UTF-8"))
                val type = object : TypeToken<List<ControlFile>>() {}.type
                ctrlFiles = Gson().fromJson<List<ControlFile>>(r, type)!!.sortedWith(compareBy(
                        { it.order }, { it.issues }, { it.experimental }, { it.file }))
            } catch (e: Exception) {
                Log.wtf(context.javaClass.simpleName, e)
                return emptyList()
            }
        }
        return ctrlFiles!!
    }

    fun validateCtrlFiles(context: Context) {
        for (cf in getCtrlFiles(context)) {
            cf.validate()
        }
    }

    fun setCtrlFile(context: Context, cf: ControlFile) {
        //This will immediately reset the current control file
        stopService(context)
        getPrefs(context)
                .edit().putString(PrefsFragment.KEY_CONTROL_FILE, cf.file).apply()
        getSettings(context)
                .edit().putString(FILE_KEY, cf.file)
                .putString(CHARGE_ON_KEY, cf.chargeOn)
                .putString(CHARGE_OFF_KEY, cf.chargeOff).apply()
        //Respawn the service if necessary
        startServiceIfLimitEnabled(context)
    }

    fun isPhonePluggedIn(context: Context): Boolean {
        val batteryIntent = context.applicationContext.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED))!!
        return (batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                == BatteryManager.BATTERY_STATUS_CHARGING
                || batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0)
    }

    fun getBatteryLevel(batteryIntent: Intent): Int {
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        return if (level == -1 || scale == -1) {
            50
        } else {
            level * 100 / scale
        }
    }

    fun getBatteryInfo(context: Context, intent: Intent, useFahrenheit: Boolean): String {
        val batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        return context.getString(if (useFahrenheit) R.string.battery_info_F else R.string.battery_info_C,
                batteryVoltage.toFloat() / 1000f,
                if (useFahrenheit) 32f + batteryTemperature * 1.8f / 10f else batteryTemperature / 10f)
    }

//    @SuppressLint("PrivateApi")
    fun resetBatteryStats(context: Context) {
//        try {
//            // new technique for PureNexus-powered devices
//            val helperClass = Class.forName("com.android.internal.os.BatteryStatsHelper")
//            val constructor = helperClass.getConstructor(Context::class.java,
//                    Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
//            val instance = constructor.newInstance(context, false, false)
//            val createMethod = helperClass.getMethod("create", Bundle::class.javaPrimitiveType)
//            createMethod.invoke(instance, null)
//            val resetMethod = helperClass.getMethod("resetStatistics")
//            resetMethod.invoke(instance)
//            Toast.makeText(context, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
//        } catch (e: Exception) {
//            Log.i("New reset method failed", e.message, e)
            // on Exception, fall back to conventional method
            suShell.addCommand("dumpsys batterystats --reset", 0) { _, exitCode, _ ->
                if (exitCode == 0) {
                    Toast.makeText(context, R.string.stats_reset_success, Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "Statistics reset failed")
                }
            }
//        }
    }

    fun setLimit(limit: Int, settings: SharedPreferences) {
        val max = settings.getInt(LIMIT, Constants.DEFAULT_LIMIT_PC)
        // calculate new recharge threshold from previous distance
        val min = (limit - (max - settings.getInt(MIN, max - 2))).coerceAtLeast(0)
        settings.edit().putInt(LIMIT, limit).putInt(MIN, min).apply()
    }

    fun handleLimitChange(context: Context, newLimit: Any?) {
        try {
            if (newLimit == null) {
                throw NumberFormatException("null")
            }
            val limit = if (newLimit is Number) {
                newLimit.toInt()
            } else {
                Integer.parseInt(newLimit.toString())
            }
            if (limit == Constants.MAX_ALLOWED_LIMIT_PC) {
                val settings = getSettings(context)
                stopService(context)
                settings.edit().putBoolean(CHARGE_LIMIT_ENABLED, false).apply()
            } else if (limit in Constants.MIN_ALLOWED_LIMIT_PC until Constants.MAX_ALLOWED_LIMIT_PC) {
                val settings = getSettings(context)
                // set the new limit
                setLimit(limit, settings)
                Toast.makeText(context, context.getString(R.string.intent_limit_accepted, limit),
                        Toast.LENGTH_SHORT).show()
                if (!settings.getBoolean(NOTIFICATION_LIVE, false)) {
                    settings.edit().putBoolean(CHARGE_LIMIT_ENABLED, true).apply()
                    startServiceIfLimitEnabled(context)
                }
            } else {
                throw NumberFormatException("Battery limit out of range!")
            }
        } catch (fe: NumberFormatException) {
            Toast.makeText(context, R.string.intent_limit_invalid, Toast.LENGTH_SHORT).show()
        }

    }

    fun startServiceIfLimitEnabled(context: Context) {
        if (getSettings(context).getBoolean(CHARGE_LIMIT_ENABLED, false)) {
            if (getPrefs(context).getBoolean(PrefsFragment.KEY_DISABLE_AUTO_RECHARGE, false)) {
                changeState(context, CHARGE_ON)
            }
            Handler().postDelayed({
                if (isPhonePluggedIn(context)) {
                    context.startService(Intent(context, ForegroundService::class.java))
                    // display service enabled Toast message if not disabled in settings
                    if (!getPrefs(context).getBoolean("hide_toast_on_service_changes", false)) {
                        Toast.makeText(context, R.string.service_enabled, Toast.LENGTH_SHORT).show()
                    }
                }
            }, CHARGING_CHANGE_TOLERANCE_MS)
        }
    }

    fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun getSettings(context: Context): SharedPreferences {
        return context.getSharedPreferences(SETTINGS, 0)
    }

    fun stopService(context: Context, ignoreAutoReset: Boolean = true) {
        val wasServiceRunning = ForegroundService.isRunning
        if (ignoreAutoReset) {
            ForegroundService.ignoreAutoReset()
        }
        context.stopService(Intent(context, ForegroundService::class.java))
        if(!getPrefs(context).getBoolean(PrefsFragment.KEY_DISABLE_AUTO_RECHARGE, false)) {
            changeState(context, CHARGE_ON)
        }
        // display service disabled Toast message if not disabled in settings
        if (wasServiceRunning && !getPrefs(context).getBoolean("hide_toast_on_service_changes", false)) {
            Toast.makeText(context, R.string.service_disabled, Toast.LENGTH_SHORT).show()
        }
    }

    fun getCtrlFileData (context: Context): String? {
        val settings = getSettings(context)
        val preferences = getPrefs(context)

        return if (preferences.getBoolean("custom_ctrl_file_data", false)) {
            // Custom Data Enabled
            settings.getString(Constants.SAVED_PATH_DATA, DEFAULT_FILE)!!
        } else {
            // Custom Data Disabled
            settings.getString(FILE_KEY, DEFAULT_FILE)!!
        }
    }

    fun getCtrlEnabledData(context: Context) : String {
        val settings = getSettings(context)
        val preferences = getPrefs(context)

        return if (preferences.getBoolean("custom_ctrl_file_data", false)) {
            // Custom Data Enabled
            settings.getString(Constants.SAVED_ENABLED_DATA, DEFAULT_ENABLED)!!
        } else {
            // Custom Data Disabled
            settings.getString(CHARGE_ON_KEY, DEFAULT_ENABLED)!!
        }
    }

    fun getCtrlDisabledData(context: Context) : String {
        val settings = getSettings(context)
        val preferences = getPrefs(context)

        return if (preferences.getBoolean("custom_ctrl_file_data", false)) {
            // Custom Data Enabled
            settings.getString(Constants.SAVED_DISABLED_DATA, DEFAULT_DISABLED)!!
        } else {
            // Custom Data Disabled
            settings.getString(CHARGE_OFF_KEY, DEFAULT_DISABLED)!!
        }
    }

    fun setTheme(activity: Activity) {
        val preferences = getPrefs(activity)
        val getTheme = preferences.getString(PrefsFragment.KEY_THEME, Constants.LIGHT)
        var theme = R.style.AppThemeLight_NoActionBar
        when (getTheme) {
            Constants.LIGHT -> { theme = R.style.AppThemeLight_NoActionBar }
            Constants.DARK -> { theme = R.style.AppThemeDark_NoActionBar }
            Constants.BLACK -> { theme = R.style.AppThemeBlack_NoActionBar }
        }
        activity.setTheme(theme)
    }

    /**
     * FUTURE provide customisability if required
     */
    fun getVoltageFile(): String {
        return Constants.DEFAULT_VOLTAGE_FILE
    }

    private var vfInitialized = false

    fun setVoltageThreshold(voltage: String?, onlyIfEnabled: Boolean, context: Context, handler: Handler?) {
        if (onlyIfEnabled && !getSettings(context).getBoolean(Constants.LIMIT_BY_VOLTAGE, false)) {
            return
        }

        val voltageThreshold = voltage ?: getSettings(context).getString(Constants.CUSTOM_VOLTAGE_LIMIT, null)

        if (voltageThreshold == null) {
            Log.e(TAG, "Custom Voltage Threshold not valid")
            return
        }

        val switchCommands: Array<String>
        val voltageFile = getVoltageFile()
        if (vfInitialized) {
            switchCommands = arrayOf("echo $voltageThreshold > $voltageFile")
        } else {
            vfInitialized = true
            switchCommands = arrayOf("mount -o rw,remount $voltageFile", "chmod u+w $voltageFile",
                    "echo $voltageThreshold > $voltageFile")
        }
        executor.submit {
            suShell.addCommand(switchCommands)
        }
        getCurrentVoltageThresholdAsync(context, handler)
    }

    fun getCurrentVoltageThresholdAsync(context: Context, handler: Handler?){
        executor.submit {
            val voltageFile = getVoltageFile()
            suShell.addCommand("cat $voltageFile", 0) { _, _, output ->
                if (output.size != 0 ) {
                    val voltage = output[0]
                    val sharedPrefs = getSettings(context)
                    if (sharedPrefs.getString(Constants.DEFAULT_VOLTAGE_LIMIT, null) == null) {
                        sharedPrefs.edit().putString(Constants.DEFAULT_VOLTAGE_LIMIT, voltage).apply()
                    }
                    if (handler != null) {
                        val msg = handler.obtainMessage(MainActivity.MSG_UPDATE_VOLTAGE_THRESHOLD)
                        val bundle = Bundle()
                        bundle.putString(MainActivity.VOLTAGE_THRESHOLD, voltage)
                        msg.data = bundle
                        handler.sendMessage(msg)
                    }
                }
            }
        }
    }

    /**
     * We assume the voltage is atleast 4 digits, and the first 4 digits make milli-Volts
     */
    fun isValidVoltageThreshold(newThreshold: String, currentThreshold: String): Boolean {
        if (newThreshold.length == currentThreshold.length && newThreshold.length > 3) {
            Log.i("copy: ", "outer")
            val voltage = newThreshold.substring(0, 4).toInt()
            val minVolThres = Constants.MIN_VOLTAGE_THRESHOLD_MV.toInt()
            val manVolThres = Constants.MAX_VOLTAGE_THRESHOLD_MV.toInt()
            if (voltage in minVolThres..manVolThres) {
                return true
            }
        }
        Log.i(TAG, "$newThreshold not valid. Current threshold: $currentThreshold")
        return false
    }
}