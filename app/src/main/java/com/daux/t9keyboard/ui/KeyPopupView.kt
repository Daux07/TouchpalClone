package com.daux.t9keyboard.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.LongPressKeys
import kotlin.math.hypot

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
            val rowView = LinearLayout(context).apply {
                orientation = HORIZONTAL
                // Rows can differ in width (".com" is wider than "@"): centring keeps a
                // wrapped panel looking like a grid instead of a ragged edge.
                gravity = Gravity.CENTER_HORIZONTAL
            }
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

    /**
     * The cell **nearest** the finger, or -1 if it is too far to count as a choice.
     *
     * Nearest rather than "the first one containing the point", because the panel wraps
     * onto two rows once it grows past [LongPressKeys.MAX_PER_ROW]: any tolerance added
     * around a cell would then overlap the row above or below, and a containment test
     * would always answer with whichever row it happened to visit first. Distance has no
     * such ambiguity — a point between two rows belongs to the closer one.
     *
     * The tolerance is deliberately asymmetric. Sideways it is generous, so sliding past
     * the end of a row still lands on its last cell. Downwards it is tight: the finger
     * that opened the panel is resting on the key just below it, and a release without
     * moving must mean "never mind", not a character chosen at random.
     */
    private fun indexAt(rawX: Float, rawY: Float): Int {
        val location = IntArray(2)
        var nearest = -1
        var nearestDistance = Float.MAX_VALUE

        for ((index, entry) in cells.withIndex()) {
            val cell = entry.first
            cell.getLocationOnScreen(location)
            val dx = gap(rawX, location[0].toFloat(), (location[0] + cell.width).toFloat())
            val dy = gap(rawY, location[1].toFloat(), (location[1] + cell.height).toFloat())
            if (dx > dp(REACH_X_DP) || dy > dp(REACH_Y_DP)) continue

            val distance = hypot(dx, dy)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = index
            }
        }
        return nearest
    }

    /** How far [value] falls outside [min]..[max]; zero when inside. */
    private fun gap(value: Float, min: Float, max: Float): Float = when {
        value < min -> min - value
        value > max -> value - max
        else -> 0f
    }

    fun clearHighlight() = setHighlighted(-1)

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    private companion object {
        const val CELL_MIN_WIDTH_DP = 40
        const val CELL_HEIGHT_DP = 46

        /** Sideways tolerance: sliding past the end of a row still picks its last cell. */
        const val REACH_X_DP = 32

        /** Vertical tolerance, kept under the distance to the key below (see indexAt). */
        const val REACH_Y_DP = 18
    }
}
