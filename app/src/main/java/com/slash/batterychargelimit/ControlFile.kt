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

    @Keep val file: String? = null
    @Keep val label: String? = null
    @Keep val details: String? = null
    @Keep val chargeOn: String? = null
    @Keep val chargeOff: String? = null
    @Keep val experimental: Boolean? = false
    @Keep val order: Int? = 0
    @Keep val issues: Boolean? = false
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
