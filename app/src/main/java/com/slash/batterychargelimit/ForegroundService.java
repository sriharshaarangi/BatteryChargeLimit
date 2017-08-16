package com.slash.batterychargelimit;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import com.slash.batterychargelimit.activities.MainActivity;
import com.slash.batterychargelimit.receivers.BatteryReceiver;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by harsha on 30/1/17.
 *
 * This is a Service that shows the notification about the current charging state
 * and supplies the context to the BatteryReceiver it is registering.
 *
 * 24/4/17 milux: Changed to make "restart" more efficient by avoiding the need to stop the service
 */

public class ForegroundService extends Service {
    private static boolean running = false;
    private static boolean ignoreAutoReset = false;

    private SharedPreferences settings = null;
    private NotificationCompat.Builder mNotifyBuilder = null;
    private int notifyID = 1;
    private boolean autoResetActive = false;
    private BatteryReceiver batteryReceiver = null;

    /**
     * Returns whether the service is running right now
     *
     * @return Whether service is running
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Ignore the automatic reset when service is shut down the next time
     */
    static void ignoreAutoReset() {
        ignoreAutoReset = true;
    }

    /**
     * Enables the automatic reset on service shutdown
     */
    public void enableAutoReset() {
        autoResetActive = true;
    }

    @Override
    public void onCreate() {
        running = true;

        notifyID = 1;
        settings = this.getSharedPreferences(SETTINGS, 0);
        settings.edit().putBoolean(NOTIFICATION_LIVE, true).apply();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mNotifyBuilder = new NotificationCompat.Builder(this);
        Notification notification = mNotifyBuilder
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setOngoing(true)
                .setContentTitle(getString(R.string.please_wait))
                .setSmallIcon(R.drawable.bcl_n)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(notifyID, notification);

        batteryReceiver = new BatteryReceiver(ForegroundService.this);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ignoreAutoReset = false;
        return super.onStartCommand(intent, flags, startId);
    }

    public void setNotificationTitle(String title) {
        mNotifyBuilder.setContentTitle(title);
    }

    public void setNotificationContentText(String contentText) {
        mNotifyBuilder.setContentText(contentText);
    }

    public void updateNotification() {
        startForeground(notifyID, mNotifyBuilder.build());
    }

    @Override
    public void onDestroy() {
        if (autoResetActive && !ignoreAutoReset && settings.getBoolean(AUTO_RESET_STATS, false)) {
            SharedMethods.resetBatteryStats(this);
        }
        ignoreAutoReset = false;

        settings.edit().putBoolean(NOTIFICATION_LIVE, false).apply();
        // unregister the battery event receiver
        unregisterReceiver(batteryReceiver);
        // make the BatteryReceiver and dependencies ready for garbage-collection
        batteryReceiver.detach();

        running = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
