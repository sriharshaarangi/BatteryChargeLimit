package com.slash.batterychargelimit;

import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.View;
import android.widget.*;

import java.io.File;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

public class MainActivity extends AppCompatActivity {
    private Button limit_Button;
    private SeekBar rangeBar;
    private Switch enable_Switch;
    private TextView status_TextView;
    private EditText limit_TextView;
    private SharedPreferences settings;
    private RadioGroup batteryFile_RadioGroup;
    private Context thisContext = this;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        settings = getSharedPreferences("Settings", 0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.previously_started), false);
        if (!previouslyStarted) {
            prefs.edit().putBoolean(getString(R.string.previously_started), true).apply();
            settings.edit()
                    .putInt(LIMIT, 80)
                    .putBoolean("limitReached", false)
                    .putBoolean("enable", false).apply();
            SharedMethods.whitlelist(thisContext);
        }

        int settingsVersion = prefs.getInt("SettingsVersion", 0);
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace(); // should never happen
        }
        if (settingsVersion < versionCode) {
            switch(settingsVersion) {
                case 0:
                    SparseArray<String[]> modes = getValidCtrlFiles();
                    if (modes.size() == 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setMessage("Your device is not supported (yet)!")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    setCtrlFile(modes.valueAt(0));
                case 4:
                    // settings upgrade for future version(s)
            }
            // update the settings version
            prefs.edit().putInt("SettingsVersion", versionCode).apply();
        }

        int limit_percentage = settings.getInt(LIMIT, 80);
        boolean is_enabled = settings.getBoolean("enable", false);

        if (is_enabled && SharedMethods.isPhonePluggedIn(thisContext)) {
            Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
            startIntent2.setAction("connected");
            thisContext.startService(startIntent2);
        }//notif is 1!

        batteryFile_RadioGroup = (RadioGroup) findViewById(R.id.rgOpinion);
        limit_Button = (Button) findViewById(R.id.changeLimitButton);
        enable_Switch = (Switch) findViewById(R.id.button1);
        limit_TextView = (EditText) findViewById(R.id.limit_EditText);
        status_TextView = (TextView) findViewById(R.id.status);
        Button resetBatteryStats_Button = (Button) findViewById(R.id.reset_battery_stats);
        rangeBar = (SeekBar) findViewById(R.id.range_bar);
        final TextView rangeText = (TextView) findViewById(R.id.range_text);

        limit_TextView.setEnabled(false);
        limit_TextView.setText(Integer.toString(limit_percentage));
        int rechargeDiff = settings.getInt(RECHARGE_DIFF, 2);
        rangeBar.setProgress(rechargeDiff);
        rangeText.setText("Recharge below " + (limit_percentage - rechargeDiff) + "%");
        enable_Switch.setChecked(is_enabled);
        updateRadioButtons();

        // if limit is enabled, disable all editable settings
        if (is_enabled) {
            limit_Button.setEnabled(false);
            rangeBar.setEnabled(false);
        }

        enable_Switch.setOnCheckedChangeListener(switchListener);
        limit_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = limit_Button.getText().toString();
                if (s.equals("Apply")) {
                    int t = Integer.parseInt(limit_TextView.getText().toString());
                    if (t >= 40 && t <= 99) {
                        settings.edit().putInt(LIMIT, t).apply();
                        if (enable_Switch.isChecked()) {
                            enable_Switch.toggle();
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    enable_Switch.toggle();
                                }
                            }, 1000);
                        }
                    } else {
                        t = settings.getInt(LIMIT, 80);
                        SharedMethods.toastMessage(thisContext, "Not valid");
                    }
                    int limit = settings.getInt(LIMIT, 80);
                    limit_TextView.setText(Integer.toString(limit));
                    rangeText.setText("Recharge below " + (limit - settings.getInt(RECHARGE_DIFF, 2)) + "%");
                    limit_Button.setText("Change");
                    limit_TextView.setEnabled(false);
                }
                else if (s.equals("Change")) {
                    limit_TextView.setEnabled(true);
                    limit_Button.setText("Apply");
                }
            }
        });
        rangeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int diff, boolean fromUser) {
                settings.edit().putInt(RECHARGE_DIFF, diff).apply();
                rangeText.setText("Recharge below " + (settings.getInt(LIMIT, 80) - diff) + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        resetBatteryStats_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedMethods.resetBatteryStats(thisContext);
            }
        });
        IntentFilter percentage = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(charging, percentage);
    }

    //oncheckedchangelistener for switches
    CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            //looks very strange when limit_TextView is yet to be clicked, see below
