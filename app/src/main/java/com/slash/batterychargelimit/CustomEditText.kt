package com.slash.batterychargelimit

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.google.android.material.textfield.TextInputEditText

class CustomEditText : TextInputEditText {
    constructor(context: Context) : super(context) {}

    constructor(context: Context, attribute_set: AttributeSet) : super(context, attribute_set) {}

    constructor(context: Context, attribute_set: AttributeSet, def_style_attribute: Int) : super(context, attribute_set, def_style_attribute) {}

    override fun onKeyPreIme(key_code: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP)
            this.clearFocus()
        return super.onKeyPreIme(key_code, event)
    }
}
