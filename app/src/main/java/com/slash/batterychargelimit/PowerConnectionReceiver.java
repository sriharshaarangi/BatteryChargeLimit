package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by harsha on 30/1/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        SharedPreferences settings = context.getSharedPreferences("Settings", 0);
        boolean en = settings.getBoolean("enable", false);
        if (en) {
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                Intent startIntent = new Intent(context, ForegroundService.class);
                startIntent.setAction("connected");
                context.startService(startIntent);
            }
            else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                Boolean isLimitReached = settings.getBoolean("limitReached", false);
                if (isLimitReached) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("limitReached", false);
                    editor.apply();
                } else {
                    Intent startIntent = new Intent(context, ForegroundService.class);
                    startIntent.setAction("reset");
                    context.startService(startIntent);
                }
            }
        }
    }
}
