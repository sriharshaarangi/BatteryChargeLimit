package com.slash.batterychargelimit

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by Michael on 20.04.2017.
 *
 * Handles new battery limits sent via Broadcasts.
 */
class LimitChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Utils.handleLimitChange(context, intent.extras.get(Intent.EXTRA_TEXT))
    }

}
