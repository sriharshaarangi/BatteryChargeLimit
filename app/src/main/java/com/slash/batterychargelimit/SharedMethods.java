package com.slash.batterychargelimit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;

import java.util.List;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 17/3/17.
 */

public class SharedMethods {
    public final static int CHARGE_ON = 0;
    public final static int CHARGE_OFF = 1;

    // remember pending state change
    private static long changePending = 0;

    /**
     * Inform the BatteryReceiver instance(es) to ignore events for CHARGING_CHANGE_TOLERANCE_MS,
     * in order to let the state change settle.
     */
    public static void setChangePending() {
        // update changePending to prevent concurrent state changes before execution
        changePending = System.currentTimeMillis();
    }

    /**
     * Returns whether some change happened at most CHARGING_CHANGE_TOLERANCE_MS ago.
     *
     * @return Whether state change is pending
     */
    public static boolean isChangePending(long tolerance) {
        return System.currentTimeMillis() <= changePending + tolerance;
    }

    public static boolean checkFile(String path) {
        return "0".equals(Shell.SU.run(new String[] {"test -e " + path, "echo $?"}).get(0));
    }

    public static void changeState(Context context, final Shell.Interactive shell, final int chargeMode) {
        final SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        final String file = settings.getString(Constants.FILE_KEY,
                "/sys/class/power_supply/battery/charging_enabled");

        final String newState;
        if (chargeMode == CHARGE_ON) {
            newState = settings.getString(Constants.CHARGE_ON_KEY, "1");
        } else {
            newState = settings.getString(Constants.CHARGE_OFF_KEY, "0");
        }

        String catCommand = "cat " + file;
        final String[] switchCommands = new String[] {
                "mount -o rw,remount " + file,
                "echo " + newState + " > " + file
        };

        if (shell != null) {
            shell.addCommand(catCommand, 0, new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (!output.get(0).equals(newState)) {
                        setChangePending();
                        shell.addCommand(switchCommands);
                    }
                }
            });
        } else {
            String recentState = Shell.SU.run(catCommand).get(0);
            if (!recentState.equals(newState)) {
                setChangePending();
                Shell.SU.run(switchCommands);
            }
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
        Toast.makeText(context, R.string.stats_reset_success, Toast.LENGTH_LONG).show();
    }

    public static void setLimit(int limit, SharedPreferences settings) {
        int max = settings.getInt(LIMIT, 80);
        // calculate new recharge threshold from previous distance
        int min = Math.max(0, limit - (max - settings.getInt(MIN, max - 2)));
        settings.edit().putInt(LIMIT, limit).putInt(MIN, min).apply();
    }

    public static void handleLimitChange(Context context, Object newLimit) {
        try {
            int limit;
            if (newLimit instanceof Number) {
                limit = ((Number) newLimit).intValue();
            } else {
                limit = Integer.parseInt(newLimit.toString());
            }
            if (limit == 100) {
                SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
                disableService(context);
                settings.edit().putBoolean(ENABLE, false).apply();
            } else if (40 <= limit && limit <= 99) {
                SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
                // set the new limit
                setLimit(limit, settings);
                Toast.makeText(context, context.getString(R.string.intent_limit_accepted, limit),
                        Toast.LENGTH_SHORT).show();
                if (settings.getBoolean(NOTIFICATION_LIVE, false)) {
                    // "restart" service if necessary
                    context.startService(new Intent(context, ForegroundService.class));
                } else {
                    settings.edit().putBoolean(ENABLE, true).apply();
                    enableService(context);
                }
            } else {
                throw new NumberFormatException("Battery limit out of range!");
            }
        } catch (NumberFormatException fe) {
            Toast.makeText(context, R.string.intent_limit_invalid, Toast.LENGTH_LONG).show();
        }
    }

    public static void enableService(Context context){
        if (SharedMethods.isPhonePluggedIn(context)) {
            context.startService(new Intent(context, ForegroundService.class));
            // display service enabled Toast message
            Toast.makeText(context, R.string.service_enabled, Toast.LENGTH_LONG).show();
        }
    }

    public static void disableService(Context context) {
        disableService(context, true);
    }
    public static void disableService(Context context, boolean ignoreAutoReset) {
        if (ignoreAutoReset) {
            ForegroundService.ignoreAutoReset();
        }
        context.stopService(new Intent(context, ForegroundService.class));
        SharedMethods.changeState(context, null, CHARGE_ON);
        // display service disabled Toast message
        Toast.makeText(context, R.string.service_disabled, Toast.LENGTH_LONG).show();
    }

}
