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
    const val CHARGE_LIMIT_ENABLED = "enable"
    const val DISABLE_CHARGE_NOW = "disable_charge_now"
    const val NOTIFICATION_LIVE = "notificationLive"
    const val AUTO_RESET_STATS = "auto_reset_stats"
    const val NOTIFICATION_SOUND = "notificationSound"

    const val LIMIT_BY_VOLTAGE = "limit_by_voltage"
    const val DEFAULT_VOLTAGE_LIMIT = "default_voltage_limit"
    const val CUSTOM_VOLTAGE_LIMIT = "custom_voltage_limit"
//    const val CURRENT_VOLTAGE_LIMIT = "current_voltage_limit"

    // ms after reaching limit, where the "unplug" event is recognized as power cut instead of action unplugging
    const val POWER_CHANGE_TOLERANCE_MS: Long = 3000
    const val CHARGING_CHANGE_TOLERANCE_MS: Long = 500
    const val MAX_BACK_OFF_TIME: Long = 30000

    const val MAX_ALLOWED_LIMIT_PC: Int = 100
    const val DEFAULT_LIMIT_PC: Int = 80
    const val MIN_ALLOWED_LIMIT_PC: Int = 40

    //voltage thresholds in mV, inclusive
    const val DEFAULT_VOLTAGE_FILE = "/sys/class/power_supply/battery/voltage_max"
    const val MIN_VOLTAGE_THRESHOLD_MV = "3700"
    const val DEFAULT_VOLTAGE_THRESHOLD_MV = "4100"
    const val MAX_VOLTAGE_THRESHOLD_MV = "4400"

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
            "Dutch: hypothermic",
            "Turkish: FatihFIRINCI"
    )

    const val SAVED_PATH_DATA = "saved_ctrl_path_data"
    const val SAVED_ENABLED_DATA = "saved_ctrl_enabled_data"
    const val SAVED_DISABLED_DATA = "saved_ctrl_disabled_data"

    const val LIGHT = "light"
    const val DARK = "dark"
    const val BLACK = "black"
}
