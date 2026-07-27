package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.engine.Candidate

/**
 * Horizontal, scrollable row of word candidates shown above the keypad. Tapping a
 * chip selects that word. The first chip is the current best guess (also shown as
 * composing text in the field).
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
        setBackgroundColor(BAR_BG)
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
        val padH = dp(16)
        val padV = dp(8)
        return TextView(context).apply {
            text = candidate.word
            gravity = Gravity.CENTER
            setPadding(padH, padV, padH, padV)
            setTextColor(if (first) FIRST_COLOR else Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            isClickable = true
            setOnClickListener { onPick(candidate) }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val BAR_BG = 0xFF141414.toInt()
        private val FIRST_COLOR = 0xFF6EA8FE.toInt() // best guess highlighted
    }
}
