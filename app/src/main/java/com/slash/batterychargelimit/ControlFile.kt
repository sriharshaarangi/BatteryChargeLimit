package com.slash.batterychargelimit

import android.support.annotation.Keep

/**
 * Created by Michael on 31.03.2017.
 *
 * This is a pojo class representing known control files.
 * The private members are populated by the GSON library.
 * The "Keep" annotation makes ProGuard ignore those fields instead of optimizing them away.
 */
class ControlFile {
    @Keep
    var file: String? = null
    @Keep
    var label: String? = null
    @Keep
    var details: String? = null
    @Keep
    var chargeOn: String? = null
    @Keep
    var chargeOff: String? = null
    @Keep
    var isExperimental: Boolean = false
    @Transient private var checked = false
    @Transient private var valid = false

    val isValid: Boolean
        get() {
            if (!checked) {
                throw RuntimeException("Tried to check unvalidated ControlFile")
            }
            return valid
        }

    fun validate() {
        val suShell = SharedMethods.suShell
        if (!checked) {
            suShell.addCommand("test -e " + file!!, 0) { _, exitCode, _ ->
                valid = 0 == exitCode
                checked = true
            }
        }
    }

}
