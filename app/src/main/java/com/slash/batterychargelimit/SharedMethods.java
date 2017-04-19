package com.slash.batterychargelimit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;

import java.io.IOException;
import java.util.List;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 17/3/17.
 */

public class SharedMethods {

    public static int CHARGE_ON = 0;
    public static int CHARGE_OFF = 1;

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
                        if (chargeMode == CHARGE_OFF) {
                            settings.edit().putLong(LIMIT_REACHED, System.currentTimeMillis()).apply();
                        }
                        shell.addCommand(switchCommands);
                    }
                }
            });
        } else {
            String recentState = Shell.SU.run(catCommand).get(0);
            if (!recentState.equals(newState)) {
                if (chargeMode == CHARGE_OFF) {
                    settings.edit().putLong(LIMIT_REACHED, System.currentTimeMillis()).apply();
                }
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
}
