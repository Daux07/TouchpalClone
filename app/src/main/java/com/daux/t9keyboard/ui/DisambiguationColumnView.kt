package com.daux.t9keyboard.ui

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The column beside the keypad, which is never dead space:
 *
 * - **while composing** it is the manual disambiguation column (plan §3), showing
 *   the letters of the digit at the current position — tap to force one;
 * - **at rest** it holds the favourite symbols, so everyday punctuation is one tap
 *   away without leaving the keypad. Tap inserts; **long-press** starts replacing
 *   that slot (the service then opens the symbol pages).
 *
 * Cells are sized to **fill the column with 3–4 items** (the usual letter count):
 * with up to 4 they divide the height evenly; with more — the 7 favourites — the
 * cells keep the 4-item size and the strip **scrolls**.
 */
@SuppressLint("ViewConstructor")
class DisambiguationColumnView(
    context: Context,
    private val onPickLetter: (Char) -> Unit,
    private val onPickSymbol: (String) -> Unit,
    private val onEditSymbol: (Int) -> Unit
) : ScrollView(context) {

    private sealed interface Item {
        val label: String

        data class Letter(val char: Char) : Item {
            override val label get() = char.uppercaseChar().toString()
        }

        data class Favourite(val index: Int, val symbol: String) : Item {
            override val label get() = symbol
        }
    }

    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private var items: List<Item> = emptyList()
    private var viewportHeight = 0

    init {
        isVerticalScrollBarEnabled = false
        setBackgroundColor(KeyboardTheme.COLUMN_BG)
        addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    /** Composing: one tappable cell per letter of the current digit. */
    fun setLetters(letters: List<Char>) = setItems(letters.map { Item.Letter(it) })

    /** At rest: the favourite symbols, in slot order. */
    fun setFavourites(symbols: List<String>) =
        setItems(symbols.mapIndexed { i, s -> Item.Favourite(i, s) })

    private fun setItems(items: List<Item>) {
        if (this.items == items) return
        this.items = items
        rebuild()
    }

    /**
     * Cell height depends on the column's height, which is unknown while the input
     * view is being built — and the items can arrive on either side of that moment.
     * Rebuilding here covers both orders (and the re-show of a retained view), which
     * `onSizeChanged` alone does not: it never fires when the size is unchanged.
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (viewportHeight != height) {
            viewportHeight = height
            rebuild()
        } else if (container.childCount != items.size) {
            rebuild()
        }
    }

    private fun rebuild() {
        container.removeAllViews()
        scrollY = 0
        if (items.isEmpty() || viewportHeight == 0) return
        // Divide by the item count (so 3–4 items fill), capped at 4 (more scroll).
        val divisor = items.size.coerceIn(3, 4)
        val cellHeight = (viewportHeight / divisor - dp(4)).coerceAtLeast(dp(30))
        for (item in items) container.addView(buildCell(item, cellHeight))
    }

    private fun buildCell(item: Item, heightPx: Int): View {
        val gap = dp(2)
        return TextView(context).apply {
            text = item.label
            gravity = Gravity.CENTER
            setTextColor(KeyboardTheme.TEXT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            background = KeyboardTheme.keyBackground(
                context,
                normal = KeyboardTheme.COLUMN_CELL,
                pressed = KeyboardTheme.COLUMN_CELL_PRESSED
            )
            isClickable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heightPx
            ).apply { setMargins(gap, gap, gap, gap) }

            when (item) {
                is Item.Letter -> setOnClickListener { onPickLetter(item.char) }
                is Item.Favourite -> {
                    setOnClickListener { onPickSymbol(item.symbol) }
                    setOnLongClickListener { onEditSymbol(item.index); true }
                }
            }
        }
    }

    private fun dp(value: Int): Int =
        (resources.displayMetrics.density * value).toInt()
}
