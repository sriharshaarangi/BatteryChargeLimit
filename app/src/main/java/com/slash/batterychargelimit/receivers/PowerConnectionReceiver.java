package com.slash.batterychargelimit.receivers;

import android.content.*;
import android.preference.PreferenceManager;
import android.util.Log;
import com.slash.batterychargelimit.Constants;
import com.slash.batterychargelimit.ForegroundService;
import com.slash.batterychargelimit.SharedMethods;
import com.slash.batterychargelimit.settings.SettingsFragment;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 30/1/17.
 *
 * This BroadcastReceiver handles the change of the power supply state.
 * Because control files like charging_enabled are causing fake events, there is a time window POWER_CHANGE_TOLERANCE_MS
 * milliseconds where the respective "changes" of the power supply will be ignored.
 *
 * 21/4/17 milux: Changed to avoid service (re)start because of fake power on event
 */

public class PowerConnectionReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        //Ignore new events after power change or during state fixing
        if (!PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(SettingsFragment.KEY_IMMEDIATE_POWER_INTENT_HANDLING, false)
                && SharedMethods.isChangePending(Math.max(Constants.POWER_CHANGE_TOLERANCE_MS,
                BatteryReceiver.getBackOffTime() * 2))) {
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                //Ignore connected event only if service is running
                if (ForegroundService.isRunning()) {
                    Log.d("Power State", "ACTION_POWER_CONNECTED ignored");
                    return;
                }
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Log.d("Power State", "ACTION_POWER_DISCONNECTED ignored");
                return;
            }
        }
        if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
            Log.d("Power State", "ACTION_POWER_CONNECTED");
            SharedMethods.startService(context);
        } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
            Log.d("Power State", "ACTION_POWER_DISCONNECTED");
            SharedMethods.stopService(context, false);
        }
    }
}
