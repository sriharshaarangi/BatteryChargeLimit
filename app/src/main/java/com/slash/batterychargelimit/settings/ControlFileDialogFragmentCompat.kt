package com.slash.batterychargelimit.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.preference.PreferenceDialogFragmentCompat
import com.slash.batterychargelimit.ControlFile
import com.slash.batterychargelimit.R
import com.slash.batterychargelimit.Utils

class ControlFileDialogFragmentCompat : PreferenceDialogFragmentCompat() {
    private var ctrlFiles = emptyList<ControlFile>()
    private var default = ""

    override fun onCreateDialogView(context: Context?): View {
        super.onCreateDialogView(context)
        ctrlFiles = Utils.getCtrlFiles(context!!)
        return ListView(context)
    }

    override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        val v = view as ListView
        default = (preference as ControlFilePreference).getCurrentControlFile()
        v.adapter = ControlFileAdapter(ctrlFiles.filter { it.isValid }, context!!)
    }

    override fun onDialogClosed(positiveResult: Boolean) {}


    inner class ControlFileAdapter internal constructor(private val data: List<ControlFile>, pContext: Context)
        : ArrayAdapter<ControlFile>(pContext, R.layout.cf_row, data) {

        private inner class ViewHolder {
            internal var label: RadioButton? = null
            internal var details: TextView? = null
            internal var experimental: TextView? = null
            internal var issues: TextView? = null
        }

        override fun getView(position: Int, pConvertView: View?, parent: ViewGroup): View {

            var convertView: View? = pConvertView
            val h: ViewHolder
            val cf = data[position]

            if (convertView == null) {
                h = ViewHolder()
                val inflater = LayoutInflater.from(context)
                convertView = inflater.inflate(R.layout.cf_row, parent, false)
                convertView!!.setOnClickListener { h.label!!.performClick() }
                h.label = convertView.findViewById(R.id.cf_label) as RadioButton
                h.label!!.setOnClickListener { v ->
                    if (v.isEnabled) {
                        Utils.setCtrlFile(context, v.tag as ControlFile)
                        this@ControlFileDialogFragmentCompat.dialog!!.dismiss()
                    }
                }
                h.details = convertView.findViewById(R.id.cf_details) as TextView
                h.experimental = convertView.findViewById(R.id.cf_experimental) as TextView
                h.issues = convertView.findViewById(R.id.cf_issues) as TextView
                convertView.tag = h
            } else {
                h = convertView.tag as ViewHolder
            }
            h.label!!.isEnabled = cf.isValid
            h.label!!.text = cf.label
            h.label!!.tag = cf
            h.label!!.isChecked = cf.file == default
            h.details!!.text = cf.details
            h.experimental!!.visibility = if (cf.experimental!!) View.VISIBLE else View.GONE
            h.issues!!.visibility = if (cf.issues!!) View.VISIBLE else View.GONE

            return convertView
        }
    }


    companion object {
        fun newInstance(key: String) : ControlFileDialogFragmentCompat{
            val instance = ControlFileDialogFragmentCompat()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            instance.arguments = b
            return instance
        }
    }
}