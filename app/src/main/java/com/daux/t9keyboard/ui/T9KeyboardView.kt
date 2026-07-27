package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.T9Layout

/**
 * The whole input surface: suggestion bar on top, then the disambiguation column
 * beside the responsive key grid, styled to resemble the classic TouchPal T9
 * (dark charcoal, rounded keys, teal accents; big lowercase letters with a small
 * corner number).
 *
 * Sizing is proportional, not fixed: rows share height via weights, keys share
 * each row's width via weights, and the column takes a small weighted slice — so
 * the layout scales cleanly between the Galaxy S25 and S25 Ultra (plan §6).
 *
 * Display-only: taps are reported through [onKey], candidate picks through
 * [onPickCandidate], column letter picks through [onPickLetter].
 */
@SuppressLint("ViewConstructor")
class T9KeyboardView(
    context: Context,
    private val onKey: (KeyAction) -> Unit,
    onPickCandidate: (Candidate) -> Unit,
    onPickLetter: (Char) -> Unit
) : LinearLayout(context) {

    private val suggestionBar = SuggestionBarView(context, onPickCandidate)
    private val column = DisambiguationColumnView(context, onPickLetter)

    /** Bottom system-bar inset, so keys clear the navigation bar (edge-to-edge). */
    private var navBottomPx = 0

    init {
        orientation = VERTICAL
        setBackgroundColor(KeyboardTheme.BG)
        fitsSystemWindows = false
        applyBottomPadding()

        addView(suggestionBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_DP)))
        addView(buildBody(), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        navBottomPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            insets.getInsets(WindowInsets.Type.navigationBars()).bottom
        } else {
            @Suppress("DEPRECATION") insets.systemWindowInsetBottom
        }
        applyBottomPadding()
        requestLayout()
        return insets
    }

    private fun applyBottomPadding() {
        setPadding(0, 0, 0, navBottomPx + dp(6))
    }

    /** Body row: disambiguation column (left, default) beside the key grid. */
    private fun buildBody(): View {
        val gap = dp(3)
        val body = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(gap, gap, gap, gap)
        }
        body.addView(column, LayoutParams(0, LayoutParams.MATCH_PARENT, COLUMN_WEIGHT))

        val grid = LinearLayout(context).apply { orientation = VERTICAL }
        for (row in T9Layout.rows) grid.addView(buildRow(row))
        body.addView(grid, LayoutParams(0, LayoutParams.MATCH_PARENT, GRID_WEIGHT))
        return body
    }

    fun setSuggestions(candidates: List<Candidate>) = suggestionBar.setCandidates(candidates)

    fun setColumnLetters(letters: List<Char>) = column.setLetters(letters)

    private fun buildRow(keys: List<KeySpec>): View {
        val rowView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        for (key in keys) rowView.addView(buildKey(key))
        return rowView
    }

    /** A TouchPal-style key: rounded face, centred label, small corner number. */
    private fun buildKey(key: KeySpec): View {
        val gap = dp(3)
        val frame = FrameLayout(context).apply {
            background = KeyboardTheme.keyBackground(
                context,
                normal = if (key.isFunction) KeyboardTheme.FUNC_KEY else KeyboardTheme.KEY
            )
            isClickable = true
            isFocusable = true
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(gap, gap, gap, gap)
            }
            setOnClickListener { onKey(key.action) }
        }

        val label = TextView(context).apply {
            text = key.mainLabel
            gravity = Gravity.CENTER
            setTextColor(if (key.isFunction) KeyboardTheme.ACCENT else KeyboardTheme.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.isFunction) 22f else 18f)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { gravity = Gravity.CENTER }
        }
        frame.addView(label)

        if (key.number != null) {
            val number = TextView(context).apply {
                text = key.number
                setTextColor(KeyboardTheme.ACCENT)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, dp(8), dp(5))
                }
            }
            frame.addView(number)
        }
        return frame
    }

    /** Force the whole view to occupy the bar height plus a fraction of the screen. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        val desired = dp(BAR_DP) + (screenH * GRID_HEIGHT_FRACTION).toInt() + navBottomPx + dp(6)
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val BAR_DP = 48
        private const val GRID_HEIGHT_FRACTION = 0.40f
        private const val COLUMN_WEIGHT = 1f   // ~1/6 of the width
        private const val GRID_WEIGHT = 5f
    }
}
