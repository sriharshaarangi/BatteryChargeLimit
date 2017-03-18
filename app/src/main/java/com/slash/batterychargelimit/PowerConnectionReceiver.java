package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by harsha on 30/1/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {
    Context powerConnectionReceiverContext;
    SharedPreferences settings;

    public void onReceive(Context context, Intent intent) {
        powerConnectionReceiverContext = context;
        String action = intent.getAction();
        settings = powerConnectionReceiverContext.getSharedPreferences("Settings", 0);
        boolean en = settings.getBoolean("enable", false);
        if (en) {
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                Intent startIntent = new Intent(powerConnectionReceiverContext, ForegroundService.class);
                startIntent.setAction("connected");
                powerConnectionReceiverContext.startService(startIntent);
            }
            else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Boolean isLimitReached = settings.getBoolean("limitReached", false);
                if (isLimitReached) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("limitReached", false);
                    editor.apply();
                } else {
                    Intent startIntent = new Intent(powerConnectionReceiverContext, ForegroundService.class);
                    startIntent.setAction("reset");
                    powerConnectionReceiverContext.startService(startIntent);
                }
            }
        }
    }
}
