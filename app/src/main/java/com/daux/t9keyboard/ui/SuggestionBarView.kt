package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.engine.Candidate

/**
 * Horizontal, scrollable row of word candidates above the keypad (TouchPal-style:
 * the first/best candidate is tinted teal, the rest are light). Tapping a chip
 * selects that word.
 */
@SuppressLint("ViewConstructor")
class SuggestionBarView(
    context: Context,
    private val onPick: (Candidate) -> Unit
) : HorizontalScrollView(context) {

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    init {
        isFillViewport = true
        isHorizontalScrollBarEnabled = false
        setBackgroundColor(KeyboardTheme.BAR_BG)
        addView(
            container,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun setCandidates(candidates: List<Candidate>) {
        container.removeAllViews()
        scrollX = 0
        candidates.forEachIndexed { i, c -> container.addView(makeChip(c, first = i == 0)) }
    }

    private fun makeChip(candidate: Candidate, first: Boolean): View {
        val padH = dp(18)
        return TextView(context).apply {
            text = candidate.word
            gravity = Gravity.CENTER
            setPadding(padH, 0, padH, 0)
            setTextColor(if (first) KeyboardTheme.ACCENT else KeyboardTheme.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            background = KeyboardTheme.ghostBackground(context)
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setOnClickListener { onPick(candidate) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()
}
