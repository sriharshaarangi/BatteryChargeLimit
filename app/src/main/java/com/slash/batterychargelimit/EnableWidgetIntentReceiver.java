package com.slash.batterychargelimit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.widget.RemoteViews;

import java.util.Set;

import static com.slash.batterychargelimit.Constants.ENABLE;
import static com.slash.batterychargelimit.Constants.SETTINGS;

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
        boolean toggle = !is_enabled;
        settings.edit().putBoolean(ENABLE, toggle).apply();

//         RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_demo);
//        remoteViews.setImageViewResource(R.id.widget_image, getImageToSet());
//
//        //REMEMBER TO ALWAYS REFRESH YOUR BUTTON CLICK LISTENERS!!!
//        remoteViews.setOnClickPendingIntent(R.id.widget_button, MyWidgetProvider.buildButtonPendingIntent(context));
//
//        MyWidgetProvider.pushWidgetUpdate(context.getApplicationContext(), remoteViews);
    }
}
