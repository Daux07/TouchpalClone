package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The manual disambiguation column (plan §3): a vertical strip beside the keypad
 * showing the letters of the digit at the current position. Tapping a letter
 * forces it into the word. Always visible; empty when at rest (favourite symbols
 * for the rest state come in Phase 3).
 */
@SuppressLint("ViewConstructor")
class DisambiguationColumnView(
    context: Context,
    private val onPickLetter: (Char) -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(COLUMN_BG)
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
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setBackgroundColor(CELL_BG)
            isClickable = true
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                setMargins(gap, gap, gap, gap)
            }
            setOnClickListener { onPickLetter(letter) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val COLUMN_BG = 0xFF181818.toInt()
        private const val CELL_BG = 0xFF3A3A3A.toInt()
    }
}
