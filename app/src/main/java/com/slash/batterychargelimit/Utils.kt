package com.slash.batterychargelimit

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.slash.batterychargelimit.Constants.CHARGE_OFF_KEY
import com.slash.batterychargelimit.Constants.CHARGE_ON_KEY
import com.slash.batterychargelimit.Constants.CHARGING_CHANGE_TOLERANCE_MS
import com.slash.batterychargelimit.Constants.DEFAULT_DISABLED
import com.slash.batterychargelimit.Constants.DEFAULT_ENABLED
import com.slash.batterychargelimit.Constants.DEFAULT_FILE
import com.slash.batterychargelimit.Constants.ENABLE
import com.slash.batterychargelimit.Constants.FILE_KEY
import com.slash.batterychargelimit.Constants.LIMIT
import com.slash.batterychargelimit.Constants.MIN
import com.slash.batterychargelimit.Constants.NOTIFICATION_LIVE
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.settings.SettingsFragment
import eu.chainfire.libsuperuser.Shell
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.concurrent.*

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

    val suShell: Shell.Interactive = Shell.Builder().setWantSTDERR(false).useSU().open()

    /**
     * Helper function to wait for the su shell in a blocking fashion (max. 3 seconds).
     * Chainfire considers this a bad practice, so use wisely!
     */
    fun waitForShell() {
        try {
            executor.submit { suShell.waitForIdle() }.get(3, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.wtf(TAG, e)
        } catch (e: ExecutionException) {
            Log.wtf(TAG, e)
        } catch (e: TimeoutException) {
            Log.w(TAG, "Timeout: Shell blocked more than 3 seconds, continue app execution.", e)
        }

    }

    val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun changeState(context: Context, chargeMode: Int) {
        val preferences = getPrefs(context)
        val alwaysWrite = preferences.getBoolean(SettingsFragment.KEY_ALWAYS_WRITE_CF, false)

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
        Utils.stopService(context)
        getPrefs(context)
                .edit().putString(SettingsFragment.KEY_CONTROL_FILE, cf.file).apply()
        context.getSharedPreferences(SETTINGS, 0)
                .edit().putString(FILE_KEY, cf.file)
                .putString(CHARGE_ON_KEY, cf.chargeOn)
                .putString(CHARGE_OFF_KEY, cf.chargeOff).apply()
        //Respawn the service if necessary
        Utils.startService(context)
    }

    fun isPhonePluggedIn(context: Context): Boolean {
        val batteryIntent = context.applicationContext.registerReceiver(null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
        val max = settings.getInt(LIMIT, 80)
        // calculate new recharge threshold from previous distance
        val min = Math.max(0, limit - (max - settings.getInt(MIN, max - 2)))
        settings.edit().putInt(LIMIT, limit).putInt(MIN, min).apply()
    }

    fun handleLimitChange(context: Context, newLimit: Any) {
        try {
            val limit = if (newLimit is Number) {
                newLimit.toInt()
            } else {
                Integer.parseInt(newLimit.toString())
            }
            if (limit == 100) {
                val settings = context.getSharedPreferences(SETTINGS, 0)
                stopService(context)
                settings.edit().putBoolean(ENABLE, false).apply()
            } else if (limit in 40..99) {
                val settings = context.getSharedPreferences(SETTINGS, 0)
                // set the new limit
                setLimit(limit, settings)
                Toast.makeText(context, context.getString(R.string.intent_limit_accepted, limit),
                        Toast.LENGTH_SHORT).show()
                if (!settings.getBoolean(NOTIFICATION_LIVE, false)) {
                    settings.edit().putBoolean(ENABLE, true).apply()
                    startService(context)
                }
            } else {
                throw NumberFormatException("Battery limit out of range!")
            }
        } catch (fe: NumberFormatException) {
            Toast.makeText(context, R.string.intent_limit_invalid, Toast.LENGTH_SHORT).show()
        }

    }

    fun startService(context: Context) {
        if (context.getSharedPreferences(SETTINGS, 0).getBoolean(ENABLE, false)) {
            if (!getPrefs(context).getBoolean(SettingsFragment.KEY_ENABLE_AUTO_RECHARGE, true)) {
                changeState(context, Utils.CHARGE_ON)
            }
            Handler().postDelayed({
                if (Utils.isPhonePluggedIn(context)) {
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

    fun stopService(context: Context, ignoreAutoReset: Boolean = true) {
        if (ignoreAutoReset) {
            ForegroundService.ignoreAutoReset()
        }
        context.stopService(Intent(context, ForegroundService::class.java))
        if(getPrefs(context).getBoolean(SettingsFragment.KEY_ENABLE_AUTO_RECHARGE, true))
            Utils.changeState(context, CHARGE_ON)
        // display service disabled Toast message if not disabled in settings
        if (!getPrefs(context).getBoolean("hide_toast_on_service_changes", false)) {
            Toast.makeText(context, R.string.service_disabled, Toast.LENGTH_SHORT).show()
        }
    }

    fun getCtrlFileData (context: Context): String? {
        val settings = context.getSharedPreferences(SETTINGS, 0)
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
        val settings = context.getSharedPreferences(SETTINGS, 0)
        val preferences = getPrefs(context)

        return if (preferences.getBoolean("custom_ctrl_file_data", false)) {
            // Custom Data Enabled
            settings.getString(Constants.SAVED_ENABLED_DATA, DEFAULT_ENABLED)
        } else {
            // Custom Data Disabled
            settings.getString(CHARGE_ON_KEY, DEFAULT_ENABLED)
        }
    }

    fun getCtrlDisabledData(context: Context) : String {
        val settings = context.getSharedPreferences(SETTINGS, 0)
        val preferences = getPrefs(context)

        return if (preferences.getBoolean("custom_ctrl_file_data", false)) {
            // Custom Data Enabled
            settings.getString(Constants.SAVED_DISABLED_DATA, DEFAULT_DISABLED)
        } else {
            // Custom Data Disabled
            settings.getString(CHARGE_OFF_KEY, DEFAULT_DISABLED)
        }
    }
}
