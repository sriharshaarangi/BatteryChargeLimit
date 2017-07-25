package com.slash.batterychargelimit.settings;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import com.slash.batterychargelimit.SharedMethods;

public final class CtrlFileHelper {

    public static void validateFiles(@NonNull final Context context, final Runnable callback) {
        final Handler handler = new Handler();
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Checking control files, please wait...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        SharedMethods.getExecutor().submit(new Runnable() {
            @Override
            public void run() {
                SharedMethods.validateCtrlFiles(context);
                SharedMethods.getSuShell().waitForIdle();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        if (callback != null) {
                            callback.run();
                        }
                    }
                });
            }
        });
    }

}
