package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.T9Layout

/**
 * The T9 surface itself, reproducing the original TouchPal structure:
 *
 * ```
 * ┌──────┬───────────────────────────┬─────────┐
 * │ dis- │  @   abc   def            │   ⌫     │
 * │ amb. │  ghi jkl   mno            │   ⇧     │
 * │ col. │  pqrs tuv  wxyz           │   ☺     │
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

    /** Engaged shift is bright white; at rest the key stays in the accent colour. */
    fun setShiftState(state: ShiftState) {
        val view = shiftKey ?: return
        val color =
            if (state == ShiftState.OFF) KeyboardTheme.ACCENT else KeyboardTheme.TEXT
        keys.updateLabel(view, state.label(), color)
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
            grid.addView(keys.row(row), LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
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
        private const val BOTTOM_ROW_WEIGHT = 0.72f // thinner than the letter rows
    }
}
