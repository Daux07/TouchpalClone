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

    /**
     * Candidate text size in sp. Phase 3 will drive this from the keyboard settings
     * (together with the keyboard height); until then it stays at [DEFAULT_TEXT_SP].
     */
    var textSizeSp: Float = DEFAULT_TEXT_SP
        set(value) {
            if (field == value) return
            field = value
            for (i in 0 until container.childCount) {
                (container.getChildAt(i) as TextView)
                    .setTextSize(TypedValue.COMPLEX_UNIT_SP, value)
            }
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
            // Typo-tolerant candidates read as dimmer offers, exact ones as answers.
            setTextColor(
                when {
                    candidate.fuzzy -> KeyboardTheme.TEXT_DIM
                    first -> KeyboardTheme.ACCENT
                    else -> KeyboardTheme.TEXT
                }
            )
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
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

    companion object {
        /** Comfortably readable at arm's length; adjustable from settings in Phase 3. */
        const val DEFAULT_TEXT_SP = 22f
    }
}
