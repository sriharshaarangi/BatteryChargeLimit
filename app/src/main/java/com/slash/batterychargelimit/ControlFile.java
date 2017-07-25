package com.slash.batterychargelimit;

import android.support.annotation.Keep;
import eu.chainfire.libsuperuser.Shell;

import java.util.List;

/**
 * Created by Michael on 31.03.2017.
 *
 * This is a pojo class representing known control files.
 * The private members are populated by the GSON library.
 * The "Keep" annotation makes ProGuard ignore those fields instead of optimizing them away.
 */
public class ControlFile {
    @Keep
    private String file;
    @Keep
    private String label;
    @Keep
    private String details;
    @Keep
    private String chargeOn;
    @Keep
    private String chargeOff;
    @Keep
    private boolean experimental;
    private transient boolean checked = false, valid = false;

    public String getFile() {
        return file;
    }

    public String getLabel() {
        return label;
    }

    public String getDetails() {
        return details;
    }

    public String getChargeOn() {
        return chargeOn;
    }

    public String getChargeOff() {
        return chargeOff;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public boolean isValid() {
        if (!checked) {
            throw new RuntimeException("Tried to check unvalidated ControlFile");
        }
        return valid;
    }

    public void validate() {
        Shell.Interactive suShell = SharedMethods.getSuShell();
        if (!checked) {
            suShell.addCommand("test -e " + getFile(), 0, new Shell.OnCommandResultListener() {
                @Override
                public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                    valid = 0 == exitCode;
                    checked = true;
                }
            });
        }
    }

}
