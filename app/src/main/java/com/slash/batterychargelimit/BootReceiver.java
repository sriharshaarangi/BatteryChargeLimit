package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static com.slash.batterychargelimit.Constants.ENABLE;
import static com.slash.batterychargelimit.Constants.SETTINGS;

/**
 * Created by Michael on 20.04.2017.
 *
 * Triggered when the phone finished booting.
 * Checks whether power supply is attached and starts the foreground service if necessary.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(context.getSharedPreferences(SETTINGS, 0).getBoolean(ENABLE, false)
                && SharedMethods.isPhonePluggedIn(context)) {
            context.startService(new Intent(context, ForegroundService.class));
        }
    }
}
