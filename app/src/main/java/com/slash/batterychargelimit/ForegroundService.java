package com.slash.batterychargelimit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import static com.slash.batterychargelimit.Constants.*;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_OFF;
import static com.slash.batterychargelimit.SharedMethods.CHARGE_ON;

/**
 * Created by harsha on 30/1/17.
 */

public class ForegroundService extends Service {
    private SharedPreferences settings;
    private Context thisContext = this;
    private int flag = 0, flag2 = 0;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private int notifyID = 1;
    private static boolean ignoreAutoReset = false;

    static void ignoreAutoReset() {
        ignoreAutoReset = true;
    }

    @Override
    public void onCreate() {
        notifyID = 1;
        settings = this.getSharedPreferences(SETTINGS, 0);
        settings.edit().putBoolean(NOTIFICATION_LIVE, true).apply();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        mNotifyBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mNotifyBuilder
                .setContentTitle(getString(R.string.please_wait))
                .setContentText("")
                .setSmallIcon(R.drawable.bcl_n)
                .setContentIntent(pendingIntent)
                .build();
        mNotificationManager.notify(
                notifyID,
                mNotifyBuilder.build());
        startForeground(notifyID, notification);

        flag = 0;
        flag2 = 0;

        registerReceiver(charging, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private BroadcastReceiver charging = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int limitPercentage = settings.getInt(LIMIT, 80);
                int rechargePercentage = limitPercentage - settings.getInt(RECHARGE_DIFF, 2);

                if (SharedMethods.getBatteryLevel(thisContext) >= limitPercentage) {
                    if (flag == 0) {
                        flag2 = 1;
                        SharedMethods.changeState(thisContext, CHARGE_OFF);
                        mNotifyBuilder.setContentTitle(getString(R.string.maintaining_x_to_y,
                                rechargePercentage, limitPercentage));
                        mNotificationManager.notify(notifyID, mNotifyBuilder.build());
                        flag = 1;
                    }
                } else if (flag2 == 0) {
                    SharedMethods.changeState(thisContext, CHARGE_ON);
                    flag2 = 2;
                    flag = 0;
                    mNotifyBuilder.setContentTitle(getString(R.string.waiting_until_x, limitPercentage)).setContentText("");
                    mNotificationManager.notify(notifyID, mNotifyBuilder.build());

                    if (!SharedMethods.isPhonePluggedIn(thisContext)) {
                        thisContext.stopService(new Intent(thisContext, ForegroundService.class));
                    }
                } else if ((flag2 == 1 && SharedMethods.getBatteryLevel(thisContext) < rechargePercentage)) {
                    SharedMethods.changeState(thisContext, CHARGE_ON);
                    thisContext.stopService(new Intent(thisContext, ForegroundService.class));
                    flag2 = 2;
                    flag = 0;
                    mNotifyBuilder.setContentTitle(getString(R.string.waiting_until_x_plugged, limitPercentage)).setContentText("");
                    mNotificationManager.notify(notifyID, mNotifyBuilder.build());
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (SharedMethods.isPhonePluggedIn(thisContext)) {
                                thisContext.startService(new Intent(thisContext, ForegroundService.class));
                            }
                        }
                    }, 1000);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        if (!ignoreAutoReset && settings.getBoolean(AUTO_RESET_STATS, false)
                && SharedMethods.getBatteryLevel(this)
                > settings.getInt(LIMIT, 80) - settings.getInt(RECHARGE_DIFF, 2)) {
            SharedMethods.resetBatteryStats(this);
        }
        ignoreAutoReset = false;

        SharedMethods.changeState(thisContext, CHARGE_ON);
        settings.edit().putBoolean(NOTIFICATION_LIVE, false).apply();
        unregisterReceiver(charging);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
