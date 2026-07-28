package com.daux.t9keyboard.model

/**
 * The favourite symbols shown in the disambiguation column while no word is being
 * composed (plan §3: the column is never dead space).
 *
 * Pure list logic, kept apart from where it is stored so it can be unit-tested:
 * [KeyboardSettings][com.daux.t9keyboard.settings.KeyboardSettings] only persists
 * the result.
 */
object FavouriteSymbols {

    /** How many slots the column holds. Beyond ~4 the column starts scrolling. */
    const val COUNT = 7

    /** Everyday punctuation the T9 keypad does not reach in one tap. */
    val DEFAULTS: List<String> = listOf("@", "?", "!", "/", "-", "'", "\"")

    /**
     * Force a stored list back to exactly [COUNT] usable slots: missing or blank
     * entries fall back to the default for that position. Keeps a corrupt or
     * outdated preference (say, saved when COUNT was smaller) from emptying the
     * column.
     */
    fun normalize(symbols: List<String>): List<String> =
        List(COUNT) { i ->
            symbols.getOrNull(i)?.takeIf { it.isNotBlank() } ?: DEFAULTS[i % DEFAULTS.size]
        }

    /**
     * [symbols] with slot [index] replaced by [symbol]. If the symbol is already a
     * favourite elsewhere, the two slots **swap** instead of leaving a duplicate —
     * which makes reordering possible with the same gesture used for replacing.
     */
    fun replace(symbols: List<String>, index: Int, symbol: String): List<String> {
        val current = normalize(symbols)
        if (index !in current.indices || symbol.isBlank()) return current

        val existing = current.indexOf(symbol)
        return current.toMutableList().apply {
            if (existing >= 0) this[existing] = current[index]
            this[index] = symbol
        }
    }
}
