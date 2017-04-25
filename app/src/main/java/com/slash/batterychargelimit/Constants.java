package com.slash.batterychargelimit;

/**
 * Created by Michael on 26.03.2017.
 *
 * This class holds constants for internal use that are not shown to the user
 */

public abstract class Constants {
    public static final String SETTINGS = "Settings";
    public static final String SETTINGS_VERSION = "SettingsVersion";

    public static final String FILE_KEY = "ctrl_file";
    public static final String CHARGE_ON_KEY = "charge_on";
    public static final String CHARGE_OFF_KEY = "charge_off";

    public static final String LIMIT = "limit";
    public static final String RECHARGE_DIFF = "recharge_threshold";
    public static final String LIMIT_REACHED = "limit_reached";
    public static final String REFRESH_STARTED = "refresh_started";
    public static final String ENABLE = "enable";
    public static final String NOTIFICATION_LIVE = "notificationLive";
    public static final String AUTO_RESET_STATS = "auto_reset_stats";

    // ms after reaching limit, where the "unplug" event is recognized as power cut instead of action unplugging
    public static final long POWER_CHANGE_TOLERANCE_MS = 3000;
    public static final long CHARGING_CHANGE_TOLERANCE_MS = 500;
}
