package com.slash.batterychargelimit.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.slash.batterychargelimit.R;
import com.slash.batterychargelimit.SharedMethods;
import eu.chainfire.libsuperuser.Shell;

import static com.slash.batterychargelimit.Constants.ENABLE;
import static com.slash.batterychargelimit.Constants.SETTINGS;
import static com.slash.batterychargelimit.Constants.INTENT_TOGGLE_ACTION;
import static com.slash.batterychargelimit.EnableWidget.buildButtonPendingIntent;
import static com.slash.batterychargelimit.EnableWidget.pushWidgetUpdate;

/**
 * Created by harsha on 5/5/17.
 */

public class EnableWidgetIntentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(INTENT_TOGGLE_ACTION)){
            SharedPreferences settings = context.getSharedPreferences(SETTINGS, 0);
            if (Shell.SU.available()) {
                boolean enable = !settings.getBoolean(ENABLE, false);
                settings.edit().putBoolean(ENABLE, enable).apply();
                if(enable) {
                    SharedMethods.startService(context);
                } else {
                    SharedMethods.stopService(context);
                }
                updateWidget(context, enable);
            } else {
                Toast.makeText(context, R.string.root_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void updateWidget(Context context, boolean enable){
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.enable);

        remoteViews.setImageViewResource(R.id.enable, getImage(enable));
        remoteViews.setOnClickPendingIntent(R.id.enable, buildButtonPendingIntent(context));

        pushWidgetUpdate(context, remoteViews);
    }

    public static int getImage(boolean enabled){
        if (enabled) {
            return R.drawable.widget_enabled;
        } else {
            return R.drawable.widget_disabled;
        }
    }
}
