package com.slash.batterychargelimit;

import android.content.*;
import android.os.IBinder;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

/**
 * Created by harsha on 30/1/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        if (settings.getBoolean(ENABLE, false)) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                context.startService(new Intent(context, ForegroundService.class));
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                if (settings.getLong(LIMIT_REACHED, -1) <= System.currentTimeMillis() - UNPLUG_TOLERANCE) {
                    SharedMethods.changeState(context, CHARGE_ON);
                    context.stopService(new Intent(context, ForegroundService.class));
                }
            }
        }
    }
}
