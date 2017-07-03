package com.slash.batterychargelimit;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import eu.chainfire.libsuperuser.Shell;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import static com.slash.batterychargelimit.Constants.*;

public class MainActivity extends AppCompatActivity {
//    private SeekBar rangeBar;
    private NumberPicker minPicker;
    private TextView minText;
    private NumberPicker maxPicker;
    private TextView maxText;
    private SharedPreferences settings;
    private TextView status_TextView;
    private TextView batteryInfo;
    private RadioGroup batteryFile_RadioGroup;
    private Switch enable_Switch;
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
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.previously_started), false);
        if (!previouslyStarted) {
            // whitelist App for Doze Mode
            Shell.SU.run("dumpsys deviceidle whitelist +com.slash.batterychargelimit");
            prefs.edit().putBoolean(getString(R.string.previously_started), true).apply();
        }

        int settingsVersion = prefs.getInt(SETTINGS_VERSION, 0);
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(); // should never happen
        }
        if (settingsVersion < versionCode) {
            switch(settingsVersion) {
                case 0:
                    boolean found = false;
                    for (ControlFile cf : getCtrlFiles()) {
                        if (cf.isValid()) {
                            setCtrlFile(cf);
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
                        return;
                    }
                case 1: case 2: case 3: case 4:
                    if (settings.contains("limit_reached")) {
                        settings.edit().remove("limit_reached").apply();
                    }
                case 5: case 6: case 7: case 8: case 9: case 10: case 11: case 12: case 13:
                    if (settings.contains("recharge_threshold")) {
                        int limit = settings.getInt(LIMIT, 80);
                        int diff = settings.getInt("recharge_threshold", limit - 2);
                        settings.edit().putInt(MIN, limit - diff).remove("recharge_threshold").apply();
                    }
                case 14:
                    // settings upgrade for future version(s)
            }
            // update the settings version
            prefs.edit().putInt(SETTINGS_VERSION, versionCode).apply();
        }

        boolean is_enabled = settings.getBoolean(ENABLE, false);

        if (is_enabled && SharedMethods.isPhonePluggedIn(this)) {
            this.startService(new Intent(this, ForegroundService.class));
        }

        batteryFile_RadioGroup = (RadioGroup) findViewById(R.id.rgOpinion);
        enable_Switch = (Switch) findViewById(R.id.enable_switch);
        maxPicker = (NumberPicker) findViewById(R.id.max_picker);
        maxText = (TextView) findViewById(R.id.max_text);
        status_TextView = (TextView) findViewById(R.id.status);
        batteryInfo = (TextView) findViewById(R.id.battery_info);
        final Button resetBatteryStats_Button = (Button) findViewById(R.id.reset_battery_stats);
        minPicker = (NumberPicker) findViewById(R.id.min_picker);
//        rangeBar = (SeekBar) findViewById(R.id.range_bar);
        minText = (TextView) findViewById(R.id.min_text);
        final Switch autoResetSwitch = (Switch) findViewById(R.id.auto_stats_reset);

        updateRadioButtons(true);
        autoResetSwitch.setChecked(settings.getBoolean(AUTO_RESET_STATS, false));
        maxPicker.setMinValue(40);
        maxPicker.setMaxValue(99);
        minPicker.setMinValue(0);

        // if limit is enabled, disable all editable settings
        if (is_enabled) {
            maxPicker.setEnabled(false);
            minPicker.setEnabled(false);
        }

        enable_Switch.setOnCheckedChangeListener(switchListener);
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
    private CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
        Context context = MainActivity.this;
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                SharedMethods.enableService(context);
            } else {
                SharedMethods.disableService(context);
            }

            settings.edit().putBoolean(ENABLE, isChecked).apply();

            maxPicker.setEnabled(!isChecked);
            minPicker.setEnabled(!isChecked);
            updateRadioButtons(false);

            EnableWidgetIntentReceiver.updateWidget(context, isChecked);
        }
    };

    private List<ControlFile> ctrlFiles = null;
    private List<ControlFile> getCtrlFiles() {
        if (ctrlFiles == null) {
            try {
                Reader r = new InputStreamReader(getResources().openRawResource(R.raw.control_files),
                        Charset.forName("UTF-8"));
                Gson gson = new Gson();
                ctrlFiles = gson.fromJson(r, new TypeToken<List<ControlFile>>(){}.getType());
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        }
        return ctrlFiles;
    }

    private View.OnClickListener radioButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setCtrlFile(getCtrlFiles().get((int) v.getTag(R.id.radio_index_tag)));
        }
    };

    private static ColorStateList radioDefaultColors = null;
    private void updateRadioButtons(boolean init) {
        if (radioDefaultColors == null) {
            synchronized (MainActivity.class) {
                if (radioDefaultColors == null) {
                    radioDefaultColors = new RadioButton(this).getTextColors();
                }
            }
        }
        List<ControlFile> ctrlFiles = getCtrlFiles();
        String currentCtrlFile = settings.getString(FILE_KEY, "");
        boolean serviceEnabled = settings.getBoolean(ENABLE, false);
        for (int i = 0; i < ctrlFiles.size(); i++) {
            RadioGroup.LayoutParams layoutParams = init ? new RadioGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT) : null;
            ControlFile cf = ctrlFiles.get(i);
            RadioButton b;
            if (init) {
                b = new RadioButton(this);
                b.setId(View.generateViewId());
                b.setTag(R.id.radio_index_tag, i);
                if (cf.isExperimental()) {
                    b.setText(getString(R.string.experimental, cf.getLabel()));
                } else {
                    b.setText(cf.getLabel());
                }
                b.setChecked(currentCtrlFile.equals(cf.getFile()));
                b.setOnClickListener(radioButtonListener);
                batteryFile_RadioGroup.addView(b, i, layoutParams);
            } else {
                b = (RadioButton) batteryFile_RadioGroup.getChildAt(i);
            }
            // if service is not active, enable control file radio button if the control file is valid
            b.setEnabled(!serviceEnabled && cf.isValid());
            // color experimental control files red
            if (cf.isExperimental() && !serviceEnabled) {
                b.setTextColor(Color.RED);
            } else {
                b.setTextColor(radioDefaultColors);
            }
        }
    }

    private void setCtrlFile(ControlFile cf) {
        getSharedPreferences(SETTINGS, 0)
                .edit().putString(FILE_KEY, cf.getFile())
                .putString(CHARGE_ON_KEY, cf.getChargeOn())
                .putString(CHARGE_OFF_KEY, cf.getChargeOff()).apply();
    }

    //to update battery status on UI
    private BroadcastReceiver charging = new BroadcastReceiver() {
        private int previousStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;

        @Override
        public void onReceive(Context context, Intent intent) {
            int currentStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            float batteryVoltage = (float) intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.f;
            float batteryCelsius = (float) intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10.f;
            if (currentStatus != previousStatus && status_TextView != null) {
                previousStatus = currentStatus;
                if (currentStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    status_TextView.setText(R.string.charging);
                    status_TextView.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.darkGreen));
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_DISCHARGING) {
                    status_TextView.setText(R.string.discharging);
                    status_TextView.setTextColor(Color.DKGRAY);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_FULL) {
                    status_TextView.setText(R.string.full);
                    status_TextView.setTextColor(Color.BLACK);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING) {
                    status_TextView.setText(R.string.not_charging);
                    status_TextView.setTextColor(Color.BLACK);
                } else if (currentStatus == BatteryManager.BATTERY_STATUS_UNKNOWN) {
                    status_TextView.setText(R.string.unknown);
                    status_TextView.setTextColor(Color.BLACK);
                }
            }
            batteryInfo.setText(" (" + getString(R.string.battery_info, batteryVoltage, batteryCelsius) + ")");
        }
    };


    
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
        enable_Switch.setChecked(settings.getBoolean(ENABLE, false));
        int max = settings.getInt(LIMIT, 80);
        int min = settings.getInt(MIN, max - 2);
        maxPicker.setValue(max);
        maxText.setText(getString(R.string.limit, max));
        minPicker.setMaxValue(max);
        minPicker.setValue(min);
        updateMinText(min);
        updateRadioButtons(false);
    }
}
