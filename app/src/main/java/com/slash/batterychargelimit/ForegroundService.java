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

/**
 * Created by harsha on 30/1/17.
 */

public class ForegroundService extends Service {
    SharedPreferences settings;
    Context thisContext = this;
    int flag = 0, flag2 = 0;
    Notification notification;
    NotificationCompat.Builder mNotifyBuilder;
    NotificationManager mNotificationManager;
    int notifyID = 1;
    int limitPercentage;
    String limit;

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
        notification = mNotifyBuilder
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

//        if (!settings.contains("limit")) {
//            editor.putInt("limit", 80);
//        }
        limitPercentage = settings.getInt("limit", 80);
        limit = Integer.toString(limitPercentage);

        flag = 0;
        flag2 = 0;

        IntentFilter percentage = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(charging, percentage);
    }

    @Override//todo on change incerease/decrease auto restart
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals("connected")) {
            //onCreate called if not already started
        }
        else if (intent.getAction().equals("reset")) {
            SharedMethods.changeState(thisContext, "1");
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
                limitPercentage = settings.getInt("limit", 80);
                if (SharedMethods.getBatteryLevel(thisContext) >= limitPercentage) {
                    if (flag == 0) {
                        flag2 = 1;
                        SharedMethods.changeState(thisContext, "0");
                        mNotifyBuilder.setContentTitle("Maintaining " + Integer.toString(limitPercentage - 2) + " - " + Integer.toString(limitPercentage) + " IF plugged");
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
                    SharedMethods.changeState(thisContext, "1");
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
                else if ((flag2 == 1 && SharedMethods.getBatteryLevel(thisContext) < limitPercentage - 2)) {
                    SharedMethods.changeState(thisContext, "1");
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
                else {

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
