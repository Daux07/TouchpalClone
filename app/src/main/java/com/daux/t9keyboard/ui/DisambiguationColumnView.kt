package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The manual disambiguation column (plan §3): a narrow vertical strip beside the
 * keypad showing the letters of the digit at the current position. Cells have a
 * small fixed height and the whole strip **scrolls** when there are more items
 * than fit — useful for longer candidate lists and for the favourite-symbols rest
 * state (Phase 3). Tapping a cell forces that letter into the word.
 *
 * Styled lighter than the keys so it stands out (plan §3.2/§3.10).
 */
@SuppressLint("ViewConstructor")
class DisambiguationColumnView(
    context: Context,
    private val onPickLetter: (Char) -> Unit
) : ScrollView(context) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    init {
        isVerticalScrollBarEnabled = false
        setBackgroundColor(KeyboardTheme.COLUMN_BG)
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    /** Show one small tappable cell per letter (lowercase chars from the keypad). */
    fun setLetters(letters: List<Char>) {
        container.removeAllViews()
        for (letter in letters) container.addView(buildCell(letter))
        scrollY = 0
    }

    private fun buildCell(letter: Char): View {
        val gap = dp(2)
        return TextView(context).apply {
            text = letter.uppercaseChar().toString()
            gravity = Gravity.CENTER
            setTextColor(KeyboardTheme.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            background = KeyboardTheme.keyBackground(
                context,
                normal = KeyboardTheme.COLUMN_CELL,
                pressed = KeyboardTheme.COLUMN_CELL_PRESSED
            )
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(CELL_HEIGHT_DP)
            ).apply { setMargins(gap, gap, gap, gap) }
            setOnClickListener { onPickLetter(letter) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val CELL_HEIGHT_DP = 40
    }
}
