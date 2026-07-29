package com.daux.t9keyboard.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.LongPressKeys

/**
 * The panel shown above a key while it is held down.
 *
 * It never receives touch events of its own: the finger belongs to the key that
 * opened it, which keeps forwarding coordinates here ([highlightAt]) until it lifts
 * ([selectionAt]). That is what makes hold-slide-release work as one gesture, and it
 * is why this can be an ordinary child view instead of a `PopupWindow` — no window
 * token, nothing that can outlive the keyboard and leak.
 */
class KeyPopupView(context: Context) : LinearLayout(context) {

    private val cells = mutableListOf<Pair<View, KeySpec>>()
    private var highlighted = -1

    init {
        orientation = VERTICAL
        val pad = dp(4)
        setPadding(pad, pad, pad, pad)
        background = KeyboardTheme.keyBackground(
            context,
            normal = KeyboardTheme.POPUP_BG,
            pressed = KeyboardTheme.POPUP_BG,
            radiusDp = 12f
        )
        elevation = dp(8).toFloat()
        visibility = GONE
    }

    fun setCells(specs: List<KeySpec>) {
        removeAllViews()
        cells.clear()
        highlighted = -1
        for (row in LongPressKeys.rows(specs)) {
            val rowView = LinearLayout(context).apply { orientation = HORIZONTAL }
            for (spec in row) {
                val cell = buildCell(spec)
                cells += cell to spec
                rowView.addView(cell)
            }
            addView(rowView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun buildCell(spec: KeySpec): View = TextView(context).apply {
        text = spec.mainLabel
        gravity = Gravity.CENTER
        // Function cells are the key's own digit: accent-coloured, like the little
        // number in the key's corner it stands for, so it reads apart from the letters.
        setTextColor(if (spec.isFunction) KeyboardTheme.ACCENT else KeyboardTheme.TEXT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
        minWidth = dp(CELL_MIN_WIDTH_DP)
        setPadding(dp(6), 0, dp(6), 0)
        background = KeyboardTheme.keyBackground(context, normal = KeyboardTheme.POPUP_BG)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(CELL_HEIGHT_DP))
            .apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    }

    /** Follow the finger: light up the cell under it, if any. */
    fun highlightAt(rawX: Float, rawY: Float) = setHighlighted(indexAt(rawX, rawY))

    /** The cell under the finger when it lifted, or null if it was outside. */
    fun selectionAt(rawX: Float, rawY: Float): KeySpec? =
        indexAt(rawX, rawY).takeIf { it >= 0 }?.let { cells[it].second }

    private fun setHighlighted(index: Int) {
        if (index == highlighted) return
        cells.getOrNull(highlighted)?.first?.isPressed = false
        cells.getOrNull(index)?.first?.isPressed = true
        highlighted = index
    }

    private fun indexAt(rawX: Float, rawY: Float): Int {
        val location = IntArray(2)
        for ((index, entry) in cells.withIndex()) {
            val cell = entry.first
            cell.getLocationOnScreen(location)
            // Vertically forgiving: the finger sits below the popup while sliding, and
            // demanding it be inside the row would make selection feel finicky.
            val insideX = rawX >= location[0] && rawX <= location[0] + cell.width
            val insideY = rawY >= location[1] - dp(TOUCH_SLOP_DP) &&
                rawY <= location[1] + cell.height + dp(TOUCH_SLOP_DP)
            if (insideX && insideY) return index
        }
        return -1
    }

    fun clearHighlight() = setHighlighted(-1)

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    private companion object {
        const val CELL_MIN_WIDTH_DP = 40
        const val CELL_HEIGHT_DP = 46

        /** Extra vertical reach, since the finger is below the popup while sliding. */
        const val TOUCH_SLOP_DP = 28
    }
}
