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
 * keypad showing the letters of the digit at the current position.
 *
 * Cells are sized to **fill the column with 3–4 items** (the usual letter count):
 * with up to 4 items they divide the height evenly; with more the cells keep the
 * 4-item size and the strip **scrolls** (useful for the favourite-symbols rest
 * state in Phase 3). Styled lighter than the keys so it stands out.
 */
@SuppressLint("ViewConstructor")
class DisambiguationColumnView(
    context: Context,
    private val onPickLetter: (Char) -> Unit
) : ScrollView(context) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private var letters: List<Char> = emptyList()
    private var viewportHeight = 0

    init {
        isVerticalScrollBarEnabled = false
        setBackgroundColor(KeyboardTheme.COLUMN_BG)
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    /** Show one tappable cell per letter (lowercase chars from the keypad). */
    fun setLetters(letters: List<Char>) {
        this.letters = letters
        rebuild()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewportHeight = h
        rebuild()
    }

    private fun rebuild() {
        container.removeAllViews()
        scrollY = 0
        if (letters.isEmpty() || viewportHeight == 0) return
        // Divide by the item count (so 3–4 items fill), capped at 4 (more scroll).
        val divisor = letters.size.coerceIn(3, 4)
        val cellHeight = (viewportHeight / divisor - dp(4)).coerceAtLeast(dp(30))
        for (letter in letters) container.addView(buildCell(letter, cellHeight))
    }

    private fun buildCell(letter: Char, heightPx: Int): View {
        val gap = dp(2)
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
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            ).apply { setMargins(gap, gap, gap, gap) }
            setOnClickListener { onPickLetter(letter) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()
}
