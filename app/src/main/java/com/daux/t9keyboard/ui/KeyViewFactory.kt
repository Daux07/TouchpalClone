package com.daux.t9keyboard.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
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
    private val onKey: (KeyAction) -> Unit
) {

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
            setOnClickListener { onKey(spec.action) }
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
}
