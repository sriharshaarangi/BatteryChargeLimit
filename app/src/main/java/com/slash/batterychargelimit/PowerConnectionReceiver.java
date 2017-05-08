package com.slash.batterychargelimit;

import android.content.*;
import android.os.Handler;
import android.util.Log;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

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
        if (SharedMethods.isChangePending(Constants.POWER_CHANGE_TOLERANCE_MS)) {
            return;
        }
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        if (settings.getBoolean(ENABLE, false)) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                Log.d("Power State", "ACTION_POWER_CONNECTED");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SharedMethods.enableService(context);
                    }
                }, CHARGING_CHANGE_TOLERANCE_MS);
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Log.d("Power State", "ACTION_POWER_DISCONNECTED");
                SharedMethods.disableService(context, false);
            }
        }
    }
}
