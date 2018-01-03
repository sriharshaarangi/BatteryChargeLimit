package com.slash.batterychargelimit.receivers

import android.content.*
import android.os.BatteryManager
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import com.slash.batterychargelimit.Constants.POWER_CHANGE_TOLERANCE_MS
import com.slash.batterychargelimit.Constants.MAX_BACK_OFF_TIME
import com.slash.batterychargelimit.Constants.CHARGING_CHANGE_TOLERANCE_MS
import com.slash.batterychargelimit.Constants.LIMIT
import com.slash.batterychargelimit.Constants.MIN
import com.slash.batterychargelimit.Constants.NOTIF_CHARGE
import com.slash.batterychargelimit.Constants.NOTIF_MAINTAIN
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.ForegroundService
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.SharedMethods
import com.slash.batterychargelimit.settings.SettingsFragment
import android.media.RingtoneManager
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.Constants.ENABLE
import com.slash.batterychargelimit.Constants.NOTIFICATION_SOUND


/**
 * Created by Michael on 01.04.2017.
 *
 * Dynamically created receiver for battery events. Only registered if power supply is attached.
 */
class BatteryReceiver(private val service: ForegroundService) : BroadcastReceiver() {

    private var chargedToLimit = false
    private var useFahrenheit = false
    private var lastState = -1
    private var limitPercentage: Int = 0
    private var rechargePercentage: Int = 0
    private val prefs: SharedPreferences
    private var preferenceChangeListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    init {
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                SettingsFragment.KEY_TEMP_FAHRENHEIT -> {
                    useFahrenheit = sharedPreferences.getBoolean(SettingsFragment.KEY_TEMP_FAHRENHEIT, false)
                    service.setNotificationContentText(SharedMethods.getBatteryInfo(service,
                            service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)),
                            useFahrenheit))
                    service.updateNotification()
                }
                LIMIT, MIN -> reset(sharedPreferences)
            }
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(service.baseContext)
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        val settings = service.getSharedPreferences(SETTINGS, 0)
        settings.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        this.useFahrenheit = prefs.getBoolean(SettingsFragment.KEY_TEMP_FAHRENHEIT, false)
        reset(settings)
    }

    private fun reset(settings: SharedPreferences) {
        chargedToLimit = false
        lastState = -1
        backOffTime = CHARGING_CHANGE_TOLERANCE_MS
        limitPercentage = settings.getInt(LIMIT, 80)
        rechargePercentage = settings.getInt(MIN, limitPercentage - 2)
        //Manually fire onReceive() to update state
        onReceive(service, service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
    }

    /**
     * Remembers the new state and returns whether the state was changed
     *
     * @param newState the new state
     * @return whether the state has changed
     */
    private fun switchState(newState: Int): Boolean {
        val oldState = lastState
        lastState = newState
        return oldState != newState
    }

    /**
     * If battery should be charging, but there's no power supply, stop the service.
     * NOT to be called if charging is expected to be disabled!
     */
    private fun stopIfUnplugged() {
        // save the state that caused this function call
        val triggerState = lastState
        handler.postDelayed({
            // continue only if the state didn't change in the meantime
            if (triggerState == lastState && !SharedMethods.isPhonePluggedIn(service)) {
                SharedMethods.stopService(service, false)
            }
        }, POWER_CHANGE_TOLERANCE_MS)
    }

    /**
     *
     */
    private fun notifyLimitReached() {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        service.setNotificationSound(soundUri)
    }

    override fun onReceive(context: Context?, intent: Intent) {
        val settings = context!!.getSharedPreferences(Constants.SETTINGS, 0)
        // ignore events while trying to fix charging state, see below
        if (SharedMethods.isChangePending(backOffTime * 2)) {
            return
        }
        Log.d("BatteryReceiver", "onReceive " + this.hashCode())

        val batteryLevel = SharedMethods.getBatteryLevel(intent)
        val currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)

        if (settings.getBoolean(ENABLE, false)) {
            // when the service was "freshly started", charge until limit
            if (!chargedToLimit && batteryLevel < limitPercentage) {
                if (switchState(CHARGE_FULL)) {
                    Log.d("Charging State", "CHARGE_FULL " + this.hashCode())
                    SharedMethods.changeState(service, SharedMethods.CHARGE_ON)
                    service.setNotificationTitle(service.getString(R.string.waiting_until_x, limitPercentage))
                    service.setNotificationContentText(service.getString(R.string.waiting_description))
                    service.setNotificationIcon(NOTIF_CHARGE)
                    stopIfUnplugged()
                }
            } else if (batteryLevel >= limitPercentage) {
                if (switchState(CHARGE_STOP)) {
                    Log.d("Charging State", "CHARGE_STOP " + this.hashCode())
                    if(settings.getBoolean(NOTIFICATION_SOUND, false) && !chargedToLimit)
                        notifyLimitReached()
                    // remember that we let the device charge until limit at least once
                    chargedToLimit = true
                    // active auto reset on service shutdown
                    service.enableAutoReset()
                    SharedMethods.changeState(service, SharedMethods.CHARGE_OFF)
                    // set the "maintain" notification, this must not change from now
                    service.setNotificationTitle(service.getString(R.string.maintaining_x_to_y,
                            rechargePercentage, limitPercentage))
                    service.setNotificationContentText(service.getString(R.string.maintaining_description))
                    service.setNotificationIcon(NOTIF_MAINTAIN)
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_CHARGING
                        && prefs.getBoolean(SettingsFragment.KEY_ENFORCE_CHARGE_LIMIT, true)) {
                    //Double the back off time with every unsuccessful round up to MAX_BACK_OFF_TIME
                    backOffTime = Math.min(backOffTime * 2, MAX_BACK_OFF_TIME)
                    Log.d("Charging State", "Fixing state w. CHARGE_ON/CHARGE_OFF " + this.hashCode()
                            + " (Delay: $backOffTime)")
                    // if the device did not stop charging, try to "cycle" the state to fix this
                    SharedMethods.changeState(service, SharedMethods.CHARGE_ON)
                    // schedule the charging stop command to be executed after CHARGING_CHANGE_TOLERANCE_MS
                    val service = this.service
                    handler.postDelayed({ SharedMethods.changeState(service, SharedMethods.CHARGE_OFF) }, backOffTime)
                } else {
                    backOffTime = CHARGING_CHANGE_TOLERANCE_MS
                }
            } else if (batteryLevel < rechargePercentage) {
                if (switchState(CHARGE_REFRESH)) {
                    Log.d("Charging State", "CHARGE_REFRESH " + this.hashCode())
                    service.setNotificationIcon(NOTIF_CHARGE)
                    service.setNotificationTitle(service.getString(R.string.waiting_until_x, limitPercentage))
                    service.setNotificationContentText(service.getString(R.string.waiting_description))
                    SharedMethods.changeState(service, SharedMethods.CHARGE_ON)
                    stopIfUnplugged()
                }
            }
        }

        // update battery status information and rebuild notification
        // service.setNotificationContentText(SharedMethods.getBatteryInfo(service, intent, useFahrenheit))
        service.updateNotification()
        service.removeNotificationSound()
        Log.d("BatteryReceiver", "onReceive executed " + this.hashCode())

    }

    fun detach() {
        //Technically not necessary, but it prevents inlining of this required field
        //See end of https://developer.android.com/guide/topics/ui/settings.html#Listening
        this.preferenceChangeListener = null
    }

    companion object {
        private const val CHARGE_FULL = 0
        private const val CHARGE_STOP = 1
        private const val CHARGE_REFRESH = 2

        private val handler = Handler()
        internal var backOffTime = CHARGING_CHANGE_TOLERANCE_MS
    }

}
