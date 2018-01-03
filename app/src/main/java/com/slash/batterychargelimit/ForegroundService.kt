package com.slash.batterychargelimit

import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.net.Uri
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.slash.batterychargelimit.activities.MainActivity
import com.slash.batterychargelimit.receivers.BatteryReceiver
import com.slash.batterychargelimit.Constants.SETTINGS
import com.slash.batterychargelimit.Constants.NOTIFICATION_LIVE
import com.slash.batterychargelimit.Constants.AUTO_RESET_STATS
import com.slash.batterychargelimit.Constants.INTENT_DISABLE_ACTION
import com.slash.batterychargelimit.Constants.NOTIF_CHARGE
import com.slash.batterychargelimit.Constants.NOTIF_MAINTAIN

/**
 * Created by harsha on 30/1/17.
 *
 * This is a Service that shows the notification about the current charging state
 * and supplies the context to the BatteryReceiver it is registering.
 *
 * 24/4/17 milux: Changed to make "restart" more efficient by avoiding the need to stop the service
 */
class ForegroundService : Service() {

    private val settings by lazy(LazyThreadSafetyMode.NONE) {this.getSharedPreferences(SETTINGS, 0)}
    private val mNotifyBuilder by lazy(LazyThreadSafetyMode.NONE) {NotificationCompat.Builder(this)}
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
        settings.edit().putBoolean(NOTIFICATION_LIVE, true).apply()

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentApp = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val pendingIntentDisable = PendingIntent.getBroadcast(this, 0, Intent().setAction(INTENT_DISABLE_ACTION), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = mNotifyBuilder
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .addAction(0, getString(R.string.disable), pendingIntentDisable)
                .addAction(0, getString(R.string.open_app), pendingIntentApp)
                .setOngoing(true)
                .setContentTitle(getString(R.string.please_wait))
                .setContentInfo(getString(R.string.please_wait))
                .setSmallIcon(R.drawable.ic_notif_charge)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
        startForeground(notifyID, notification)

        batteryReceiver = BatteryReceiver(this@ForegroundService)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        Log.d("BatteryReceiver", "registered " + batteryReceiver!!.hashCode())

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        ignoreAutoReset = false
        return super.onStartCommand(intent, flags, startId)
    }

    fun setNotificationTitle(title: String) {
        mNotifyBuilder.setContentTitle(title)
    }

    fun setNotificationContentText(contentText: String) {
        mNotifyBuilder.setContentText(contentText)
    }

    fun setNotificationIcon(iconType: String) {
        if (iconType == NOTIF_MAINTAIN) {
            mNotifyBuilder.setSmallIcon(R.drawable.ic_notif_maintain)
        } else if (iconType == NOTIF_CHARGE) {
            mNotifyBuilder.setSmallIcon(R.drawable.ic_notif_charge)
        }
    }

    fun updateNotification() {
        startForeground(notifyID, mNotifyBuilder.build())
    }

    fun setNotificationSound(soundUri: Uri) {
        mNotifyBuilder.setSound(soundUri)
    }

    fun removeNotificationSound() {
        mNotifyBuilder.setSound(null)
    }
    override fun onDestroy() {
        if (autoResetActive && !ignoreAutoReset && settings.getBoolean(AUTO_RESET_STATS, false)) {
            SharedMethods.resetBatteryStats(this)
        }
        ignoreAutoReset = false

        settings.edit().putBoolean(NOTIFICATION_LIVE, false).apply()
        // unregister the battery event receiver
        unregisterReceiver(batteryReceiver)

        Log.d("BatteryReceiver", "unregistered " + batteryReceiver!!.hashCode())
        // make the BatteryReceiver and dependencies ready for garbage-collection
        batteryReceiver!!.detach()
        // clear the reference to the battery receiver for GC
        batteryReceiver = null

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
        private var ignoreAutoReset = false

        /**
         * Ignore the automatic reset when service is shut down the next time
         */
        internal fun ignoreAutoReset() {
            ignoreAutoReset = true
        }
    }
}
