package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The manual disambiguation column (plan §3): a vertical strip beside the keypad
 * showing the letters of the digit at the current position, styled to match the
 * TouchPal look (rounded cells, teal letters). Tapping a letter forces it into the
 * word. Always visible; empty when at rest (favourite symbols come in Phase 3).
 */
@SuppressLint("ViewConstructor")
class DisambiguationColumnView(
    context: Context,
    private val onPickLetter: (Char) -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(KeyboardTheme.COLUMN_BG)
    }

    /** Show one tappable cell per letter (lowercase chars from the keypad map). */
    fun setLetters(letters: List<Char>) {
        removeAllViews()
        for (letter in letters) addView(buildCell(letter))
    }

    private fun buildCell(letter: Char): View {
        val gap = dp(3)
        return TextView(context).apply {
            text = letter.uppercaseChar().toString()
            gravity = Gravity.CENTER
            setTextColor(KeyboardTheme.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            background = KeyboardTheme.keyBackground(
                context,
                normal = KeyboardTheme.COLUMN_CELL,
                pressed = KeyboardTheme.COLUMN_CELL_PRESSED
            )
            isClickable = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(gap, gap, gap, gap)
            }
            setOnClickListener { onPickLetter(letter) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()
}
