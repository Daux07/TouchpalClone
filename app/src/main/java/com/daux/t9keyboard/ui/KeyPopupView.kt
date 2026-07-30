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

    /**
     * What is selected while the finger has not moved: the **digit**, when the panel opens
     * with one, otherwise nothing.
     *
     * So holding a numbered key and letting go simply types its number — no aiming — which
     * is the shortest path there is to a digit on a keypad that has no 0–9 keys. It costs
     * the escape hatch of Step 1.12d (open the panel, release, nothing happens): to back
     * out now you slide off the panel, where nothing is selected, and release there.
     *
     * On the `.` panel, which is favourites only, resting still selects nothing: no cell
     * there is the obvious default, and picking one at random would type it by surprise.
     */
    private var restingIndex = -1

    /** Where the finger was when the panel opened (screen px): the zero of the gesture. */
    private var originX = 0f
    private var originY = 0f

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
        // The digit is the first cell (LongPressKeys) and the only one marked as a
        // function key, which is exactly what makes it recognisable here without the
        // panel having to know anything about digits.
        restingIndex = if (cells.firstOrNull()?.second?.isFunction == true) 0 else -1
    }

    /** Light up what a release without moving would choose. Called when the panel opens. */
    fun highlightResting() = setHighlighted(restingIndex)

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
     * Where the finger was when the panel opened, in screen coordinates. The finger stays
     * down on the keypad and the highlight tracks **above** it, so the hand never covers
     * the very characters you are choosing between — the trick Gboard uses. That spot is
     * the zero of the gesture: everything the finger does afterwards is measured from it.
     */
    fun setTracking(originX: Float, originY: Float) {
        this.originX = originX
        this.originY = originY
    }

    /** Follow the finger: light up the cell it points at, if any. */
    fun highlightAt(rawX: Float, rawY: Float) = setHighlighted(indexAt(rawX, rawY))

    /** The cell the finger pointed at when it lifted, or null if it pointed at none. */
    fun selectionAt(rawX: Float, rawY: Float): KeySpec? =
        indexAt(rawX, rawY).takeIf { it >= 0 }?.let { cells[it].second }

    /**
     * The finger's position translated into the panel: it starts on [anchorPoint] and
     * moves from there, amplified.
     *
     * Two things shape the mapping, and they belong together:
     *
     * - **The zero is the finger, not the key.** Where inside the key the press landed
     *   must not decide what starts out selected; measuring from [originX]/[originY]
     *   makes "hasn't moved" mean the same cell every time. It also makes the gesture
     *   identical for a key at the edge of the keyboard, whose panel gets nudged back
     *   inside the screen and so no longer sits centred over it.
     * - **The movement is amplified**, more upwards than sideways ([VERTICAL_GAIN],
     *   [HORIZONTAL_GAIN]). One-to-one tracking meant the panel had to be crossed at its
     *   real size: the row above cost a whole row pitch of travel (about the height of a
     *   key) and the far end of a five-cell row two cell widths. Reaching either meant
     *   walking the finger onto the panel, the very thing tracking from below exists to
     *   avoid.
     *
     * Returns null while the finger has not really moved, which hands the choice to
     * [restingIndex] instead of to whatever cell happens to be nearest.
     */
    private fun pointerInPanel(rawX: Float, rawY: Float): PointF? {
        if (hypot(rawX - originX, rawY - originY) < dp(MOVE_THRESHOLD_DP)) return null
        val anchor = anchorPoint() ?: return null

        return PointF(
            anchor.x + (rawX - originX) * HORIZONTAL_GAIN,
            anchor.y + (rawY - originY) * VERTICAL_GAIN
        )
    }

    /**
     * Where in the panel a motionless finger points: **the centre of the cell that is
     * already selected**, so the first movement slides off it instead of jumping.
     *
     * Getting this wrong is not cosmetic. Until Step 1.12i the anchor was the middle of
     * the bottom row while the highlight sat on the first cell — two different places, so
     * crossing the 10dp threshold teleported the selection: on `jkl` (`5 j k l`) a nudge
     * to the right went from `5` straight to `k`, skipping `j`.
     *
     * With the digit first the anchor is the top-left cell, which is why the rest of the
     * panel is now reached by moving right and **down**. That is the trade the user chose:
     * a gesture that starts where the eye already is, rather than one that starts nearest
     * the finger.
     *
     * The `.` panel has no preselected cell (no digit among the favourites), and there the
     * old anchor is still the right one: nothing is selected, so the finger should point
     * at the row closest to it.
     */
    private fun anchorPoint(): PointF? {
        val location = IntArray(2)
        cells.getOrNull(restingIndex)?.first?.let { cell ->
            cell.getLocationOnScreen(location)
            return PointF(location[0] + cell.width / 2f, location[1] + cell.height / 2f)
        }

        val bottomRow = cells.lastOrNull()?.first ?: return null
        bottomRow.getLocationOnScreen(location)
        val bottomRowCentreY = location[1] + bottomRow.height / 2f
        // Rows are centred inside the panel, so the panel's own centre is every row's.
        getLocationOnScreen(location)
        return PointF(location[0] + width / 2f, bottomRowCentreY)
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
        val pointer = pointerInPanel(rawX, rawY) ?: return restingIndex
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

        /**
         * Sideways tolerance: sliding past the end of a row still picks its last cell.
         * Measured in panel space, so [POINTER_GAIN] halves what it costs the finger —
         * hence the generous value. Being generous here is safe: reach only decides
         * whether *anything* is selected, never which cell wins, which is always the
         * nearest one.
         */
        const val REACH_X_DP = 60

        /** Vertical tolerance around a row, once the finger has been translated up. */
        const val REACH_Y_DP = 30

        /**
         * How much panel movement one unit of finger movement buys. The two axes are
         * **not** the same, because the two problems are not the same: sideways the
         * distances are short (one cell) and too much gain makes the highlight skittish,
         * while upwards the finger must cover a whole row pitch *and* stay off a panel
         * that sits right above it, so it needs more help.
         *
         * Tuned by hand on the emulator and then on the phone: 2/2 was reported as too
         * fast sideways and still not enough upwards.
         */
        const val HORIZONTAL_GAIN = 1.5f

        /** See [HORIZONTAL_GAIN]: the row above is ~20dp of travel away. */
        const val VERTICAL_GAIN = 2.5f

        /** Below this the finger counts as still, and nothing is selected yet. */
        const val MOVE_THRESHOLD_DP = 10
    }
}
