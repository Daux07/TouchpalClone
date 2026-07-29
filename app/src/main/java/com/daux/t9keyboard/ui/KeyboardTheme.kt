package com.daux.t9keyboard.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable

/**
 * Shared dark palette and key backgrounds, tuned to resemble the classic TouchPal
 * T9 look: charcoal-blue background, slightly lighter rounded keys, teal accents.
 * Centralised here so all views stay consistent (theming/config comes in Phase 3).
 */
object KeyboardTheme {

    const val BG = 0xFF262A33.toInt()          // keyboard background
    const val BAR_BG = 0xFF20242C.toInt()      // suggestion bar background
    const val COLUMN_BG = 0xFF20242C.toInt()   // disambiguation column background

    const val KEY = 0xFF363B45.toInt()         // key face
    const val KEY_PRESSED = 0xFF4A515C.toInt() // key pressed
    const val FUNC_KEY = 0xFF2E333C.toInt()    // function key face (slightly darker)

    const val COLUMN_CELL = 0xFF66707D.toInt()         // disambiguation cell (lighter, to stand out)
    const val COLUMN_CELL_PRESSED = 0xFF7B8593.toInt()

    const val POPUP_BG = 0xFF454B57.toInt()    // long-press panel: lighter, floats over the keys

    const val TEXT = 0xFFF2F4F7.toInt()        // primary text (letters)
    const val TEXT_DIM = 0xFFAEB4BF.toInt()    // secondary text
    const val ACCENT = 0xFF35C5D0.toInt()      // teal accent (numbers, icons, 1st suggestion)

    /** Rounded key background with a pressed state, for tactile feedback. */
    fun keyBackground(
        context: Context,
        normal: Int = KEY,
        pressed: Int = KEY_PRESSED,
        radiusDp: Float = 9f
    ): StateListDrawable {
        val radius = radiusDp * context.resources.displayMetrics.density
        fun face(color: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), face(pressed))
            addState(intArrayOf(), face(normal))
        }
    }

    /** Transparent background that only highlights while pressed (for chips). */
    fun ghostBackground(context: Context, radiusDp: Float = 8f): StateListDrawable =
        keyBackground(context, normal = 0x00000000, pressed = KEY, radiusDp = radiusDp)
}
