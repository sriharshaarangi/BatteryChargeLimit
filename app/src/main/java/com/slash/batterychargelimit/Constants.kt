package com.slash.batterychargelimit

/**
 * Created by Michael on 26.03.2017.
 *
 * This class holds constants for internal use that are not shown to the user
 */

object Constants {
    val SETTINGS = "Settings"
    val SETTINGS_VERSION = "SettingsVersion"

    val FILE_KEY = "ctrl_file"
    val CHARGE_ON_KEY = "charge_on"
    val CHARGE_OFF_KEY = "charge_off"

    val LIMIT = "limit"
    val MIN = "min"
    val ENABLE = "enable"
    val NOTIFICATION_LIVE = "notificationLive"
    val AUTO_RESET_STATS = "auto_reset_stats"

    // ms after reaching limit, where the "unplug" event is recognized as power cut instead of action unplugging
    val POWER_CHANGE_TOLERANCE_MS: Long = 3000
    val CHARGING_CHANGE_TOLERANCE_MS: Long = 500
    val MAX_BACK_OFF_TIME: Long = 30000

    val INTENT_TOGGLE_ACTION = "com.slash.batterychargelimit.TOGGLE"

    val XDA_THREAD = "https://forum.xda-developers.com/android/apps-games/root-battery-charge-limit-t3557002/"
    val SOURCE_CODE = "https://github.com/sriharshaarangi/BatteryChargeLimit"
    val DEVELOPERS = arrayOf("Sri Harsha Arangi", "Michael Lux")
    val TRANSLATORS = arrayOf("Caio Roberto")
}
