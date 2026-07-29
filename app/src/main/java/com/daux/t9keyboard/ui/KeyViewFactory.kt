package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec

/**
 * Builds the TouchPal-style key views — rounded face with a pressed state, centred
 * label, optional small corner number.
 *
 * Shared by every surface (T9 keypad, symbol pages, and the planned QWERTY) so they
 * cannot drift apart visually: a change to how a key looks happens once, here.
 */
class KeyViewFactory(
    private val context: Context,
    private val onKey: (KeyAction) -> Unit,
    private val popups: PopupHost? = null
) {

    /**
     * Whoever can display a long-press panel above a key — implemented by the root
     * keyboard view, which is the only thing that knows where the key sits.
     *
     * [alternatesFor] is asked at press time, not when the key is built, because the
     * answer changes with the state: the favourite symbols, the capitalisation, and
     * whether the field is an email address all affect it.
     */
    interface PopupHost {
        fun alternatesFor(spec: KeySpec): List<KeySpec>

        /** [originX]/[originY]: where the finger is now, the zero of the slide. */
        fun showPopup(anchor: View, cells: List<KeySpec>, originX: Float, originY: Float)
        fun movePopup(rawX: Float, rawY: Float)

        /** Closes the panel; returns the cell under the finger when [select]. */
        fun dismissPopup(rawX: Float, rawY: Float, select: Boolean): KeySpec?
    }

    /** A horizontal row; each key's width comes from its [KeySpec.weight]. */
    fun row(keys: List<KeySpec>): View {
        val rowView = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        for (spec in keys) {
            rowView.addView(
                key(spec, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, spec.weight))
            )
        }
        return rowView
    }

    fun key(spec: KeySpec, lp: LinearLayout.LayoutParams): View {
        val gap = dp(3)
        lp.setMargins(gap, gap, gap, gap)
        val frame = FrameLayout(context).apply {
            layoutParams = lp
            background = KeyboardTheme.keyBackground(
                context,
                normal = if (spec.isFunction) KeyboardTheme.FUNC_KEY else KeyboardTheme.KEY
            )
            isClickable = true
            isFocusable = true
            if (spec.action is KeyAction.Backspace) {
                // Backspace owns its touch stream (hold to repeat), so it gets no popup.
                attachHoldToDelete(this)
            } else {
                setOnClickListener { onKey(spec.action) }
                attachLongPressPopup(this, spec)
            }
        }

        frame.addView(
            TextView(context).apply {
                text = spec.mainLabel
                gravity = Gravity.CENTER
                setTextColor(labelColor(spec))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize(spec))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).apply { gravity = Gravity.CENTER }
            }
        )

        if (spec.number != null) {
            frame.addView(
                TextView(context).apply {
                    text = spec.number
                    setTextColor(KeyboardTheme.ACCENT)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(0, 0, dp(8), dp(5))
                    }
                }
            )
        }
        return frame
    }

    /**
     * Backspace repeats while held, and **speeds up**: single characters at first,
     * then whole words once deleting letter by letter has stopped being useful
     * (holding it to clear a sentence was painfully slow otherwise).
     *
     * Handled entirely through touch — no click listener — so a plain tap deletes
     * exactly once; the pressed look is driven by hand since we consume the events.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachHoldToDelete(view: View) {
        val handler = Handler(Looper.getMainLooper())
        var repeats = 0
        lateinit var tick: Runnable
        tick = Runnable {
            repeats++
            onKey(if (repeats > CHARS_BEFORE_WORDS) KeyAction.DeleteWord else KeyAction.Backspace)
            handler.postDelayed(
                tick,
                if (repeats > CHARS_BEFORE_WORDS) WORD_INTERVAL_MS else CHAR_INTERVAL_MS
            )
        }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    repeats = 0
                    onKey(KeyAction.Backspace) // the tap itself
                    handler.postDelayed(tick, HOLD_DELAY_MS)
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(tick)
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Hold a key to open its alternatives, slide onto one, release to pick it — the
     * gesture everyone already knows from Gboard.
     *
     * The whole thing is one touch stream, which is what lets sliding work. A key with
     * no alternatives is left completely alone: the listener declines the gesture at
     * `ACTION_DOWN` and the ordinary click listener handles it exactly as before. When
     * we do take over, a release without an open panel still fires [View.performClick],
     * so a plain tap behaves the same and accessibility keeps working.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun attachLongPressPopup(view: View, spec: KeySpec) {
        val host = popups ?: return
        val handler = Handler(Looper.getMainLooper())
        var handling = false
        var open = false
        var cells: List<KeySpec> = emptyList()
        // The panel opens 400 ms after the press, so the finger's position has to be
        // remembered as it moves: it is the origin the slide is measured from.
        var lastX = 0f
        var lastY = 0f

        val openPopup = Runnable {
            open = true
            host.showPopup(view, cells, lastX, lastY)
        }

        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cells = host.alternatesFor(spec)
                    handling = cells.isNotEmpty()
                    if (handling) {
                        v.isPressed = true
                        open = false
                        lastX = event.rawX
                        lastY = event.rawY
                        handler.postDelayed(openPopup, HOLD_DELAY_MS)
                    }
                    handling
                }

                MotionEvent.ACTION_MOVE -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    if (handling && open) host.movePopup(event.rawX, event.rawY)
                    handling
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!handling) return@setOnTouchListener false
                    val cancelled = event.actionMasked == MotionEvent.ACTION_CANCEL
                    v.isPressed = false
                    handler.removeCallbacks(openPopup)
                    if (open) {
                        host.dismissPopup(event.rawX, event.rawY, select = !cancelled)
                            ?.let { onKey(it.action) }
                    } else if (!cancelled) {
                        v.performClick()
                    }
                    handling = false
                    open = false
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Restyle a key built by [key] — used for the shift key, whose glyph and colour
     * follow the capitalisation state. The label is the frame's first child.
     */
    fun updateLabel(keyView: View, text: String, color: Int) {
        val label = (keyView as? FrameLayout)?.getChildAt(0) as? TextView ?: return
        label.text = text
        label.setTextColor(color)
    }

    private fun labelColor(spec: KeySpec): Int = when {
        spec.mainLabel == "space" -> KeyboardTheme.TEXT_DIM
        spec.isFunction -> KeyboardTheme.ACCENT
        else -> KeyboardTheme.TEXT
    }

    private fun labelSize(spec: KeySpec): Float = when {
        // Worded mode keys ("12#", "abc", "1/2") — but not the glyph ones (☺).
        spec.action is KeyAction.Mode && spec.mainLabel.first().isLetterOrDigit() -> 15f
        spec.mainLabel == "space" -> 15f
        spec.isFunction -> 20f
        // Single glyphs (symbols, emoji) get more presence than "abc" clusters.
        // Counted in code points, so an emoji made of a surrogate pair still counts as one.
        spec.mainLabel.codePointCount(0, spec.mainLabel.length) == 1 -> 22f
        else -> 18f
    }

    private fun dp(value: Int): Int =
        (context.resources.displayMetrics.density * value).toInt()

    private companion object {
        /** Long enough that a normal tap never starts the repeat. */
        const val HOLD_DELAY_MS = 400L
        const val CHAR_INTERVAL_MS = 55L

        /** After this many characters, holding switches to whole words. */
        const val CHARS_BEFORE_WORDS = 10

        /** Slower than characters: a word is a much bigger step. */
        const val WORD_INTERVAL_MS = 140L
    }
}
