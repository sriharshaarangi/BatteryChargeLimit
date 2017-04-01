package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static com.slash.batterychargelimit.Constants.LIMIT;
import static com.slash.batterychargelimit.Constants.RECHARGE_DIFF;
import static com.slash.batterychargelimit.Constants.SETTINGS;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_OFF;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

/**
 * Created by Michael on 01.04.2017.
 *
 * Dynamically created receiver for battery events. Only registered if power supply is attached.
 */
public class BatteryReceiver extends BroadcastReceiver {
    private static final int CHARGE_FULL = 0, CHARGE_STOP = 1, CHARGE_REFRESH = 2;

    private boolean chargedToLimit = false;
    private int lastState = -1;
    private ForegroundService service;
    private SharedPreferences settings;

    BatteryReceiver(ForegroundService service) {
        this.service = service;
        this.settings = service.getSharedPreferences(SETTINGS, 0);
    }

    /**
     * Remembers the new state and returns whether the state was changed
     *
     * @param newState the new state
     * @return whether the state has changed
     */
    private boolean switchState(int newState) {
        int oldState = lastState;
        lastState = newState;
        return oldState != newState;
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        int limitPercentage = settings.getInt(LIMIT, 80);
        int rechargePercentage = limitPercentage - settings.getInt(RECHARGE_DIFF, 2);
        int batteryLevel = SharedMethods.getBatteryLevel(intent);

        // when the service was "freshly started", charge until limit
        if (!chargedToLimit && batteryLevel < limitPercentage) {
            if (switchState(CHARGE_FULL)) {
                SharedMethods.changeState(service, CHARGE_ON);
                service.setNotification(service.getString(R.string.waiting_until_x, limitPercentage));
            }
        } else if (batteryLevel >= limitPercentage) {
            if (switchState(CHARGE_STOP)) {
                // remember that we let the device charge until limit at least once
                chargedToLimit = true;
                // remember the time when disabling charging, so that PowerConnectionReceiver can identify fakes
                SharedMethods.changeState(service, CHARGE_OFF);
                // set the "maintain" notification, this must not change from now
                service.setNotification(service.getString(R.string.maintaining_x_to_y,
                        rechargePercentage, limitPercentage));
            }
        } else if (batteryLevel < rechargePercentage) {
            if (switchState(CHARGE_REFRESH)) {
                SharedMethods.changeState(service, CHARGE_ON);
                // for those control files that with fake unplug events, stop service if power source was detached
                if (!SharedMethods.isPhonePluggedIn(intent)) {
                    service.stopService(new Intent(service, ForegroundService.class));
                }
            }
        }
    }

}
