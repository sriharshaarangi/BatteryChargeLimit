package com.slash.batterychargelimit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.slash.batterychargelimit.Constants.INTENT_DISABLE_ACTION
import com.slash.batterychargelimit.Utils

/**
 * Created by xdnax on 11/14/2017
 */
class ServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == INTENT_DISABLE_ACTION) {
            Utils.stopService(context, false)
        }
    }
}
