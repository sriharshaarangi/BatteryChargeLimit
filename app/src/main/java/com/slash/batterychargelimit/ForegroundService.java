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

import static com.slash.batterychargelimit.Constants.LIMIT;
import static com.slash.batterychargelimit.Constants.RECHARGE_DIFF;
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
    private String limit;

    @Override
    public void onCreate() {
        notifyID = 1;
        settings = thisContext.getSharedPreferences("Settings", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("notificationLive", true);
        editor.apply();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        mNotifyBuilder = new NotificationCompat.Builder(this);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mNotifyBuilder
                .setContentTitle("Please wait.......")
                .setContentText("")
//                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.bcl_n)
                .setContentIntent(pendingIntent)
//                .setTicker(getText(R.string.ticker_text))
                .build();
        mNotificationManager.notify(
                notifyID,
                mNotifyBuilder.build());
        startForeground(notifyID, notification);

        flag = 0;
        flag2 = 0;

        registerReceiver(charging, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override//todo on change incerease/decrease auto restart
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals("connected")) {
            //onCreate called if not already started
        }
        else if (intent.getAction().equals("reset")) {
            SharedMethods.changeState(thisContext, CHARGE_ON);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("notificationLive", false);
            editor.apply();
            unregisterReceiver(charging);

            stopForeground(true);
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    BroadcastReceiver charging = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                int limitPercentage = settings.getInt(LIMIT, 80);
                int rechargePercentage = limitPercentage - settings.getInt(RECHARGE_DIFF, 2);

                if (SharedMethods.getBatteryLevel(thisContext) >= limitPercentage) {
                    if (flag == 0) {
                        flag2 = 1;
                        SharedMethods.changeState(thisContext, CHARGE_OFF);
                        mNotifyBuilder.setContentTitle("Maintaining " + Integer.toString(rechargePercentage)
                                + " - " + Integer.toString(limitPercentage) + " IF plugged");
//                                .setContentText("NOT CHARGING - Click to charge to 100%")
//                                .addAction(android.R.drawable.something, "Something", resetIntent);
//                                .setContentIntent(resetIntent);
                        mNotificationManager.notify(
                                notifyID,
                                mNotifyBuilder.build());
                        flag = 1;

                    }
                }
                else if (flag2 == 0) {
                    SharedMethods.changeState(thisContext, CHARGE_ON);
                    flag2 = 2;
                    flag = 0;
                    mNotifyBuilder.setContentTitle("Waiting until " + limit + "%")
                            .setContentText("");
//                            .addAction(android.R.drawable.something, "Something", resetIntent);
//                            .setContentIntent(resetIntent);
                    mNotificationManager.notify(
                            notifyID,
                            mNotifyBuilder.build());

                    if (!SharedMethods.isPhonePluggedIn(thisContext)) {//todo IMP
                        Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                        startIntent2.setAction("reset");//disconnected
                        thisContext.startService(startIntent2);
                    }
                }
                else if ((flag2 == 1 && SharedMethods.getBatteryLevel(thisContext) < rechargePercentage)) {
                    SharedMethods.changeState(thisContext, CHARGE_ON);
                    Intent startIntent2 = new Intent(thisContext, ForegroundService.class);
                    startIntent2.setAction("reset");
                    thisContext.startService(startIntent2);
                    flag2 = 2;
                    flag = 0;
                    mNotifyBuilder.setContentTitle("Waiting until " + limit + "% IF plugged")
                            .setContentText("");
//                                .addAction(android.R.drawable.ic_media_previous, "Previous",
//                                        resetIntent);
//                            .setContentIntent(resetIntent);
                    mNotificationManager.notify(
                            notifyID,
                            mNotifyBuilder.build());
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (SharedMethods.isPhonePluggedIn(thisContext)) {
                                Intent startIntent3 = new Intent(thisContext, ForegroundService.class);
                                startIntent3.setAction("connected");
                                thisContext.startService(startIntent3);
                            }
                        }
                    }, 1000);
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }
}
