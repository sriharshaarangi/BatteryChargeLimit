package com.slash.batterychargelimit.settings

import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import com.slash.batterychargelimit.SharedMethods

object CtrlFileHelper {

    fun validateFiles(context: Context, callback: Runnable?) {
        val handler = Handler()
        val dialog = ProgressDialog(context)
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dialog.setMessage("Checking control file data, please wait...")
        dialog.isIndeterminate = true
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        SharedMethods.executor.submit {
            SharedMethods.validateCtrlFiles(context)
            SharedMethods.suShell.waitForIdle()
            handler.post {
                dialog.dismiss()
                callback?.run()
            }
        }
    }

}
