package com.slash.batterychargelimit.receivers;

import android.content.*;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import com.slash.batterychargelimit.ForegroundService;
import com.slash.batterychargelimit.R;
import com.slash.batterychargelimit.SharedMethods;
import com.slash.batterychargelimit.settings.SettingsFragment;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_OFF;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

/**
 * Created by Michael on 01.04.2017.
 *
 * Dynamically created receiver for battery events. Only registered if power supply is attached.
 */
public class BatteryReceiver extends BroadcastReceiver {
    private static final String TAG = BatteryReceiver.class.getSimpleName();
    private static final int CHARGE_FULL = 0, CHARGE_STOP = 1, CHARGE_REFRESH = 2;

    private final static Handler handler = new Handler();
    private static long backOffTime = CHARGING_CHANGE_TOLERANCE_MS;
    static long getBackOffTime() {
        return backOffTime;
    }

    private boolean chargedToLimit = false, useFahrenheit = false;
    private int lastState = -1;
    private final ForegroundService service;
    private int limitPercentage, rechargePercentage;

    public BatteryReceiver(final ForegroundService service) {
        this.service = service;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(service.getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (SettingsFragment.KEY_TEMP_FAHRENHEIT.equals(key)) {
                    useFahrenheit = sharedPreferences.getBoolean(SettingsFragment.KEY_TEMP_FAHRENHEIT, false);
                    service.setNotificationContentText(SharedMethods.getBatteryInfo(service,
                            service.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)),
                            useFahrenheit));
                    service.updateNotification();
                }
            }
        });
        this.useFahrenheit = prefs.getBoolean(SettingsFragment.KEY_TEMP_FAHRENHEIT, false);
        SharedPreferences settings = service.getSharedPreferences(SETTINGS, 0);
        settings.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences settings, String key) {
                if (LIMIT.equals(key) || MIN.equals(key)) {
                    reset(settings);
                }
            }
        });
        reset(settings);
    }

    private void reset(SharedPreferences settings) {
        chargedToLimit = false;
        lastState = -1;
        backOffTime = CHARGING_CHANGE_TOLERANCE_MS;
        limitPercentage = settings.getInt(LIMIT, 80);
        rechargePercentage = settings.getInt(MIN, limitPercentage - 2);
        //Manually fire onReceive() to update state
        onReceive(service, service.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
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
                    SharedMethods.stopService(service, false);
                }
            }
        }, POWER_CHANGE_TOLERANCE_MS);
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        // ignore events while trying to fix charging state, see below
        if (SharedMethods.isChangePending(backOffTime * 2)) {
            return;
        }

        int batteryLevel = SharedMethods.getBatteryLevel(intent);
        int currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);

        // when the service was "freshly started", charge until limit
        if (!chargedToLimit && batteryLevel < limitPercentage) {
            if (switchState(CHARGE_FULL)) {
                Log.d("Charging State", "CHARGE_FULL " + this.hashCode());
                SharedMethods.changeState(service, CHARGE_ON);
                service.setNotificationTitle(service.getString(R.string.waiting_until_x, limitPercentage));
                stopIfUnplugged();
            }
        } else if (batteryLevel >= limitPercentage) {
            if (switchState(CHARGE_STOP)) {
                Log.d("Charging State", "CHARGE_STOP " + this.hashCode());
                // remember that we let the device charge until limit at least once
                chargedToLimit = true;
                // active auto reset on service shutdown
                service.enableAutoReset();
                SharedMethods.changeState(service, CHARGE_OFF);
                // set the "maintain" notification, this must not change from now
                service.setNotificationTitle(service.getString(R.string.maintaining_x_to_y,
                        rechargePercentage, limitPercentage));
            } else if (currentStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                //Double the back off time with every unsuccessful round up to MAX_BACK_OFF_TIME
                backOffTime = Math.min(backOffTime * 2, MAX_BACK_OFF_TIME);
                Log.d("Charging State", "Fixing state w. CHARGE_ON/CHARGE_OFF " + this.hashCode()
                        + " (Delay: " + backOffTime + ")");
                // if the device did not stop charging, try to "cycle" the state to fix this
                SharedMethods.changeState(service, CHARGE_ON);
                // schedule the charging stop command to be executed after CHARGING_CHANGE_TOLERANCE_MS
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SharedMethods.changeState(service, CHARGE_OFF);
                    }
                }, backOffTime);
            } else {
                backOffTime = CHARGING_CHANGE_TOLERANCE_MS;
            }
        } else if (batteryLevel < rechargePercentage) {
            if (switchState(CHARGE_REFRESH)) {
                Log.d("Charging State", "CHARGE_REFRESH " + this.hashCode());
                SharedMethods.changeState(service, CHARGE_ON);
                stopIfUnplugged();
            }
        }

        // update battery status information and rebuild notification
        service.setNotificationContentText(SharedMethods.getBatteryInfo(service, intent, useFahrenheit));
        service.updateNotification();
    }

}
