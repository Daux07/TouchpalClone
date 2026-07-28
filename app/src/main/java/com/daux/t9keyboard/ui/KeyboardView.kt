package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeyboardMode
import com.daux.t9keyboard.model.SymbolLayout

/**
 * The whole input surface: the suggestion bar on top, and below it the body of the
 * current [KeyboardMode] — the T9 keypad ([T9BodyView]) or a key grid
 * ([GridKeyboardView], used by the symbol pages and, later, by QWERTY).
 *
 * Owns what is common to every mode: the dark background, the navigation-bar inset
 * (targetSdk 35 is edge-to-edge) and the keyboard's overall height.
 */
@SuppressLint("ViewConstructor")
class KeyboardView(
    context: Context,
    onKey: (KeyAction) -> Unit,
    onPickCandidate: (Candidate) -> Unit,
    onPickLetter: (Char) -> Unit,
    onPickSymbol: (String) -> Unit,
    onEditSymbol: (Int) -> Unit
) : LinearLayout(context) {

    private val keys = KeyViewFactory(context, onKey)
    private val suggestionBar = SuggestionBarView(context, onPickCandidate)
    private val t9Body = T9BodyView(context, keys, onPickLetter, onPickSymbol, onEditSymbol)
    private val gridBody = GridKeyboardView(context, keys)

    private var mode = KeyboardMode.T9

    /** Non-null while the bar is explaining something instead of suggesting words. */
    private var hint: String? = null

    /** Bottom system-bar inset, so keys clear the navigation bar (edge-to-edge). */
    private var navBottomPx = 0

    init {
        orientation = VERTICAL
        setBackgroundColor(KeyboardTheme.BG)
        fitsSystemWindows = false
        applyBottomPadding()

        addView(suggestionBar, LayoutParams(LayoutParams.MATCH_PARENT, dp(BAR_DP)))
        addView(t9Body, bodyParams())
        addView(gridBody, bodyParams())
        showMode(KeyboardMode.T9)
    }

    private fun bodyParams() = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)

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
