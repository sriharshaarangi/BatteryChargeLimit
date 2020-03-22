package com.slash.batterychargelimit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.slash.batterychargelimit.Constants.POWER_CHANGE_TOLERANCE_MS
import com.slash.batterychargelimit.ForegroundService
import com.slash.batterychargelimit.Utils
import com.slash.batterychargelimit.settings.PrefsFragment

/**
 * Created by harsha on 30/1/17.
 *
 * This BroadcastReceiver handles the change of the power supply state.
 * Because control files like charging_enabled are causing fake events, there is a time window POWER_CHANGE_TOLERANCE_MS
 * milliseconds where the respective "changes" of the power supply will be ignored.
 *
 * 21/4/17 milux: Changed to avoid service (re)start because of fake power on event
 */

class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Utils.setVoltageThreshold(null, true, context, null)

        //Ignore new events after power change or during state fixing
        if (!Utils.getPrefs(context).getBoolean(PrefsFragment.KEY_IMMEDIATE_POWER_INTENT_HANDLING, false)
                && Utils.isChangePending((BatteryReceiver.backOffTime * 2).coerceAtLeast(POWER_CHANGE_TOLERANCE_MS))) {
            if (action == Intent.ACTION_POWER_CONNECTED) {
                //Ignore connected event only if service is running
                if (ForegroundService.isRunning
                        || Utils.getPrefs(context).getBoolean(PrefsFragment.KEY_DISABLE_AUTO_RECHARGE, false)) {
                    Log.d("Power State", "ACTION_POWER_CONNECTED ignored")
                    return
                }
            } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
                Log.d("Power State", "ACTION_POWER_DISCONNECTED ignored")
                return
            }
        }

        if (action == Intent.ACTION_POWER_CONNECTED) {
            Log.d("Power State", "ACTION_POWER_CONNECTED")
            Utils.startServiceIfLimitEnabled(context)
        } else if (action == Intent.ACTION_POWER_DISCONNECTED) {
            Log.d("Power State", "ACTION_POWER_DISCONNECTED")
            Utils.stopService(context, false)
        }
    }
}
