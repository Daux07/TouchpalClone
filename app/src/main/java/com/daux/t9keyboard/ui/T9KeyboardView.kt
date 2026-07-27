package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.T9Layout

/**
 * The on-screen keyboard: a responsive grid of keys.
 *
 * Sizing is proportional, not fixed: rows share the height via layout weights and
 * keys share each row's width via weights, so the same layout scales cleanly
 * between the Galaxy S25 and S25 Ultra without device-specific dimensions
 * (see plan §6). The overall height is a fraction of the screen height.
 *
 * The view is display-only: every tap is reported through [onKey]; the service
 * owns all input logic.
 */
@SuppressLint("ViewConstructor")
class T9KeyboardView(
    context: Context,
    private val onKey: (com.daux.t9keyboard.model.KeyAction) -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setBackgroundColor(BG)
        val gap = dp(2)
        setPadding(gap, gap, gap, gap)

        for (row in T9Layout.rows) {
            addView(buildRow(row))
        }
    }

    private fun buildRow(keys: List<KeySpec>): View {
        val rowView = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        for (key in keys) rowView.addView(buildKey(key))
        return rowView
    }

    private fun buildKey(key: KeySpec): View {
        val gap = dp(3)
        val tv = TextView(context).apply {
            text = keyLabel(key)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setBackgroundColor(KEY_BG)
            isClickable = true
            isFocusable = true
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(gap, gap, gap, gap)
            }
            setOnClickListener { onKey(key.action) }
        }
        return tv
    }

    /** Big label with a smaller, dimmer subtitle underneath (e.g. "2" / "ABC"). */
    private fun keyLabel(key: KeySpec): CharSequence {
        val sub = key.subtitle ?: return key.label
        val full = "${key.label}\n$sub"
        return SpannableString(full).apply {
            val subStart = key.label.length + 1
            setSpan(RelativeSizeSpan(0.55f), subStart, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(SUB_COLOR), subStart, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** Force the keyboard to occupy a fraction of the screen height. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val screenH = resources.displayMetrics.heightPixels
        val desired = (screenH * KEYBOARD_HEIGHT_FRACTION).toInt()
        val hSpec = MeasureSpec.makeMeasureSpec(desired, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, hSpec)
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()

    companion object {
        private const val KEYBOARD_HEIGHT_FRACTION = 0.42f
        private const val BG = 0xFF1E1E1E.toInt()
        private const val KEY_BG = 0xFF2C2C2C.toInt()
        private val SUB_COLOR = 0xFF9E9E9E.toInt()
    }
}
