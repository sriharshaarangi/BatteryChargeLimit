package com.slash.batterychargelimit;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import eu.chainfire.libsuperuser.Shell;

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
    private SharedPreferences settings = null;
    private NotificationCompat.Builder mNotifyBuilder = null;
    private NotificationManager mNotificationManager = null;
    private int notifyID = 1;
    private static boolean ignoreAutoReset = false;
    private boolean autoResetActive = false;
    private BatteryReceiver batteryReceiver = null;
    // interactive shell for better performance
    private Shell.Interactive shell = null;

    /**
     * Ignore the automatic reset when service is shut down the next time
     */
    static void ignoreAutoReset() {
        ignoreAutoReset = true;
    }

    /**
     * Enable the automatic reset on service shutdown
     */
    public void enableAutoReset() {
        autoResetActive = true;
    }

    @Override
    public void onCreate() {
        notifyID = 1;
        ignoreAutoReset = false;
        autoResetActive = false;
        if (settings == null) {
            settings = this.getSharedPreferences(SETTINGS, 0);
        }
        settings.edit().putBoolean(NOTIFICATION_LIVE, true).apply();

        if (mNotifyBuilder == null) {
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
        } else {
            setNotification(getString(R.string.please_wait));
        }

        // create and register the receiver for the battery change events
        if (shell == null) {
            shell = new Shell.Builder().setWantSTDERR(false).useSU().open();
        }

        // it is necessary to create a new BatteryReceiver on restart
        if (batteryReceiver != null) {
            unregisterReceiver(batteryReceiver);
        }
        batteryReceiver = new BatteryReceiver(ForegroundService.this, shell);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void setNotification(String notification) {
        mNotifyBuilder.setContentTitle(notification);
        mNotificationManager.notify(notifyID, mNotifyBuilder.build());
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
        shell.close();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
