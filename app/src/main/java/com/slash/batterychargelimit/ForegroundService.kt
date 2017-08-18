package com.slash.batterychargelimit

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.slash.batterychargelimit.activities.MainActivity
import com.slash.batterychargelimit.receivers.BatteryReceiver
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.Constants.NOTIFICATION_LIVE
import com.slash.batterychargelimit.Constants.AUTO_RESET_STATS

/**
 * Created by harsha on 30/1/17.
 *
 * This is a Service that shows the notification about the current charging state
 * and supplies the context to the BatteryReceiver it is registering.
 *
 * 24/4/17 milux: Changed to make "restart" more efficient by avoiding the need to stop the service
 */

class ForegroundService : Service() {

    private var settings: SharedPreferences? = null
    private var mNotifyBuilder: NotificationCompat.Builder? = null
    private var notifyID = 1
    private var autoResetActive = false
    private var batteryReceiver: BatteryReceiver? = null

    /**
     * Enables the automatic reset on service shutdown
     */
    fun enableAutoReset() {
        autoResetActive = true
    }

    override fun onCreate() {
        isRunning = true

        notifyID = 1
        settings = this.getSharedPreferences(SETTINGS, 0)
        settings!!.edit().putBoolean(NOTIFICATION_LIVE, true).apply()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        mNotifyBuilder = NotificationCompat.Builder(this)
        val notification = mNotifyBuilder!!
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setOngoing(true)
                .setContentTitle(getString(R.string.please_wait))
                .setSmallIcon(R.drawable.bcl_n)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(notifyID, notification)

        batteryReceiver = BatteryReceiver(this@ForegroundService)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        ignoreAutoReset = false
        return super.onStartCommand(intent, flags, startId)
    }

    fun setNotificationTitle(title: String) {
        mNotifyBuilder!!.setContentTitle(title)
    }

    fun setNotificationContentText(contentText: String) {
        mNotifyBuilder!!.setContentText(contentText)
    }

    fun updateNotification() {
        startForeground(notifyID, mNotifyBuilder!!.build())
    }

    override fun onDestroy() {
        if (autoResetActive && !ignoreAutoReset && settings!!.getBoolean(AUTO_RESET_STATS, false)) {
            SharedMethods.resetBatteryStats(this)
        }
        ignoreAutoReset = false

        settings!!.edit().putBoolean(NOTIFICATION_LIVE, false).apply()
        // unregister the battery event receiver
        unregisterReceiver(batteryReceiver)
        // make the BatteryReceiver and dependencies ready for garbage-collection
        batteryReceiver!!.detach()

        isRunning = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        /**
         * Returns whether the service is running right now
         *
         * @return Whether service is running
         */
        var isRunning = false
            private set
        private var ignoreAutoReset = false

        /**
         * Ignore the automatic reset when service is shut down the next time
         */
        internal fun ignoreAutoReset() {
            ignoreAutoReset = true
        }
    }
}
