package com.slash.batterychargelimit.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.slash.batterychargelimit.SharedMethods

class DisableServiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SharedMethods.stopService(this)
        finish()
    }
}
