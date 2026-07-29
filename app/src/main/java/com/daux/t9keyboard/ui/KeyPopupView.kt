package com.daux.t9keyboard.ui

import android.content.Context
import android.graphics.PointF
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

    /** Where the finger was when the panel opened, and the key it belongs to (screen px). */
    private var originX = 0f
    private var originY = 0f
    private var anchorCenterY = 0f

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
        setTextColor(restingColor(spec))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
        minWidth = dp(CELL_MIN_WIDTH_DP)
        setPadding(dp(6), 0, dp(6), 0)
        // The cell under the finger fills with the accent colour, the way Gboard marks a
        // choice. An earlier, subtler shade was invisible here: the panel already sits on
        // a light grey, so "slightly lighter grey" said nothing. Filling does.
        background = KeyboardTheme.keyBackground(
            context,
            normal = KeyboardTheme.POPUP_BG,
            pressed = KeyboardTheme.ACCENT
        )
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(CELL_HEIGHT_DP))
            .apply { setMargins(dp(2), dp(2), dp(2), dp(2)) }
    }

    /**
     * Function cells are the key's own digit: accent-coloured, like the little number in
     * the key's corner they stand for, so they read apart from the letters.
     */
    private fun restingColor(spec: KeySpec): Int =
        if (spec.isFunction) KeyboardTheme.ACCENT else KeyboardTheme.TEXT

    /**
     * Where the finger was when the panel opened, and how far above it the selection
     * runs. The finger stays down on the keypad and the highlight tracks **above** it,
     * so the hand never covers the very characters you are choosing between — the trick
     * Gboard uses. Set together with the position, in screen coordinates.
     */
    fun setTracking(originX: Float, originY: Float, anchorCenterY: Float) {
        this.originX = originX
        this.originY = originY
        this.anchorCenterY = anchorCenterY
    }

    /** Follow the finger: light up the cell it points at, if any. */
    fun highlightAt(rawX: Float, rawY: Float) = setHighlighted(indexAt(rawX, rawY))

    /** The cell the finger pointed at when it lifted, or null if it pointed at none. */
    fun selectionAt(rawX: Float, rawY: Float): KeySpec? =
        indexAt(rawX, rawY).takeIf { it >= 0 }?.let { cells[it].second }

    /**
     * The finger's position translated into the panel: the same distance left or right,
     * but lifted so that resting on the key points at the **bottom row**, and moving up
     * by a cell reaches the row above.
     *
     * Returns null while the finger has not really moved, which keeps a long press that
     * ends where it began from choosing anything: opening the panel and letting go must
     * stay a way to change your mind.
     */
    private fun pointerInPanel(rawX: Float, rawY: Float): PointF? {
        if (hypot(rawX - originX, rawY - originY) < dp(MOVE_THRESHOLD_DP)) return null

        val bottomRowCentre = cells.lastOrNull()?.first?.let { cell ->
            val location = IntArray(2)
            cell.getLocationOnScreen(location)
            location[1] + cell.height / 2f
        } ?: return null

        return PointF(rawX, rawY - (anchorCenterY - bottomRowCentre))
    }

    /**
     * The glyph flips to the panel's own dark colour on the accent fill: the digit cell
     * is already accent-coloured, so leaving it as it is would make it vanish into its
     * own highlight.
     */
    private fun setHighlighted(index: Int) {
        if (index == highlighted) return
        cells.getOrNull(highlighted)?.let { (view, spec) ->
            view.isPressed = false
            (view as TextView).setTextColor(restingColor(spec))
        }
        cells.getOrNull(index)?.let { (view, _) ->
            view.isPressed = true
            (view as TextView).setTextColor(KeyboardTheme.BG)
        }
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
     * The point tested is not the finger itself but [pointerInPanel]'s translation of it,
     * so everything here works in panel coordinates while the hand stays out of the way.
     *
     * The tolerance is generous sideways — sliding past the end of a row still lands on
     * its last cell — and tighter vertically, so drifting a long way off the panel gives
     * back "nothing selected" instead of clinging to the nearest edge.
     */
    private fun indexAt(rawX: Float, rawY: Float): Int {
        val pointer = pointerInPanel(rawX, rawY) ?: return -1
        val location = IntArray(2)
        var nearest = -1
        var nearestDistance = Float.MAX_VALUE

        for ((index, entry) in cells.withIndex()) {
            val cell = entry.first
            cell.getLocationOnScreen(location)
            val dx = gap(pointer.x, location[0].toFloat(), (location[0] + cell.width).toFloat())
            val dy = gap(pointer.y, location[1].toFloat(), (location[1] + cell.height).toFloat())
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

        /** Vertical tolerance around a row, once the finger has been translated up. */
        const val REACH_Y_DP = 30

        /** Below this the finger counts as still, and nothing is selected yet. */
        const val MOVE_THRESHOLD_DP = 10
    }
}
