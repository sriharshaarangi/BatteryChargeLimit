package com.slash.batterychargelimit;

import android.content.*;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.slash.batterychargelimit.settings.SettingsFragment;
import eu.chainfire.libsuperuser.Shell;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 17/3/17.
 */

public class SharedMethods {
    private static final String TAG = SharedMethods.class.getSimpleName();
    public final static int CHARGE_ON = 0, CHARGE_OFF = 1;

    // remember pending state change
    private static long changePending = 0;

    /**
     * Inform the BatteryReceiver instance(es) to ignore events for CHARGING_CHANGE_TOLERANCE_MS,
     * in order to let the state change settle.
     */
    private static void setChangePending() {
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

    private static final Shell.Interactive suShell = new Shell.Builder().setWantSTDERR(false).useSU().open();
    public static Shell.Interactive getSuShell() {
        return suShell;
    }

    /**
     * Helper function to wait for the su shell in a blocking fashion (max. 3 seconds).
     * Chainfire considers this a bad practice, so use wisely!
     */
    public static void waitForShell() {
        try {
            getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    suShell.waitForIdle();
                }
            }).get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            Log.wtf(TAG, e);
        } catch (TimeoutException e) {
            Log.w(TAG, "Timeout: Shell blocked more than 3 seconds, continue app execution.", e);
        }
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static ExecutorService getExecutor() {
        return executor;
    }


    public static void changeState(@NonNull Context context, final int chargeMode) {
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

        getSuShell().addCommand(catCommand, 0, new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (!output.get(0).equals(newState)) {
                    setChangePending();
                    getSuShell().addCommand(switchCommands);
                }
            }
        });
    }

    private static List<ControlFile> ctrlFiles = null;
    public static List<ControlFile> getCtrlFiles(Context context) {
        if (ctrlFiles == null) {
            try {
                Reader r = new InputStreamReader(context.getResources().openRawResource(R.raw.control_files),
                        Charset.forName("UTF-8"));
                Gson gson = new Gson();
                ctrlFiles = gson.fromJson(r, new TypeToken<List<ControlFile>>(){}.getType());
            } catch (Exception e) {
                Log.wtf(context.getClass().getSimpleName(), e);
                return Collections.emptyList();
            }
        }
        return ctrlFiles;
    }
    public static void validateCtrlFiles(Context context) {
        for (ControlFile cf : getCtrlFiles(context)) {
            cf.validate();
        }
    }

    public static void setCtrlFile(Context context, ControlFile cf) {
        //This will immediately reset the current control file
        SharedMethods.stopService(context, true);
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(SettingsFragment.KEY_CONTROL_FILE, cf.getFile()).apply();
        context.getSharedPreferences(SETTINGS, 0)
                .edit().putString(FILE_KEY, cf.getFile())
                .putString(CHARGE_ON_KEY, cf.getChargeOn())
                .putString(CHARGE_OFF_KEY, cf.getChargeOff()).apply();
        //Respawn the service if necessary
        SharedMethods.startService(context);
    }

    public static boolean isPhonePluggedIn(Context context) {
        final Intent batteryIntent = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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

    public static String getBatteryInfo(@NonNull Context context, @NonNull Intent intent, boolean useFahrenheit) {
        int batteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        int batteryTemperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        return context.getString(useFahrenheit ? R.string.battery_info_F : R.string.battery_info_C,
                (float) batteryVoltage / 1000.f,
                useFahrenheit ? 32.f + batteryTemperature * 1.8f / 10.f : batteryTemperature / 10.f);
    }

    public static void resetBatteryStats(final Context context) {
//        try {
//            // new technique for PureNexus-powered devices
//            Class<?> helperClass = Class.forName("com.android.internal.os.BatteryStatsHelper");
//            Constructor<?> constructor = helperClass.getConstructor(Context.class, boolean.class, boolean.class);
//            Object instance = constructor.newInstance(context, false, false);
//            Method createMethod = helperClass.getMethod("create", Bundle.class);
//            createMethod.invoke(instance, (Bundle) null);
//            Method resetMethod = helperClass.getMethod("resetStatistics");
//            resetMethod.invoke(instance);
//        } catch (Exception e) {
//            Log.i("New reset method failed", e.getMessage(), e);
//            // on Exception, fall back to conventional method
            getSuShell().addCommand("dumpsys batterystats --reset", 0, new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    if (exitCode == 0) {
                        Toast.makeText(context, R.string.stats_reset_success, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Statistics reset failed");
                    }
                }
            });
//        }
    }

    public static void setLimit(int limit, SharedPreferences settings) {
        int max = settings.getInt(LIMIT, 80);
        // calculate new recharge threshold from previous distance
        int min = Math.max(0, limit - (max - settings.getInt(MIN, max - 2)));
        settings.edit().putInt(LIMIT, limit).putInt(MIN, min).apply();
    }

    static void handleLimitChange(Context context, Object newLimit) {
        try {
            int limit;
            if (newLimit instanceof Number) {
                limit = ((Number) newLimit).intValue();
            } else {
                limit = Integer.parseInt(newLimit.toString());
            }
            if (limit == 100) {
                SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
                stopService(context);
                settings.edit().putBoolean(ENABLE, false).apply();
            } else if (40 <= limit && limit <= 99) {
                SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
                // set the new limit
                setLimit(limit, settings);
                Toast.makeText(context, context.getString(R.string.intent_limit_accepted, limit),
                        Toast.LENGTH_SHORT).show();
                if (!settings.getBoolean(NOTIFICATION_LIVE, false)) {
                    settings.edit().putBoolean(ENABLE, true).apply();
                    startService(context);
                }
            } else {
                throw new NumberFormatException("Battery limit out of range!");
            }
        } catch (NumberFormatException fe) {
            Toast.makeText(context, R.string.intent_limit_invalid, Toast.LENGTH_LONG).show();
        }
    }

    public static void startService(final Context context) {
        if (context.getSharedPreferences(SETTINGS, 0).getBoolean(ENABLE, false)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (SharedMethods.isPhonePluggedIn(context)) {
                        context.startService(new Intent(context, ForegroundService.class));
                        // display service enabled Toast message
                        Toast.makeText(context, R.string.service_enabled, Toast.LENGTH_LONG).show();
                    }
                }
            }, CHARGING_CHANGE_TOLERANCE_MS);
        }
    }

    public static void stopService(final Context context) {
        stopService(context, true);
    }
    public static void stopService(final Context context, boolean ignoreAutoReset) {
        if (ignoreAutoReset) {
            ForegroundService.ignoreAutoReset();
        }
        context.stopService(new Intent(context, ForegroundService.class));
        SharedMethods.changeState(context, CHARGE_ON);
        // display service disabled Toast message
        Toast.makeText(context, R.string.service_disabled, Toast.LENGTH_LONG).show();
    }

}
