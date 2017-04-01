package com.slash.batterychargelimit;

/**
 * Created by Michael on 26.03.2017.
 *
 * This class holds constants for internal use that are not shown to the user
 */

public abstract class Constants {
    public static String SETTINGS = "Settings";
    public static String SETTINGS_VERSION = "SettingsVersion";

    public static int FILE_INDEX = 0;
    public static int LABEL_INDEX = 1;
    public static int CHARGE_ON_INDEX = 2;
    public static int CHARGE_OFF_INDEX = 3;
    public static String FILE_KEY = "ctrl_file";
    public static String CHARGE_ON_KEY = "charge_on";
    public static String CHARGE_OFF_KEY = "charge_off";

    public static String LIMIT = "limit";
    public static String RECHARGE_DIFF = "recharge_threshold";
    public static String LIMIT_REACHED = "limit_reached";
    public static String ENABLE = "enable";
    public static String NOTIFICATION_LIVE = "notificationLive";
    public static String AUTO_RESET_STATS = "auto_reset_stats";

    // ms after reaching limit, where the "unplug" event is recognized as power cut instead of action unplugging
    public static long UNPLUG_TOLERANCE = 1000;
}
