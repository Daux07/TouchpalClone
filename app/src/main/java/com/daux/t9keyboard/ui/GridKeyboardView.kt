package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import com.daux.t9keyboard.model.KeyGrid

/**
 * Renders any [KeyGrid] as full-width weighted rows — no disambiguation column, no
 * T9 assumptions.
 *
 * Today it draws the symbol pages; the QWERTY alternative to the T9 keypad will be
 * the same view with a different grid.
 */
@SuppressLint("ViewConstructor")
class GridKeyboardView(
    context: Context,
    private val keys: KeyViewFactory
) : LinearLayout(context) {

    private var grid: KeyGrid? = null

    init {
        orientation = VERTICAL
        val gap = dp(3)
        setPadding(gap, gap, gap, gap)
    }

    fun setGrid(grid: KeyGrid) {
        if (this.grid == grid) return
        this.grid = grid
        removeAllViews()
        for (row in grid.rows) {
            addView(keys.row(row.keys), LayoutParams(LayoutParams.MATCH_PARENT, 0, row.weight))
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()
}
