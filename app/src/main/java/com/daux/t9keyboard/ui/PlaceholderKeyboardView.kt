package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView

/**
 * Temporary Phase 0 keyboard surface: a single labelled panel sized like a real
 * keyboard, so we can confirm the IME renders. Replaced in Phase 1 by the custom
 * 12-key grid + disambiguation column.
 */
@SuppressLint("ViewConstructor")
class PlaceholderKeyboardView(context: Context) : TextView(context) {

    init {
        text = "T9 Keyboard — placeholder (Fase 0)"
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        // Approximate a keyboard-height panel; real responsive sizing comes later.
        minHeight = (resources.displayMetrics.density * 220).toInt()
    }
}
