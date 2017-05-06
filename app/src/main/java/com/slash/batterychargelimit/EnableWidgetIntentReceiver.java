package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Set;

import eu.chainfire.libsuperuser.Shell;

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
            SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
            boolean is_enabled = settings.getBoolean(ENABLE, false);
            boolean now_enabled = false;
            if (Shell.SU.available()) {
                now_enabled = !is_enabled;
                toggle(context, now_enabled);
                settings.edit().putBoolean(ENABLE, now_enabled).apply();
            } else {
                Toast.makeText(context, R.string.root_denied, Toast.LENGTH_LONG).show();
            }

            updateWidget(context, now_enabled);
        }
    }

    private void toggle(Context context, boolean now_enabled) {


        if(now_enabled)
            SharedMethods.serviceEnabled(context);
        else
            SharedMethods.serviceDisabled(context);

    }
    public static void updateWidget(Context context, boolean now_enabled){
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.enable);

        remoteViews.setImageViewResource(R.id.enable, getImage(now_enabled));
        remoteViews.setOnClickPendingIntent(R.id.enable, buildButtonPendingIntent(context));

        pushWidgetUpdate(context, remoteViews);
    }
    public static int getImage(boolean now_enabled){
        if(now_enabled)
            return R.drawable.widget_enabled;
        return R.drawable.widget_disabled;
    }
}
