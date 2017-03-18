package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button limit_Button;
    Button resetBatteryStats_Button;
    Switch enable_Switch;
    TextView status_TextView;
    EditText limit_TextView;
    SharedPreferences settings;
    String battery_file;
    int limit_percentage;
    boolean is_enabled;
    RadioGroup batteryFile_RadioGroup;
    Context thisContext = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        settings = getSharedPreferences("Settings", 0);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        boolean previouslyStarted = prefs.getBoolean(getString(R.string.previously_started), false);
        if(!previouslyStarted) {
            SharedPreferences.Editor edit = prefs.edit();
            edit.putBoolean(getString(R.string.previously_started), Boolean.TRUE);
            edit.apply();
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("limit", 80);
            editor.putBoolean("limitReached", false);
            editor.putBoolean("enable", false);
            if(Build.BRAND.equalsIgnoreCase("Samsung"))
                editor.putString("file", "batt_slate_mode");
            else if(Build.BRAND.equalsIgnoreCase("google"))//nexus/pixel devices
                editor.putString("file", "battery_charging_enabled");
            else
                editor.putString("file", "charging_enabled");
            editor.apply();
            SharedMethods.whitlelist(thisContext);
        }
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        battery_file = settings.getString("file", "charging_enabled");
        limit_percentage = settings.getInt("limit", 80);
        is_enabled = settings.getBoolean("enable", false);

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
        resetBatteryStats_Button = (Button) findViewById(R.id.reset_battery_stats);

        limit_TextView.setEnabled(false);
        limit_TextView.setText(Integer.toString(limit_percentage));
        enable_Switch.setChecked(is_enabled);

        int getBatteryFile = getResources().getIdentifier(battery_file, "id", getApplicationContext().getPackageName());
        RadioButton currentBatteryFile = (RadioButton) findViewById(getBatteryFile);
        currentBatteryFile.setChecked(true);

        // if limit is enabled, disable all editable settings
        if (is_enabled) {
            limit_Button.setEnabled(false);
            for (int i = 0; i < batteryFile_RadioGroup.getChildCount(); i++) {
                ((RadioButton) batteryFile_RadioGroup.getChildAt(i)).setEnabled(false);
            }
        }

        enable_Switch.setOnCheckedChangeListener(switchListener);
        limit_Button.setOnClickListener(this);
        resetBatteryStats_Button.setOnClickListener(this);
        IntentFilter percentage = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(charging, percentage);
    }

    //oncheckedchangelistener for switches
    CompoundButton.OnCheckedChangeListener switchListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                enable_Switch.setEnabled(false);

                for (int i = 0; i < batteryFile_RadioGroup.getChildCount(); i++) {
                    ((RadioButton) batteryFile_RadioGroup.getChildAt(i)).setEnabled(false);
                }

                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("enable", true);
                editor.apply();

                if (SharedMethods.isPhonePluggedIn(thisContext)) {
                    if (SharedMethods.getBatteryLevel(thisContext) >= settings.getInt("limit", 80)) {
                        SharedPreferences.Editor editor2 = settings.edit();
                        editor2.putBoolean("limitReached", true);
                        editor2.apply();
                    }                        Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                    startIntent2.setAction("connected");//todo even when disconnected
                    thisContext.startService(startIntent2);
                }

                limit_Button.setText("Change");
                limit_Button.setEnabled(false);
                limit_TextView.setEnabled(false);

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        enable_Switch.setEnabled(true);
                    }
                }, 500);
            }
            else {
                enable_Switch.setEnabled(false);

                for (int i = 0; i < batteryFile_RadioGroup.getChildCount(); i++) {
                    ((RadioButton) batteryFile_RadioGroup.getChildAt(i)).setEnabled(true);
                }
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("enable", false);
                editor.apply();
                boolean is_notificationLive = settings.getBoolean("notificationLive", false);
                if (is_notificationLive) {
                    Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                    startIntent2.setAction("reset");//todo even when disconnected
                    thisContext.startService(startIntent2);
                }
                SharedMethods.changeState(thisContext, "1");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        limit_Button.setEnabled(true);
                        enable_Switch.setEnabled(true);
                    }
                }, 500);
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.changeLimitButton:
                SharedPreferences.Editor editor = settings.edit();
                String s = limit_Button.getText().toString();
                if (s.equals("Apply")) {
                    int t = Integer.parseInt(limit_TextView.getText().toString());
                    if (t >= 40 && t <= 99) {
                        editor.putInt("limit", t);
                        editor.apply();
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
                        SharedMethods.toastMessage(thisContext, "Not valid");
                    }
                    limit_percentage = settings.getInt("limit", 80);
                    limit_TextView.setText(Integer.toString(limit_percentage));
                    limit_Button.setText("Change");
                    limit_TextView.setEnabled(false);
                }
                else if (s.equals("Change")) {
                    limit_TextView.setEnabled(true);
                    limit_Button.setText("Apply");
                }
                break;
            case R.id.reset_battery_stats:
                SharedMethods.resetBatteryStats(thisContext);
            default:
                break;
        }
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.charging_enabled:
                if (checked) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("file", "charging_enabled");
                    editor.apply();
                }
                break;
            case R.id.battery_charging_enabled:
                if (checked) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("file", "battery_charging_enabled");
                    editor.apply();
                }
                break;
            case R.id.batt_slate_mode:
                if (checked) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("file", "batt_slate_mode");
                    editor.apply();
                }
                break;
        }
    }

    int currentStatus = 1, previouStatus = 1;

    //to update battery status on UI
    BroadcastReceiver charging = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                //in a function
                Intent batteryIntent2 = thisContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                currentStatus = batteryIntent2.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                //in a function
                if (currentStatus != previouStatus) {
                    previouStatus = currentStatus;
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

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
}
