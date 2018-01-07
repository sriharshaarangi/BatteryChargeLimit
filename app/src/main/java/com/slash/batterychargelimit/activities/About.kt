package com.slash.batterychargelimit.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.widget.TextView
import com.slash.batterychargelimit.Constants.DEVELOPERS
import com.slash.batterychargelimit.Constants.TRANSLATORS
import com.slash.batterychargelimit.Constants.SOURCE_CODE
import com.slash.batterychargelimit.Constants.XDA_THREAD
import com.slash.batterychargelimit.R

class About : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        displayVersion()
        displayDevelopers()
        displayTranslators()
        displaySourceLink()
        displayXdaLink()
    }

    private fun displayVersion() {
        val versionTV = findViewById(R.id.app_version) as TextView
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val version = packageInfo.versionName
            val versionCode = packageInfo.versionCode
            versionTV.text = "$version ($versionCode)"
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun displayDevelopers() {
        val developersTV = findViewById(R.id.developers) as TextView
        val builder = StringBuilder()
        for (s in DEVELOPERS) {
            builder.append(s).append("\n")
        }
        developersTV.text = builder.toString()
    }

    private fun displayTranslators() {
        val translatorsTV = findViewById(R.id.translators) as TextView
        val builder = StringBuilder()
        for (s in TRANSLATORS) {
            builder.append(s).append("\n")
        }
        translatorsTV.text = builder.toString()
    }

    private fun displaySourceLink() {
        val sourceTV = findViewById(R.id.source_code) as TextView
        sourceTV.text = SOURCE_CODE
        sourceTV.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun displayXdaLink() {
        val xdaTV = findViewById(R.id.xda_thread) as TextView
        xdaTV.text = XDA_THREAD
        xdaTV.movementMethod = LinkMovementMethod.getInstance()
    }
}
