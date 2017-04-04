package com.slash.batterychargelimit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 30/1/17.
 */

public class ForegroundService extends Service {
    private SharedPreferences settings;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManager mNotificationManager;
    private int notifyID = 1;
    private static boolean ignoreAutoReset = false;
    private BatteryReceiver batteryReceiver;

    static void ignoreAutoReset() {
        ignoreAutoReset = true;
    }

    @Override
    public void onCreate() {
        notifyID = 1;
        ignoreAutoReset = false;
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

        // create and register the receiver for the battery change events
        batteryReceiver = new BatteryReceiver(ForegroundService.this);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void setNotification(String notification) {
        mNotifyBuilder.setContentTitle(notification);
        mNotificationManager.notify(notifyID, mNotifyBuilder.build());
    }

    @Override
    public void onDestroy() {
        if (!ignoreAutoReset && settings.getBoolean(AUTO_RESET_STATS, false)
                && SharedMethods.getBatteryLevel(this)
                > settings.getInt(LIMIT, 80) - settings.getInt(RECHARGE_DIFF, 2)) {
            SharedMethods.resetBatteryStats(this);
        }
        ignoreAutoReset = false;

        settings.edit().putBoolean(NOTIFICATION_LIVE, false).apply();
        // unregister the battery event receiver
        unregisterReceiver(batteryReceiver);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