//            enable_Switch.setEnabled(false);
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    enable_Switch.setEnabled(true);
//                }
//            }, 500);

            settings.edit().putBoolean("enable", isChecked).apply();

            if (isChecked) {
                if (SharedMethods.isPhonePluggedIn(thisContext)) {
                    if (SharedMethods.getBatteryLevel(thisContext) >= settings.getInt(LIMIT, 80)) {
                        settings.edit().putBoolean("limitReached", true).apply();
                    }
                    Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                    startIntent2.setAction("connected");//todo even when disconnected
                    thisContext.startService(startIntent2);
                }

                if (limit_TextView.isEnabled()) {
                    limit_Button.performClick();
                    limit_TextView.setEnabled(false);
                }
            } else {
                boolean is_notificationLive = settings.getBoolean("notificationLive", false);
                if (is_notificationLive) {
                    Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                    startIntent2.setAction("reset");//todo even when disconnected
                    thisContext.startService(startIntent2);
                }
                SharedMethods.changeState(thisContext, CHARGE_ON);
            }

            updateRadioButtons();
            limit_Button.setEnabled(!isChecked);
            rangeBar.setEnabled(!isChecked);
        }
    };

    /**
     * This listener is bound via XML!
     *
     * @param view The view
     */
    public void onRadioButtonClicked(View view) {
        if (((RadioButton) view).isChecked()) {
            setCtrlFile(getValidCtrlFiles().get(view.getId()));
        }
    }

    private SparseIntArray getCtrlFileMapping() {
        SparseIntArray m = new SparseIntArray();
        m.put(R.id.batt_slate_mode, R.array.batt_slate_mode);
        m.put(R.id.store_mode, R.array.store_mode);
        m.put(R.id.battery_charging_enabled, R.array.battery_charging_enabled);
        m.put(R.id.charging_enabled, R.array.charging_enabled);
        return m;
    }

    private SparseArray<String[]> ctrlFiles = null;
    private SparseArray<String[]> getValidCtrlFiles() {
        if (ctrlFiles == null) {
            Resources res = getResources();
            SparseIntArray map = getCtrlFileMapping();
            SparseArray<String[]> modeMap = new SparseArray<>();
            for (int i = 0; i < map.size(); i++) {
                String[] mode = res.getStringArray(map.valueAt(i));
                if (new File(mode[FILE_INDEX]).exists()) {
                    modeMap.put(map.keyAt(i), mode);
                }
            }
            ctrlFiles = modeMap;
        }
        return ctrlFiles;
    }

    private void updateRadioButtons() {
        if (settings.getBoolean("enable", false)) {
            for (int i = 0; i < batteryFile_RadioGroup.getChildCount(); i++) {
                batteryFile_RadioGroup.getChildAt(i).setEnabled(false);
            }
        } else {
            boolean first = false;
            SparseArray<String[]> ctrlFiles = getValidCtrlFiles();
            for (int i = 0; i < batteryFile_RadioGroup.getChildCount(); i++) {
                RadioButton b = (RadioButton) batteryFile_RadioGroup.getChildAt(i);
                String[] mode = ctrlFiles.get(b.getId());
                if (mode == null) {
                    b.setEnabled(false);
                } else {
                    b.setEnabled(true);
                    String currentCtrlFile = settings.getString(FILE_KEY, "");
                    b.setChecked(currentCtrlFile.equals(mode[FILE_INDEX]));
                }
            }
        }
    }

    private void setCtrlFile(String[] mode) {
        getSharedPreferences("Settings", 0)
                .edit().putString(FILE_KEY, mode[FILE_INDEX])
                .putString(CHARGE_ON_KEY, mode[CHARGE_ON_INDEX])
                .putString(CHARGE_OFF_KEY, mode[CHARGE_OFF_INDEX]).apply();
    }

    //to update battery status on UI
    BroadcastReceiver charging = new BroadcastReceiver() {
        private int previousStatus = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                //in a function
                Intent batteryIntent2 = thisContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int currentStatus = batteryIntent2.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                //in a function
                if (currentStatus != previousStatus) {
                    previousStatus = currentStatus;
                    if (currentStatus == 2) {
                        status_TextView.setText("CHARGING");
                        status_TextView.setTextColor(Color.parseColor("#4CAF50"));
                    } else if (currentStatus == 3) {
                        status_TextView.setText("DISCHARGING");
                        status_TextView.setTextColor(Color.DKGRAY);
                    } else if (currentStatus == 5) {
                        status_TextView.setText("FULL");
                    } else if (currentStatus == 4) {
                        status_TextView.setText("NOT CHARGING");
                    } else if (currentStatus == 1) {
                        status_TextView.setText("UNKOWN");
                    }
                }
            }
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onStop() {
        super.onStop();
        unregisterReceiver(charging);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        IntentFilter percentage = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(charging, percentage);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
