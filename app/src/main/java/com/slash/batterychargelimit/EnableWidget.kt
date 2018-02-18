package com.slash.batterychargelimit

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.slash.batterychargelimit.receivers.EnableWidgetIntentReceiver
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.Constants.ENABLE
import com.slash.batterychargelimit.Constants.INTENT_TOGGLE_ACTION

class EnableWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_button)
        val settings = context.getSharedPreferences(SETTINGS, 0)
        val is_enabled = settings.getBoolean(ENABLE, false)
        remoteViews.setImageViewResource(R.id.enable, EnableWidgetIntentReceiver.getImage(is_enabled))
        remoteViews.setOnClickPendingIntent(R.id.enable, buildButtonPendingIntent(context))

        pushWidgetUpdate(context, remoteViews)
    }

    companion object {

        fun buildButtonPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(context, 0,
                    Intent().setAction(INTENT_TOGGLE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT)
        }

        fun pushWidgetUpdate(context: Context, remoteViews: RemoteViews) {
            val myWidget = ComponentName(context, EnableWidget::class.java)
            val manager = AppWidgetManager.getInstance(context)
            manager.updateAppWidget(myWidget, remoteViews)
        }
    }
}
