package com.daux.t9keyboard.model

/**
 * A row of keys. [weight] is the row's share of the grid height, so a layout can
 * have a thinner bottom row (as both the T9 keypad and a QWERTY do).
 */
data class KeyRow(val keys: List<KeySpec>, val weight: Float = 1f)

/**
 * A plain grid of keys spanning the full width — the QWERTY shape.
 *
 * Deliberately *not* T9-specific: the symbol pages (Phase 1.8) and the QWERTY
 * alternative to the T9 keypad (planned) are both just a [KeyGrid] handed to
 * `GridKeyboardView`. Key widths inside a row come from [KeySpec.weight], so rows
 * with a wide space bar or a double-width backspace need no special casing.
 */
data class KeyGrid(val rows: List<KeyRow>)
