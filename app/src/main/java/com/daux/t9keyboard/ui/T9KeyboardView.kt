package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.T9Layout

/**
 * The whole input surface: a suggestion bar on top of a responsive key grid.
 *
 * Sizing is proportional, not fixed: the grid rows share their height via layout
 * weights and keys share each row's width via weights, so the same layout scales
 * cleanly between the Galaxy S25 and S25 Ultra (plan §6). The suggestion bar has a
 * fixed height; the grid takes a fraction of the screen height below it.
 *
 * Display-only: taps are reported through [onKey]; candidate picks through
 * [onPickCandidate]. All input logic lives in the service.
 */
@SuppressLint("ViewConstructor")
class T9KeyboardView(
    context: Context,
    private val onKey: (KeyAction) -> Unit,
    onPickCandidate: (Candidate) -> Unit
) : LinearLayout(context) {

    private val suggestionBar = SuggestionBarView(context, onPickCandidate)

    init {
        orientation = VERTICAL
        setBackgroundColor(BG)

        addView(suggestionBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_DP)))

        val gap = dp(2)
        val gridWrap = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(gap, gap, gap, gap)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        for (row in T9Layout.rows) gridWrap.addView(buildRow(row))
        addView(gridWrap)
    }

    /** Replace the candidates shown in the suggestion bar. */
    fun setSuggestions(candidates: List<Candidate>) {
        suggestionBar.setCandidates(candidates)
    }

    private fun buildRow(keys: List<KeySpec>): View {
        val rowView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        for (key in keys) rowView.addView(buildKey(key))
        return rowView
    }

    private fun buildKey(key: KeySpec): View {
        val gap = dp(3)
        return TextView(context).apply {
            text = keyLabel(key)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setBackgroundColor(KEY_BG)
            isClickable = true
            isFocusable = true
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(gap, gap, gap, gap)
            }
            setOnClickListener { onKey(key.action) }
        }
    }

    /** Big label with a smaller, dimmer subtitle underneath (e.g. "2" / "ABC"). */
    private fun keyLabel(key: KeySpec): CharSequence {
        val sub = key.subtitle ?: return key.label
        val full = "${key.label}\n$sub"
        return SpannableString(full).apply {
            val subStart = key.label.length + 1
            setSpan(RelativeSizeSpan(0.55f), subStart, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(SUB_COLOR), subStart, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** Force the whole view to occupy the bar height plus a fraction of the screen. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        val desired = dp(BAR_DP) + (screenH * GRID_HEIGHT_FRACTION).toInt()
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val BAR_DP = 46
        private const val GRID_HEIGHT_FRACTION = 0.40f
        private const val BG = 0xFF1E1E1E.toInt()
        private const val KEY_BG = 0xFF2C2C2C.toInt()
        private val SUB_COLOR = 0xFF9E9E9E.toInt()
    }
}
