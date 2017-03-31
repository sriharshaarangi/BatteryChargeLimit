package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 30/1/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        boolean en = settings.getBoolean(ENABLE, false);
        if (en) {
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                context.startService(new Intent(context, ForegroundService.class));
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                long limitReached = settings.getLong(LIMIT_REACHED, -1);
                if (limitReached > 0 && limitReached > System.currentTimeMillis() - UNPLUG_TOLERANCE) {
                    settings.edit().putLong(LIMIT_REACHED, -1).apply();
                } else {
                    context.stopService(new Intent(context, ForegroundService.class));
                }
            }
        }
    }
}
