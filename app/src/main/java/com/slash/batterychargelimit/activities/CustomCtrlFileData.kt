package com.slash.batterychargelimit.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.SharedMethods

class CustomCtrlFileData : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ctrl_file_data)

        var customPathData: String? = null
        var customEnabledData: String? = null
        var customDisabledData: String? = null
        val editPathData = findViewById(R.id.edit_path_file) as EditText
        val editEnabledData = findViewById(R.id.edit_path_enabled) as EditText
        val editDisabledData = findViewById(R.id.edit_path_disabled) as EditText
        val btnUpdateData = findViewById(R.id.btn_update_custom) as Button
        val settings = this.getSharedPreferences(Constants.SETTINGS, 0)
        val savedPathData = settings.getString(Constants.SAVED_PATH_DATA, "/sys/class/power_supply/battery/charge_disable")
        val savedEnabledData = settings.getString(Constants.SAVED_ENABLED_DATA, "1")
        val savedDisabledData = settings.getString(Constants.SAVED_DISABLED_DATA, "0")
        val updatedDataText = findViewById(R.id.custom_data_updated) as TextView

        updatedDataText.hint = "Path Data: " + savedPathData + "\nEnable Value: " + savedEnabledData + "\nDisabled Value: " + savedDisabledData


        editPathData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                customPathData = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        editEnabledData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                customEnabledData = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        editDisabledData.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                customDisabledData = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        btnUpdateData.setOnClickListener({
            SharedMethods.stopService(this, true)
            settings.edit().putString(Constants.SAVED_PATH_DATA, customPathData).apply()
            settings.edit().putString(Constants.SAVED_ENABLED_DATA, customEnabledData).apply()
            settings.edit().putString(Constants.SAVED_DISABLED_DATA, customDisabledData).apply()
            updatedDataText.hint = "Path Data: $customPathData\nEnable Value: $customEnabledData\nDisabled Value: $customDisabledData"
            SharedMethods.startService(this)
        })
    }
}
