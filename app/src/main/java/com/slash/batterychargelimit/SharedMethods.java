package com.slash.batterychargelimit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.StringRes;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;

import java.io.IOException;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 17/3/17.
 */

public class SharedMethods {

    public static int CHARGE_ON = 0;
    public static int CHARGE_OFF = 1;

    public static boolean checkFile(String path) {
        return "0".equals(Shell.SU.run(new String[] {"stat " + path + " >/dev/null", "echo $?"}).get(0));
    }

    public static void changeState(Context context, int chargeMode) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        String file = settings.getString(Constants.FILE_KEY,
                "/sys/class/power_supply/battery/charging_enabled");

        String newState;
        if (chargeMode == CHARGE_ON) {
            newState = settings.getString(Constants.CHARGE_ON_KEY, "1");
        } else {
            newState = settings.getString(Constants.CHARGE_OFF_KEY, "0");
        }

        String recentState = Shell.SU.run("cat " + file).get(0);
        if (!recentState.equals(newState)) {
            if (chargeMode == CHARGE_OFF) {
                settings.edit().putLong(LIMIT_REACHED, System.currentTimeMillis()).apply();
            }
            Shell.SU.run(new String[] {
                    "mount -o rw,remount " + file,
                    "echo " + newState + " > " + file
            });
        }
    }

    public static boolean isPhonePluggedIn(Context context) {
        final Intent batteryIntent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return isPhonePluggedIn(batteryIntent);
    }
    public static boolean isPhonePluggedIn(Intent batteryIntent) {
        return batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                == BatteryManager.BATTERY_STATUS_CHARGING
                || batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) > 0;
    }

    public static void toastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
    public static void toastMessage(Context context, @StringRes int messageRes) {
        Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show();
    }

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return getBatteryLevel(batteryIntent);
    }
    public static int getBatteryLevel(Intent batteryIntent) {
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return 50;
        }
        return level * 100 / scale;
    }

    public static void resetBatteryStats(Context context){
        Shell.SU.run("dumpsys batterystats --reset");
        toastMessage(context, R.string.stats_reset_success);
    }
}
