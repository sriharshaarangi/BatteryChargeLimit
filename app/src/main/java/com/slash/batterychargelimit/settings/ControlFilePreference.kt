package com.slash.batterychargelimit.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.annotation.Keep
import androidx.preference.DialogPreference

import com.slash.batterychargelimit.ControlFile
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.Utils

@Keep
class ControlFilePreference(context: Context, attrs: AttributeSet?) : DialogPreference(context, attrs) {

    lateinit var controlFile:String

    init {
        positiveButtonText = null
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        controlFile = getPersistedString(null)
    }

    fun getCurrentControlFile() : String {
        return controlFile
    }
}