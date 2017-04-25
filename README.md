# BatteryChargeLimit
Source code of the android app that stops charging at a desired level.

For more info: https://forum.xda-developers.com/android/apps-games/root-battery-charge-limit-t3557002

Since version 0.7, the charging limit can be set using Intents. There are two ways for doing so:
- Using an Intent to Broadcast a message of type *"com.slash.batterychargelimit.CHANGE_LIMIT"*, supplying the percentage by the *Intent extra "android.intent.extra.TEXT"* (Intent.EXTRA_TEXT). __This is technically more clean and therefore recommended!__
- Starting Activity *com.slash.batterychargelimit.LimitChangeActivity* with a *ACTION_SEND* intent, using *MIME type "text/x-battery-limit"* and supplying the percentage as String with *Intent extra "android.intent.extra.TEXT"* (Intent.EXTRA_TEXT). __This will cause the receiving Activity to pop up for a short moment. The Intent should be provided with FLAG_ACTIVITY_NO_HISTORY, otherwise the main activity of this app will become the foreground Activity after sending the Intent.__
