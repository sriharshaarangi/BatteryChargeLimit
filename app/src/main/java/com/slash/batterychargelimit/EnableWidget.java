package com.slash.batterychargelimit;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Created by harsha on 5/5/17.
 */

public class EnableWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.enable);
        remoteViews.setOnClickPendingIntent(R.id.enable, buildButtonPendingIntent(context));

        pushWidgetUpdate(context, remoteViews);
    }

    public static PendingIntent buildButtonPendingIntent(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.slash.batterychargelimit.TOGGLE");
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void pushWidgetUpdate(Context context, RemoteViews remoteViews) {
        ComponentName myWidget = new ComponentName(context, EnableWidget.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.updateAppWidget(myWidget, remoteViews);
    }
}
