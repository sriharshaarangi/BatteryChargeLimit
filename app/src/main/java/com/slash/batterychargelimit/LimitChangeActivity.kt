package com.slash.batterychargelimit

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

/**
 * Created by Michael on 20.04.2017.
 *
 * Handles new battery limits sent via Intents.
 */
class LimitChangeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // handle data sent by intent
        val batteryLimitMime = this.getString(R.string.mime_battery_limit)
        val intent = intent
        if (Intent.ACTION_SEND == intent.action && batteryLimitMime == intent.type) {
            Utils.handleLimitChange(this, intent.extras.get(Intent.EXTRA_TEXT))
        } else {
            Toast.makeText(this, R.string.intent_invalid, Toast.LENGTH_LONG).show()
        }

        finish()
    }

}
