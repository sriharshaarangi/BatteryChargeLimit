package com.slash.batterychargelimit.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.slash.batterychargelimit.ForegroundService;
import com.slash.batterychargelimit.SharedMethods;

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
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedMethods.startService(context);
        }
    }
}
