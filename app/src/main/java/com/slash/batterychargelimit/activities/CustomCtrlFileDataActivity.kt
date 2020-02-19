package com.slash.batterychargelimit.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.Constants.DEFAULT_DISABLED
import com.slash.batterychargelimit.Constants.DEFAULT_ENABLED
import com.slash.batterychargelimit.Constants.DEFAULT_FILE
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.Utils

class CustomCtrlFileDataActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Utils.setTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ctrl_file_data)

        var customPathData: String? = null
        var customEnabledData: String? = null
        var customDisabledData: String? = null
        val editPathData = findViewById<EditText>(R.id.edit_path_file)
        val editEnabledData = findViewById<EditText>(R.id.edit_path_enabled)
        val editDisabledData = findViewById<EditText>(R.id.edit_path_disabled)
        val btnUpdateData = findViewById<Button>(R.id.btn_update_custom)
        val settings = this.getSharedPreferences(Constants.SETTINGS, 0)
        val savedPathData = settings.getString(Constants.SAVED_PATH_DATA, DEFAULT_FILE)
        val savedEnabledData = settings.getString(Constants.SAVED_ENABLED_DATA, DEFAULT_ENABLED)
        val savedDisabledData = settings.getString(Constants.SAVED_DISABLED_DATA, DEFAULT_DISABLED)
        val updatedDataText = findViewById<TextView>(R.id.custom_data_updated)

        updatedDataText.hint = "Path Data: $savedPathData\nEnable Value: $savedEnabledData\nDisabled Value: $savedDisabledData"


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

        btnUpdateData.setOnClickListener {
            Utils.stopService(this)
            settings.edit().putString(Constants.SAVED_PATH_DATA, customPathData).apply()
            settings.edit().putString(Constants.SAVED_ENABLED_DATA, customEnabledData).apply()
            settings.edit().putString(Constants.SAVED_DISABLED_DATA, customDisabledData).apply()
            updatedDataText.hint = "Path Data: $customPathData\nEnable Value: $customEnabledData\nDisabled Value: $customDisabledData"
            Utils.startServiceIfLimitEnabled(this)
        }
    }
}
