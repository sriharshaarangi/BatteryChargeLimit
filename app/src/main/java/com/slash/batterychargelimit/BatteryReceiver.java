package com.slash.batterychargelimit;

import android.content.*;
import android.os.BatteryManager;
import android.os.Handler;
import android.util.Log;
import eu.chainfire.libsuperuser.Shell;

import static com.slash.batterychargelimit.Constants.*;
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
    private int limitPercentage, rechargePercentage;
    // interactive shell for better performance
    private Shell.Interactive shell;
    private final static Handler handler = new Handler();

    BatteryReceiver(ForegroundService service, Shell.Interactive shell) {
        this.service = service;
        this.shell = shell;
    }

    /**
     * "Restart" this receiver by resetting its flags/limits and calling its onReceive() handler
     *
     * @param callOnReceive Whether to call the onReceive() method
     */
    public void reset(boolean callOnReceive) {
        chargedToLimit = false;
        lastState = -1;
        SharedPreferences settings = service.getSharedPreferences(SETTINGS, 0);
        limitPercentage = settings.getInt(LIMIT, 80);
        rechargePercentage = limitPercentage - settings.getInt(RECHARGE_DIFF, 2);
        if (callOnReceive) {
            Intent batteryIntent = service.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            onReceive(service, batteryIntent);
        }
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

    /**
     * If battery should be charging, but there's no power supply, stop the service.
     * NOT to be called if charging is expected to be disabled!
     */
    private void stopIfUnplugged() {
        // save the state that caused this function call
        final int triggerState = lastState;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // continue only if the state didn't change in the meantime
                if (triggerState == lastState && !SharedMethods.isPhonePluggedIn(service)) {
                    service.stopService(new Intent(service, ForegroundService.class));
                }
            }
        }, CHARGING_CHANGE_TOLERANCE_MS);
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        // ignore events while trying to fix charging state, see below
        if (SharedMethods.isChangePending(CHARGING_CHANGE_TOLERANCE_MS)) {
            return;
        }

        int batteryLevel = SharedMethods.getBatteryLevel(intent);
        int currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);

        // when the service was "freshly started", charge until limit
        if (!chargedToLimit && batteryLevel < limitPercentage) {
            if (switchState(CHARGE_FULL)) {
                Log.d("Charging State", "CHARGE_FULL");
                SharedMethods.changeState(service, shell, CHARGE_ON);
                service.setNotification(service.getString(R.string.waiting_until_x, limitPercentage));
                stopIfUnplugged();
            }
        } else if (batteryLevel >= limitPercentage) {
            if (switchState(CHARGE_STOP)) {
                Log.d("Charging State", "CHARGE_STOP " + this.hashCode());
                // remember that we let the device charge until limit at least once
                chargedToLimit = true;
                // active auto reset on service shutdown
                service.enableAutoReset();
                SharedMethods.changeState(service, shell, CHARGE_OFF);
                // set the "maintain" notification, this must not change from now
                service.setNotification(service.getString(R.string.maintaining_x_to_y,
                        rechargePercentage, limitPercentage));
            } else if (currentStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                Log.d("Charging State", "Fixing state w. CHARGE_ON/CHARGE_OFF");
                // if the device did not stop charging, try to "cycle" the state to fix this
                SharedMethods.changeState(service, shell, CHARGE_ON);
                // schedule the charging stop command to be executed after CHARGING_CHANGE_TOLERANCE_MS
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SharedMethods.changeState(service, shell, CHARGE_OFF);
                    }
                }, CHARGING_CHANGE_TOLERANCE_MS);
            }
        } else if (batteryLevel < rechargePercentage) {
            if (switchState(CHARGE_REFRESH)) {
                Log.d("Charging State", "CHARGE_REFRESH");
                SharedMethods.changeState(service, shell, CHARGE_ON);
                stopIfUnplugged();
            }
        }
    }

}
