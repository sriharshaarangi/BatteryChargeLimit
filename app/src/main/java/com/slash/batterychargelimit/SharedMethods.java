package com.slash.batterychargelimit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by harsha on 17/3/17.
 */

public class SharedMethods {
    public static void changeState(Context context, String s) {
        Process p;
        SharedPreferences settings = context.getSharedPreferences("Settings", 0);
        try {
            // Preform su to get root privledges
            p = Runtime.getRuntime().exec("su");
            String battery_file = settings.getString("file", "charging_enabled");
            String file = "/sys/class/power_supply/battery/" + battery_file + "\n";
            if (battery_file.equals("batt_slate_mode")) {
                if (s.equals("1")) s = "0";
                else s = "1";
            }
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            os.writeBytes("cat " + file);
            os.flush();
            String tmp = bf.readLine();
//            os.writeBytes("echo \"Do I have root?\" >/system/sd/temporary.txt\n");
            if (!tmp.equals(s)) {
                if (s.equals("0")) {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("limitReached", true);
                    editor.apply();
                }
                os.writeBytes("mount -o rw,remount " + file);
                os.writeBytes("echo " + s + " > " + file);
            }
//            os.writeBytes("mount -o ro,remount /sys/class/power_supply/battery/charging_enabled\n");
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    //Code to run on success
                } else {
                    //Code to run on unsuccessful
                }
            } catch (InterruptedException e) {
                //Code to run in interrupted exception
            }
        } catch (IOException e) {
            //Code to run in input/output exception
            toastMessage(context, "App is denied ROOT access ");
        }
    }

    public static boolean isPhonePluggedIn(Context context) {
        boolean charging = false;

        final Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean batteryCharge = status == BatteryManager.BATTERY_STATUS_CHARGING;

        int chargePlug = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        if (batteryCharge) charging = true;
        if (usbCharge) charging = true;
        if (acCharge) charging = true;

        return charging;
    }

    public static void toastMessage(Context context, String message) {
        Toast.makeText(context, message,
                Toast.LENGTH_LONG).show();
    }

    public static int getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return 50;
        }
        int d = ((level * 100) / scale);
        return d;
    }

    public static boolean isConnected(Context context) {
        Intent intent = context.registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        return intent.getExtras().getBoolean("connected");
    }

    public static void resetBatteryStats(Context context){
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("dumpsys batterystats --reset\n");
            os.flush();
            toastMessage(context, "Reset Successful");
        }
        catch (IOException e) {
            toastMessage(context, "App is denied ROOT access ");
        }
    }
    public static void whitlelist(Context context){
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("dumpsys deviceidle whitelist +com.slash.batterychargelimit");
            os.flush();
        }
        catch (IOException e) {
            toastMessage(context, "App is denied ROOT access ");
        }
    }
}
