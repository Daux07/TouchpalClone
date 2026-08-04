package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.T9Layout

/**
 * The T9 surface itself, reproducing the original TouchPal structure:
 *
 * ```
 * ┌──────┬───────────────────────────┬─────────┐
 * │ dis- │  @   abc   def            │   ☺     │
 * │ amb. │  ghi jkl   mno            │   ⇧     │
 * │ col. │  pqrs tuv  wxyz           │   ⌫     │
 * │      ├───────────────────────────┴─────────┤
 * │      │ 12# ,   [   space   ]  .   ⏎        │
 * └──────┴─────────────────────────────────────┘
 * ```
 *
 * Sizing is proportional (weights), so it scales cleanly between the Galaxy S25 and
 * S25 Ultra (plan §6). Display-only: taps leave through the shared [KeyViewFactory]
 * and the column's own callback.
 */
@SuppressLint("ViewConstructor")
class T9BodyView(
    context: Context,
    private val keys: KeyViewFactory,
    onPickLetter: (Char) -> Unit,
    onPickSymbol: (String) -> Unit,
    onEditSymbol: (Int) -> Unit
) : LinearLayout(context) {

    private val column =
        DisambiguationColumnView(context, onPickLetter, onPickSymbol, onEditSymbol)

    /** Kept so the glyph can follow the capitalisation state. */
    private var shiftKey: View? = null

    /** Letter keys and their specs, so their labels can follow the case. */
    private val letterKeys = mutableListOf<Pair<View, KeySpec>>()

    init {
        orientation = VERTICAL
        val gap = dp(3)
        setPadding(gap, gap, gap, gap)
        // Upper area (3 rows) over a shorter, full-width bottom row (thinner keys).
        addView(buildUpperArea(), LayoutParams(LayoutParams.MATCH_PARENT, 0, 3f))
        addView(
            keys.row(T9Layout.bottomRow),
            LayoutParams(LayoutParams.MATCH_PARENT, 0, BOTTOM_ROW_WEIGHT)
        )
    }

    fun setColumnLetters(letters: List<Char>) = column.setLetters(letters)

    fun setColumnFavourites(symbols: List<String>) = column.setFavourites(symbols)

    /**
     * Reflect capitalisation everywhere it is visible: the shift glyph (bright white
     * when engaged, accent colour at rest), the letter keys and the column.
     *
     * The two flags are separate on purpose. With a one-shot shift only the *next*
     * character is capitalised, and the keypad and the column are at different
     * positions in the word: the keys type the character after the last digit, the
     * column resolves the first unresolved one.
     */
    fun setShiftState(state: ShiftState, keysUppercase: Boolean, columnUppercase: Boolean) {
        shiftKey?.let { view ->
            val color =
                if (state == ShiftState.OFF) KeyboardTheme.ACCENT else KeyboardTheme.TEXT
            keys.updateLabel(view, state.label(), color)
        }
        for ((view, spec) in letterKeys) {
            val label =
                if (keysUppercase) spec.mainLabel.uppercase() else spec.mainLabel.lowercase()
            keys.updateLabel(view, label, KeyboardTheme.TEXT)
        }
        column.setUppercase(columnUppercase)
    }

    /** Disambiguation column + central 3×3 letter grid + right-hand function column. */
    private fun buildUpperArea(): LinearLayout {
        val upper = LinearLayout(context).apply { orientation = HORIZONTAL }

        // Far-left disambiguation column — stops above the bottom row.
        upper.addView(column, LayoutParams(0, LayoutParams.MATCH_PARENT, COLUMN_WEIGHT))

        val grid = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, LETTERS_WEIGHT)
        }
        for (row in T9Layout.letterRows) {
            val rowView = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (spec in row) {
                val view = keys.key(spec, LayoutParams(0, LayoutParams.MATCH_PARENT, spec.weight))
                if (spec.action is KeyAction.Digit) letterKeys += view to spec
                rowView.addView(view)
            }
            grid.addView(rowView, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        }
        upper.addView(grid)

        val funcColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, FUNC_COLUMN_WEIGHT)
        }
        for (key in T9Layout.rightColumn) {
            val view = keys.key(key, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            if (key.action is KeyAction.Shift) shiftKey = view
            funcColumn.addView(view)
        }
        upper.addView(funcColumn)
        return upper
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val COLUMN_WEIGHT = 0.9f
        private const val LETTERS_WEIGHT = 5.4f
        private const val FUNC_COLUMN_WEIGHT = 1.1f
        /**
         * A tenth taller than a letter row (Step 2.5). It used to be 0.72 — deliberately
         * thinner, since the space bar and its neighbours are not letters. That held
         * while the keyboard was tall; once the whole thing came down (Step 1.25) the
         * same fraction of a smaller number left a row too shallow to aim at, and the
         * space bar is the most-pressed key there is. Step 1.26 levelled it with the
         * letter rows; the user still missed the space bar now and then, so it now gets
         * a little more than they have.
         *
         * The whole row grows, not just the space bar: a bottom row of uneven keys would
         * read as a mistake rather than as a bigger target.
         *
         * **The extra tenth is new height, not height taken from the letters.** The user
         * allowed the keyboard to grow, so [KeyboardView.BODY_HEIGHT_FRACTION] went up by
         * exactly this row's share and the three letter rows keep the size they had. That
         * is what lets this number stay readable: at a fixed body height the same 10%
         * taller row would have meant a weight of 1.14, because a weight is a share of
         * the whole and the whole would have been growing too.
         */
        private const val BOTTOM_ROW_WEIGHT = 1.1f
    }
}
