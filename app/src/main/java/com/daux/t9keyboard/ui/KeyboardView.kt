package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.KeyboardMode
import com.daux.t9keyboard.model.SymbolLayout
import com.daux.t9keyboard.settings.KeyboardSettings

/**
 * The whole input surface: the suggestion bar on top, and below it the body of the
 * current [KeyboardMode] — the T9 keypad ([T9BodyView]) or a key grid
 * ([GridKeyboardView], used by the symbol pages and, later, by QWERTY).
 *
 * Owns what is common to every mode: the dark background, the navigation-bar inset
 * (targetSdk 35 is edge-to-edge), the keyboard's overall height, and the long-press
 * panel that floats above the keys.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(
    context: Context,
    onKey: (KeyAction) -> Unit,
    onPickCandidate: (Candidate) -> Unit,
    onForgetCandidate: (Candidate) -> Unit,
    onPickLetter: (Char) -> Unit,
    onPickSymbol: (String) -> Unit,
    onEditSymbol: (Int) -> Unit,
    onSettings: () -> Unit,
    private val keyAlternates: (KeySpec) -> List<KeySpec>
) : FrameLayout(context), KeyViewFactory.PopupHost {

    private val settings = KeyboardSettings(context)
    private val keys = KeyViewFactory(context, onKey, this)
    private val suggestionBar = SuggestionBarView(context, onPickCandidate, onForgetCandidate)
    private val t9Body = T9BodyView(context, keys, onPickLetter, onPickSymbol, onEditSymbol)
    private val gridBody = GridKeyboardView(context, keys)

    /**
     * The keyboard proper. It is a child rather than this view itself so the popup can
     * be a sibling drawn on top of it, without a `PopupWindow`.
     */
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val popup = KeyPopupView(context)

    /** The key the popup belongs to; non-null exactly while the popup is showing. */
    private var popupAnchor: View? = null

    /** Where the finger was when it opened — the zero the slide is measured from. */
    private var popupOrigin = PointF()

    private var mode = KeyboardMode.T9

    /** Non-null while the bar is explaining something instead of suggesting words. */
    private var hint: String? = null

    /** Bottom system-bar inset, so keys clear the navigation bar (edge-to-edge). */
    private var navBottomPx = 0

    init {
        setBackgroundColor(KeyboardTheme.BG)
        fitsSystemWindows = false
        applyBottomPadding()

        content.addView(
            topRow(onSettings),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(BAR_DP))
        )
        content.addView(t9Body, bodyParams())
        content.addView(gridBody, bodyParams())

        addView(content, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(popup, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        showMode(KeyboardMode.T9)
    }

    private fun bodyParams() =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)

    /**
     * The settings cog, then the candidates — the strip above the keys (Step 3.1).
     *
     * **Why here and not only in the launcher.** Until now the one way in was the app
     * icon, which means leaving whatever you were writing in to change how you write.
     * The cog sits directly above the disambiguation column, in the column's own strip
     * of width: the leftmost lane of the keyboard is already "not letters", so nothing
     * has to move aside to make room. It uses the same weights and the same 3dp inset as
     * [T9BodyView], which is what makes it line up with the column instead of merely
     * being near it.
     *
     * It stays visible when the bar does not (symbol and emoji pages): the way to the
     * settings should not depend on which surface you happen to be looking at.
     */
    private fun topRow(onSettings: () -> Unit): LinearLayout {
        val cog = TextView(context).apply {
            text = COG
            gravity = Gravity.CENTER
            setTextColor(KeyboardTheme.TEXT_DIM)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            isClickable = true
            setOnClickListener { onSettings() }
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(3), 0, dp(3), 0)
            addView(
                cog,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, COG_WEIGHT)
            )
            addView(
                suggestionBar,
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, BAR_WEIGHT)
            )
        }
    }

    // --- Modes ----------------------------------------------------------------

    fun setMode(mode: KeyboardMode) {
        if (this.mode == mode) return
        this.mode = mode
        showMode(mode)
    }

    private fun showMode(mode: KeyboardMode) {
        val t9 = mode == KeyboardMode.T9
        if (!t9) gridBody.setGrid(SymbolLayout.forMode(mode))
        t9Body.visibility = if (t9) View.VISIBLE else View.GONE
        gridBody.visibility = if (t9) View.GONE else View.VISIBLE
        updateBarVisibility()
    }

    /**
     * Nothing to suggest while typing symbols, so the bar goes invisible — but keeps
     * its space, so the keyboard's height never jumps. A hint brings it back.
     */
    private fun updateBarVisibility() {
        val useful = mode == KeyboardMode.T9 || hint != null
        suggestionBar.visibility = if (useful) View.VISIBLE else View.INVISIBLE
    }

    /** Show a message in place of the suggestions, or null to go back to them. */
    fun setHint(text: String?) {
        hint = text
        if (text != null) suggestionBar.showHint(text)
        updateBarVisibility()
    }

    fun setSuggestions(candidates: List<Candidate>) {
        if (hint != null) return // a pending question outranks the word list
        suggestionBar.setCandidates(candidates)
    }

    fun setColumnLetters(letters: List<Char>) = t9Body.setColumnLetters(letters)

    fun setColumnFavourites(symbols: List<String>) = t9Body.setColumnFavourites(symbols)

    fun setShiftState(state: ShiftState, keysUppercase: Boolean, columnUppercase: Boolean) =
        t9Body.setShiftState(state, keysUppercase, columnUppercase)

    // --- Long-press popup -----------------------------------------------------

    override fun alternatesFor(spec: KeySpec): List<KeySpec> = keyAlternates(spec)

    override fun showPopup(anchor: View, cells: List<KeySpec>, originX: Float, originY: Float) {
        popup.setCells(cells)
        popupAnchor = anchor
        popupOrigin = PointF(originX, originY)
        // Invisible, not gone: it must be measured before it can be placed, and it is
        // placed in onLayout — showing it here would flash it at the top-left corner.
        popup.visibility = View.INVISIBLE
        requestLayout()
    }

    override fun movePopup(rawX: Float, rawY: Float) {
        if (popupAnchor != null) popup.highlightAt(rawX, rawY)
    }

    override fun dismissPopup(rawX: Float, rawY: Float, select: Boolean): KeySpec? {
        val chosen = if (select && popupAnchor != null) popup.selectionAt(rawX, rawY) else null
        hidePopup()
        return chosen
    }

    /** Close the panel unconditionally (mode change, field change). */
    fun hidePopup() {
        if (popupAnchor == null) return
        popup.clearHighlight()
        popup.visibility = View.GONE
        popupAnchor = null
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        positionPopup()
    }

    /**
     * Centre the panel over its key and lift it clear of the finger, kept inside the
     * keyboard: keys in the first and last columns would otherwise push a panel wider
     * than themselves off the screen.
     *
     * The gap above the key is [POPUP_GAP_DP], deliberately more than the hairline it
     * started as: every dp of it is a dp the hand does not cover. It buys nothing for the
     * **top row of keys**, though, where the panel is already pinned to the top of the
     * keyboard — the IME window is all the room there is, and a panel two rows tall does
     * not fit between the top of the keyboard and a key 61dp below it. There the finger
     * ends up under the panel no matter what, which is why the vertical gain carries more
     * of the work than the horizontal one.
     */
    private fun positionPopup() {
        val anchor = popupAnchor ?: return
        if (popup.width == 0 || popup.height == 0) return

        val rect = Rect(0, 0, anchor.width, anchor.height)
        offsetDescendantRectToMyCoords(anchor, rect)

        val opening = popup.visibility != View.VISIBLE
        val maxX = (width - popup.width).coerceAtLeast(0)
        val x = (rect.centerX() - popup.width / 2).coerceIn(0, maxX)
        val y = (rect.top - popup.height - dp(POPUP_GAP_DP)).coerceAtLeast(0)

        // Translation is relative to where the layout put it, not to the origin.
        popup.translationX = (x - popup.left).toFloat()
        popup.translationY = (y - popup.top).toFloat()
        popup.visibility = View.VISIBLE

        // The panel tracks the finger from above, measuring from where the finger was when
        // the panel opened — the geometry it needs on top of that it reads off itself.
        popup.setTracking(popupOrigin.x, popupOrigin.y)

        // Only as it appears: a later re-layout must not throw away a choice the finger
        // has meanwhile slid onto.
        if (opening) popup.highlightResting()
    }

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

    /**
     * Take the height and the candidate text size from the settings again (Step 3.3).
     *
     * Called when the keyboard comes back to a field, and by the settings screen on every
     * movement of a slider — which is what makes the preview there follow the finger. The
     * height is not applied here but at [onMeasure]; this only asks for a new measurement.
     */
    fun applySizeSettings() {
        suggestionBar.textSizeSp = settings.candidateTextSp.toFloat()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        // Read here rather than cached: this is the one place the height is used, and a
        // measurement happens far too rarely for a preferences lookup to matter.
        val body = (screenH * settings.bodyHeightFraction()).toInt()
        val desired = dp(BAR_DP) + body + navBottomPx + dp(6)
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        /**
         * The candidate bar, nearly halved (Step 1.25). It was sized to let the text
         * breathe; what it actually did was spend a strip of screen on air above and
         * below a single line of words. The text shrank with it, so the words still
         * have their margin — there is just far less of nothing around them.
         */
        private const val BAR_DP = 32

        /**
         * The keypad's height is **no longer a constant** (Step 3.3): it is
         * `KeyboardSettings.bodyHeightPercent`, read at every [onMeasure].
         *
         * How the default got where it is, because the number means nothing without it.
         * It was 0.34 until Step 1.25, which lowered it: the keys were coming out very
         * nearly square, which reads as a numeric keypad rather than a keyboard and spends
         * on height what the text above needs more. Wider-than-tall is also the shape a
         * thumb actually hits — horizontal error is the common one, vertical is not. Step
         * 2.5 then raised it from 0.28 to 0.287, by the user's leave, for the single
         * purpose of paying for a bottom row a tenth taller than a letter row
         * ([T9BodyView.BOTTOM_ROW_WEIGHT]): the arithmetic was 0.28 × 4.1/4 and nothing
         * more.
         *
         * Nothing else needs to change to make it adjustable, and that is the point of
         * having built the layout out of **weights**: every key is a share of whatever
         * height this comes out to, so moving it reproportions the lot uniformly instead
         * of stretching one row.
         */

        /** Clearance between the popup and the key that opened it. See [positionPopup]. */
        private const val POPUP_GAP_DP = 10

        /**
         * `U+FE0E` is the variation selector that asks for the **text** shape of the cog
         * rather than the colour emoji one — the same trick the `☺︎` key uses. Without it
         * Android draws a full-colour gear, which would be the loudest thing on a
         * keyboard whose every other glyph is one flat colour.
         */
        private const val COG = "⚙︎"

        /**
         * The cog's share of the strip, and the candidates'. They add up to the 7.4 of
         * [T9BodyView] and the cog takes the column's own 0.9, so it sits exactly over
         * the column instead of approximately over it.
         */
        private const val COG_WEIGHT = 0.9f
        private const val BAR_WEIGHT = 6.5f
    }
}
