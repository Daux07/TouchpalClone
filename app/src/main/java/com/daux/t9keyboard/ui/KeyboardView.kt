package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.KeyboardMode
import com.daux.t9keyboard.model.SymbolLayout

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
    onPickLetter: (Char) -> Unit,
    onPickSymbol: (String) -> Unit,
    onEditSymbol: (Int) -> Unit,
    private val keyAlternates: (KeySpec) -> List<KeySpec>
) : FrameLayout(context), KeyViewFactory.PopupHost {

    private val keys = KeyViewFactory(context, onKey, this)
    private val suggestionBar = SuggestionBarView(context, onPickCandidate)
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
            suggestionBar,
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
     */
    private fun positionPopup() {
        val anchor = popupAnchor ?: return
        if (popup.width == 0 || popup.height == 0) return

        val rect = Rect(0, 0, anchor.width, anchor.height)
        offsetDescendantRectToMyCoords(anchor, rect)

        val opening = popup.visibility != View.VISIBLE
        val maxX = (width - popup.width).coerceAtLeast(0)
        val x = (rect.centerX() - popup.width / 2).coerceIn(0, maxX)
        val y = (rect.top - popup.height - dp(2)).coerceAtLeast(0)

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        val desired = dp(BAR_DP) + (screenH * BODY_HEIGHT_FRACTION).toInt() + navBottomPx + dp(6)
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        // Tall enough to leave the candidates room to breathe at DEFAULT_TEXT_SP.
        private const val BAR_DP = 56
        private const val BODY_HEIGHT_FRACTION = 0.34f // less tall; keys wider than tall
    }
}
