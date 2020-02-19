package com.slash.batterychargelimit.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.Utils

/**
 * Created by Michael on 20.04.2017.
 *
 * Triggered when the phone finished booting.
 * Checks whether power supply is attached and starts the foreground service if necessary.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Utils.setVoltageThreshold(null, true, context, null)
            Utils.startServiceIfLimitEnabled(context)
            Utils.suShell.addCommand("cat ${Utils.getVoltageFile()}", 0) { _, _, output ->
                if (output.size != 0 ) {
                    Utils.getSettings(context).edit().putString(Constants.DEFAULT_VOLTAGE_LIMIT, output[0]).apply()
                }
            }
        }
    }
}
