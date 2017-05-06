package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Set;

import static com.slash.batterychargelimit.Constants.ENABLE;
import static com.slash.batterychargelimit.Constants.SETTINGS;
import static com.slash.batterychargelimit.EnableWidget.buildButtonPendingIntent;
import static com.slash.batterychargelimit.EnableWidget.pushWidgetUpdate;

/**
 * Created by harsha on 5/5/17.
 */

public class EnableWidgetIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("com.slash.batterychargelimit.TOGGLE")){
            toggle(context);
        }
    }

    private void toggle(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
        boolean is_enabled = settings.getBoolean(ENABLE, false);
        boolean now_enabled = !is_enabled;

        if(now_enabled)
            SharedMethods.serviceEnabled(context);
        else
            SharedMethods.serviceDisabled(context);

        settings.edit().putBoolean(ENABLE, now_enabled).apply();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.enable);

//        remoteViews.setImageViewResource(R.id.widget_image, getImageToSet());
        remoteViews.setOnClickPendingIntent(R.id.enable, buildButtonPendingIntent(context));

        pushWidgetUpdate(context, remoteViews);
    }
}
