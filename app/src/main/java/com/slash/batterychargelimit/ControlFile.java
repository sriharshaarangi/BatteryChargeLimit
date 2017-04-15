package com.slash.batterychargelimit;

import static com.slash.batterychargelimit.Constants.*;

/**
 * Created by Michael on 31.03.2017.
 *
 * This is a pojo class representing known control files
 */
public class ControlFile {

    public final String file;
    public final String label;
    public final String chargeOn;
    public final String chargeOff;
    public final boolean valid;

    public ControlFile(String[] modeArray) {
        file = modeArray[FILE_INDEX];
        label = modeArray[LABEL_INDEX];
        chargeOn = modeArray[CHARGE_ON_INDEX];
        chargeOff = modeArray[CHARGE_OFF_INDEX];
        valid = SharedMethods.checkFile(file);
    }

}
