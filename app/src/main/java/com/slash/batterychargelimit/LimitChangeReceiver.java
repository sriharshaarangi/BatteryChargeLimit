package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

/**
 * Created by Michael on 20.04.2017.
 *
 * Handles new battery limits sent via Broadcasts.
 */
public class LimitChangeReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        SharedMethods.handleLimitChange(context, intent.getStringExtra(Intent.EXTRA_TEXT));
    }

}
