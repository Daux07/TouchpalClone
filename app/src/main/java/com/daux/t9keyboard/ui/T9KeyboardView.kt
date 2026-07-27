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
 * The whole input surface, reproducing the classic TouchPal T9 structure:
 *
 * ```
 * ┌───────────────────────────────────────────┐
 * │ suggestion bar                             │
 * ├──────┬───────────────────────────┬─────────┤
 * │ dis- │  @   abc   def             │   ⌫     │
 * │ amb. │  ghi jkl   mno             │   ⇧     │
 * │ col. │  pqrs tuv  wxyz            │   ☺     │
 * │      ├───────────────────────────┴─────────┤
 * │      │ 12# ,   [   space   ] 🎙   ⏎         │
 * └──────┴─────────────────────────────────────┘
 * ```
 *
 * Sizing is proportional (weights), so it scales cleanly between the Galaxy S25
 * and S25 Ultra (plan §6); the dark palette and rounded keys mimic the original.
 * Display-only: taps go out through [onKey]/[onPickCandidate]/[onPickLetter].
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

    // --- Layout ---------------------------------------------------------------

    private fun buildBody(): View {
        val gap = dp(3)
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(gap, gap, gap, gap)
        }
        // Upper area (3 rows) over a shorter, full-width bottom row (thinner keys).
        body.addView(buildUpperArea(), LayoutParams(LayoutParams.MATCH_PARENT, 0, 3f))
        body.addView(buildRow(T9Layout.bottomRow), LayoutParams(LayoutParams.MATCH_PARENT, 0, BOTTOM_ROW_WEIGHT))
        return body
    }

    /** Disambiguation column + central 3×3 letter grid + right-hand function column. */
    private fun buildUpperArea(): View {
        val upper = LinearLayout(context).apply { orientation = HORIZONTAL }

        // Far-left disambiguation column — stops above the bottom row.
        upper.addView(column, LayoutParams(0, LayoutParams.MATCH_PARENT, COLUMN_WEIGHT))

        val grid = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, LETTERS_WEIGHT)
        }
        for (row in T9Layout.letterRows) {
            grid.addView(buildRow(row), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
        upper.addView(grid)

        val funcColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, FUNC_COLUMN_WEIGHT)
        }
        for (key in T9Layout.rightColumn) {
            funcColumn.addView(buildKey(key, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)))
        }
        upper.addView(funcColumn)
        return upper
    }

    private fun buildRow(keys: List<KeySpec>): View {
        val rowView = LinearLayout(context).apply { orientation = HORIZONTAL }
        for (key in keys) {
            rowView.addView(buildKey(key, LayoutParams(0, LayoutParams.MATCH_PARENT, key.weight)))
        }
        return rowView
    }

    /** A TouchPal-style key: rounded face, centred label, optional corner number. */
    private fun buildKey(key: KeySpec, lp: LayoutParams): View {
        val gap = dp(3)
        lp.setMargins(gap, gap, gap, gap)
        val frame = FrameLayout(context).apply {
            layoutParams = lp
            background = KeyboardTheme.keyBackground(
                context,
                normal = if (key.isFunction) KeyboardTheme.FUNC_KEY else KeyboardTheme.KEY
            )
            isClickable = true
            isFocusable = true
            setOnClickListener { onKey(key.action) }
        }

        val label = TextView(context).apply {
            text = key.mainLabel
            gravity = Gravity.CENTER
            setTextColor(labelColor(key))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize(key))
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

    private fun labelColor(key: KeySpec): Int = when {
        key.mainLabel == "space" -> KeyboardTheme.TEXT_DIM
        key.isFunction -> KeyboardTheme.ACCENT
        else -> KeyboardTheme.TEXT
    }

    private fun labelSize(key: KeySpec): Float = when {
        key.action is KeyAction.ModeSwitch -> 15f
        key.mainLabel == "space" -> 15f
        key.isFunction -> 20f
        else -> 18f
    }

    fun setSuggestions(candidates: List<Candidate>) = suggestionBar.setCandidates(candidates)

    fun setColumnLetters(letters: List<Char>) = column.setLetters(letters)

    // --- Insets & sizing ------------------------------------------------------

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

    private fun applyBottomPadding() = setPadding(0, 0, 0, navBottomPx + dp(6))

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        val desired = dp(BAR_DP) + (screenH * BODY_HEIGHT_FRACTION).toInt() + navBottomPx + dp(6)
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val BAR_DP = 48
        private const val BODY_HEIGHT_FRACTION = 0.44f
        private const val COLUMN_WEIGHT = 0.9f
        private const val LETTERS_WEIGHT = 5.4f
        private const val FUNC_COLUMN_WEIGHT = 1.1f
        private const val BOTTOM_ROW_WEIGHT = 0.72f // thinner than the letter rows
    }
}
