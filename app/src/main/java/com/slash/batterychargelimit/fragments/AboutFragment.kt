package com.slash.batterychargelimit.fragments

import android.app.Fragment
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.slash.batterychargelimit.Constants
import com.slash.batterychargelimit.R

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_about, container, false)

        setHasOptionsMenu(true)

        displayVersion(view)
        displayDevelopers(view)
        displayTranslators(view)
        displaySourceLink(view)
        displayXdaLink(view)

        return view
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val item = menu.findItem(R.id.about)
        item.isVisible = false
    }

    override fun onStart() {
        visible = true
        super.onStart()
    }

    override fun onStop() {
        visible = false
        super.onStop()
    }

    companion object {
        private var visible = false

        fun aboutVisible(): Boolean {
            return visible
        }
    }

    private fun displayVersion(view: View) {
        val versionTV = view.findViewById(R.id.app_version) as TextView
        try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            val version = packageInfo.versionName
            val versionCode = packageInfo.versionCode
            versionTV.text = "$version ($versionCode)"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun displayDevelopers(view: View) {
        val developersTV = view.findViewById(R.id.developers) as TextView
        val builder = StringBuilder()
        for (s in Constants.DEVELOPERS) {
            builder.append(s).append("\n")
        }
        developersTV.text = builder.toString()
    }

    private fun displayTranslators(view: View) {
        val translatorsTV = view.findViewById(R.id.translators) as TextView
        val builder = StringBuilder()
        for (s in Constants.TRANSLATORS) {
            builder.append(s).append("\n")
        }
        translatorsTV.text = builder.toString()
    }

    private fun displaySourceLink(view: View) {
        val sourceTV = view.findViewById(R.id.source_code) as TextView
        sourceTV.text = Constants.SOURCE_CODE
        sourceTV.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun displayXdaLink(view: View) {
        val xdaTV = view.findViewById(R.id.xda_thread) as TextView
        xdaTV.text = Constants.XDA_THREAD
        xdaTV.movementMethod = LinkMovementMethod.getInstance()
    }
}