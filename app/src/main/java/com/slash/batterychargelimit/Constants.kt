package com.slash.batterychargelimit

/**
 * Created by Michael on 26.03.2017.
 *
 * This class holds constants for internal use that are not shown to the user
 */

object Constants {
    const val SETTINGS = "Settings"
    const val SETTINGS_VERSION = "SettingsVersion"

    const val FILE_KEY = "ctrl_file"
    const val CHARGE_ON_KEY = "charge_on"
    const val CHARGE_OFF_KEY = "charge_off"

    const val DEFAULT_FILE = "/sys/class/power_supply/battery/charging_enabled"
    const val DEFAULT_ENABLED = "1"
    const val DEFAULT_DISABLED = "0"

    const val LIMIT = "limit"
    const val MIN = "min"
    const val ENABLE = "enable"
    const val NOTIFICATION_LIVE = "notificationLive"
    const val AUTO_RESET_STATS = "auto_reset_stats"
    const val NOTIFICATION_SOUND = "notificationSound"

    // ms after reaching limit, where the "unplug" event is recognized as power cut instead of action unplugging
    const val POWER_CHANGE_TOLERANCE_MS: Long = 3000
    const val CHARGING_CHANGE_TOLERANCE_MS: Long = 500
    const val MAX_BACK_OFF_TIME: Long = 30000

    const val NOTIF_MAINTAIN = "ic_maintain"
    const val NOTIF_CHARGE = "ic_charge"

    const val INTENT_TOGGLE_ACTION = "com.slash.batterychargelimit.TOGGLE"
    const val INTENT_DISABLE_ACTION = "com.slash.batterychargelimit.DISABLE"

    const val XDA_THREAD = "https://forum.xda-developers.com/android/apps-games/root-battery-charge-limit-t3557002/"
    const val SOURCE_CODE = "https://github.com/sriharshaarangi/BatteryChargeLimit"
    val DEVELOPERS = arrayOf("Sri Harsha Arangi", "Michael Lux", "Mike xdnax")
    val TRANSLATORS = arrayOf("Brazilian Portuguese: Caio Roberto",
            "Bengali: AdroitAdorKhan",
            "Spanish: Jose",
            "Russian: Ричард Иванов",
            "Italian: BombeerHC",
            "Romanian: Y0lin",
            "Dutch: hypothermic"
    )

    const val SAVED_PATH_DATA = "saved_ctrl_path_data"
    const val SAVED_ENABLED_DATA = "saved_ctrl_enabled_data"
    const val SAVED_DISABLED_DATA = "saved_ctrl_disabled_data"
}
