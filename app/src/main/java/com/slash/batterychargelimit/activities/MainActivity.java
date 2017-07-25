package com.slash.batterychargelimit.activities;

import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.slash.batterychargelimit.*;
import com.slash.batterychargelimit.settings.CtrlFileHelper;
import com.slash.batterychargelimit.settings.SettingsFragment;
import com.slash.batterychargelimit.receivers.EnableWidgetIntentReceiver;
import eu.chainfire.libsuperuser.Shell;

import java.util.List;

import static com.slash.batterychargelimit.Constants.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private NumberPicker minPicker;
    private TextView minText;
    private NumberPicker maxPicker;
    private TextView maxText;
    private SharedPreferences settings;
    private TextView statusText;
    private TextView batteryInfo;
    private Switch enableSwitch;
    private boolean initComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Exit immediately if no root support
        if (!Shell.SU.available()) {
            Toast.makeText(this, R.string.root_denied, Toast.LENGTH_LONG );
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(R.string.root_denied)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    }).create().show();
            return;
        }

        settings = getSharedPreferences(SETTINGS, 0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (SettingsFragment.KEY_TEMP_FAHRENHEIT.equals(key)) {
                    updateBatteryInfo(getBaseContext().registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED)));
                }
            }
        });
        if (!prefs.contains(SettingsFragment.KEY_CONTROL_FILE)) {
            CtrlFileHelper.validateFiles(this, new Runnable() {
                @Override
                public void run() {
                    boolean found = false;
                    for (ControlFile cf : SharedMethods.getCtrlFiles(MainActivity.this)) {
                        if (cf.isValid()) {
                            SharedMethods.setCtrlFile(MainActivity.this, cf);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(R.string.device_not_supported)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                }).create().show();
                    }
                }
            });
        }
        if (!prefs.getBoolean(getString(R.string.previously_started), false)) {
            // whitelist App for Doze Mode
            SharedMethods.getSuShell().addCommand("dumpsys deviceidle whitelist +com.slash.batterychargelimit",
                    0, new Shell.OnCommandResultListener() {
                        @Override
                        public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                            PreferenceManager.getDefaultSharedPreferences(getBaseContext())
                                    .edit().putBoolean(getString(R.string.previously_started), true).apply();
                        }
                    });
        }

        int settingsVersion = prefs.getInt(SETTINGS_VERSION, 0);
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(TAG, e);
        }
        if (settingsVersion < versionCode) {
            switch(settingsVersion) {
                case 0: case 1: case 2: case 3: case 4:
                    if (settings.contains("limit_reached")) {
                        settings.edit().remove("limit_reached").apply();
                    }
                case 5: case 6: case 7: case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
                    if (settings.contains("recharge_threshold")) {
                        int limit = settings.getInt(LIMIT, 80);
                        int diff = settings.getInt("recharge_threshold", limit - 2);
                        settings.edit().putInt(MIN, limit - diff).remove("recharge_threshold").apply();
                    }
                case 16:
                    // settings upgrade for future version(s)
            }
            // update the settings version
            prefs.edit().putInt(SETTINGS_VERSION, versionCode).apply();
        }

        boolean is_enabled = settings.getBoolean(ENABLE, false);

        if (is_enabled && SharedMethods.isPhonePluggedIn(this)) {
            this.startService(new Intent(this, ForegroundService.class));
        }

        enableSwitch = (Switch) findViewById(R.id.enable_switch);
        maxPicker = (NumberPicker) findViewById(R.id.max_picker);
        maxText = (TextView) findViewById(R.id.max_text);
        statusText = (TextView) findViewById(R.id.status);
        batteryInfo = (TextView) findViewById(R.id.battery_info);
        final Button resetBatteryStats_Button = (Button) findViewById(R.id.reset_battery_stats);
        minPicker = (NumberPicker) findViewById(R.id.min_picker);
        minText = (TextView) findViewById(R.id.min_text);
        final Switch autoResetSwitch = (Switch) findViewById(R.id.auto_stats_reset);

        autoResetSwitch.setChecked(settings.getBoolean(AUTO_RESET_STATS, false));
        maxPicker.setMinValue(40);
        maxPicker.setMaxValue(99);
        minPicker.setMinValue(0);

        enableSwitch.setOnCheckedChangeListener(switchListener);
        maxPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldMax, int max) {
                SharedMethods.setLimit(max, settings);
                maxText.setText(getString(R.string.limit, max));
                int min = settings.getInt(MIN, max - 2);
                minPicker.setMaxValue(max);
                minPicker.setValue(min);
                updateMinText(min);
            }
        });
        minPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldMin, int min) {
                settings.edit().putInt(MIN, min).apply();
                updateMinText(min);
            }
        });
        resetBatteryStats_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.resetBatteryStats(MainActivity.this);
            }
        });
        autoResetSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settings.edit().putBoolean(AUTO_RESET_STATS, isChecked).apply();
            }
        });

        //The onCreate() process was not stopped via return, UI elements should be available
        initComplete = true;
    }

    //OnCheckedChangeListener for Switch elements
    private final CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
        final Context context = MainActivity.this;
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            settings.edit().putBoolean(ENABLE, isChecked).apply();
            if (isChecked) {
                SharedMethods.startService(context);
            } else {
                SharedMethods.stopService(context);
            }
            EnableWidgetIntentReceiver.updateWidget(context, isChecked);
        }
    };

    //to update battery status on UI
    private final BroadcastReceiver charging = new BroadcastReceiver() {
        private int previousStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;

        @Override
        public void onReceive(Context context, Intent intent) {
            int currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            if (currentStatus != previousStatus && statusText != null) {
                previousStatus = currentStatus;
                if (currentStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    statusText.setText(R.string.charging);
                    statusText.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.darkGreen));
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    statusText.setText(R.string.discharging);
                    statusText.setTextColor(Color.DKGRAY);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_FULL) {
                    statusText.setText(R.string.full);
                    statusText.setTextColor(Color.BLACK);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    statusText.setText(R.string.not_charging);
                    statusText.setTextColor(Color.BLACK);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    statusText.setText(R.string.unknown);
                    statusText.setTextColor(Color.BLACK);
                }
            }
            updateBatteryInfo(intent);
        }
    };

    private void updateBatteryInfo(Intent intent) {
        batteryInfo.setText(" (" + SharedMethods.getBatteryInfo(this, intent,
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(SettingsFragment.KEY_TEMP_FAHRENHEIT, false)) + ")");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.about:
                Intent intent = new Intent(this, About.class);
                this.startActivity(intent);
                break;
            case R.id.action_settings:
                if (!SettingsFragment.settingsVisible()) {
                    CtrlFileHelper.validateFiles(this, new Runnable() {
                        @Override
                        public void run() {
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, new SettingsFragment())
                                    .addToBackStack(null).commit();
                        }
                    });
                }
                break;
        }
        return true;
    }

    @Override
    public void onStop() {
        if(initComplete) {
            unregisterReceiver(charging);
        }
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(initComplete) {
            registerReceiver(charging, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            // the limits could have been changed by an Intent, so update the UI here
            updateUi();
        }
    }

    private void updateMinText(int min) {
        if (min == 0) {
            minText.setText(R.string.no_recharge);
        } else {
            minText.setText(getString(R.string.recharge_below, min));
        }
    }

    private void updateUi() {
        enableSwitch.setChecked(settings.getBoolean(ENABLE, false));
        int max = settings.getInt(LIMIT, 80);
        int min = settings.getInt(MIN, max - 2);
        maxPicker.setValue(max);
        maxText.setText(getString(R.string.limit, max));
        minPicker.setMaxValue(max);
        minPicker.setValue(min);
        updateMinText(min);
    }
}
