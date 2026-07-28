package com.daux.t9keyboard.model

/**
 * ITU-T E.161 key mapping and the default on-screen layout.
 *
 * [letters] is the multi-tap order for each digit (used in Phase 1.1). It is also
 * the source of truth for which letters belong to which digit — the predictive
 * engine and the disambiguation column (Phase 1.2+) will reuse the same groups.
 */
object T9Keypad {

    val letters: Map<Int, List<Char>> = mapOf(
        1 to listOf('.', ',', '?', '!', '\''),
        2 to listOf('a', 'b', 'c'),
        3 to listOf('d', 'e', 'f'),
        4 to listOf('g', 'h', 'i'),
        5 to listOf('j', 'k', 'l'),
        6 to listOf('m', 'n', 'o'),
        7 to listOf('p', 'q', 'r', 's'),
        8 to listOf('t', 'u', 'v'),
        9 to listOf('w', 'x', 'y', 'z'),
        0 to listOf(' ')
    )

    /** Letters shown as the main label on a digit key (e.g. "abc"). */
    fun labelFor(digit: Int): String? = when (digit) {
        in 2..9 -> letters[digit]!!.joinToString("")
        else -> null
    }

    /** Reverse map: a→2, b→2, …, z→9. Only a–z letters (no punctuation/space). */
    private val digitForChar: Map<Char, Int> = buildMap {
        for ((digit, chars) in letters) {
            for (c in chars) if (c in 'a'..'z') put(c, digit)
        }
    }

    /** Italian accented vowels fold to their base letter for lookup (plan §1). */
    private val accentFold: Map<Char, Char> = mapOf(
        'à' to 'a', 'á' to 'a',
        'è' to 'e', 'é' to 'e',
        'ì' to 'i', 'í' to 'i',
        'ò' to 'o', 'ó' to 'o',
        'ù' to 'u', 'ú' to 'u'
    )

    /**
     * The T9 digit sequence for a word (e.g. "casa" → "2272"), or null if the word
     * contains a character with no digit mapping. Accents are folded first.
     */
    fun sequenceFor(word: String): String? {
        val sb = StringBuilder(word.length)
        for (raw in word.lowercase()) {
            val c = accentFold[raw] ?: raw
            val digit = digitForChar[c] ?: return null
            sb.append(digit)
        }
        return if (sb.isEmpty()) null else sb.toString()
    }
}

/**
 * One key on screen (TouchPal-style): a large [mainLabel] (lowercase letters for
 * digit keys, or an icon/glyph for function keys) with a small [number] in the
 * corner. [isFunction] keys are tinted with the accent colour and carry no number.
 * [weight] sets the relative width within its row (default 1).
 */
data class KeySpec(
    val mainLabel: String,
    val number: String?,
    val isFunction: Boolean,
    val action: KeyAction,
    val weight: Float = 1f
)

/**
 * The keypad layout, reproducing the original TouchPal structure:
 * - [letterRows]: the central 3×3 letter grid.
 * - [rightColumn]: the right-hand function column (backspace, shift, emoji).
 * - [bottomRow]: the full-width bottom row (12#, comma, space, mic, enter).
 * The disambiguation column on the far left is a separate dynamic view.
 */
object T9Layout {

    private fun letterKey(n: Int) =
        KeySpec(T9Keypad.labelFor(n)!!, n.toString(), isFunction = false, KeyAction.Digit(n))

    val letterRows: List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("@", "1", isFunction = false, KeyAction.Digit(1)),
            letterKey(2), letterKey(3)
        ),
        listOf(letterKey(4), letterKey(5), letterKey(6)),
        listOf(letterKey(7), letterKey(8), letterKey(9))
    )

    val rightColumn: List<KeySpec> = listOf(
        KeySpec("⌫", null, isFunction = true, KeyAction.Backspace),
        KeySpec("⇧", null, isFunction = true, KeyAction.Shift),
        // U+FE0E forces monochrome (outline) rendering instead of a colour emoji.
        KeySpec("☺︎", null, isFunction = true, KeyAction.Emoji)
    )

    /**
     * Full-width bottom row. `12#` sits under the disambiguation column; the rest
     * (comma, space, period, enter) align under the letter grid + right column.
     * Weights sum to the upper area's total (7.6) so the columns line up.
     */
    val bottomRow: List<KeySpec> = listOf(
        KeySpec("12#", null, isFunction = true, KeyAction.Mode(KeyboardMode.SYMBOLS_1), weight = 0.9f),
        KeySpec(",", null, isFunction = false, KeyAction.Insert(","), weight = 1f),
        KeySpec("space", null, isFunction = false, KeyAction.Space, weight = 3.3f),
        KeySpec(".", null, isFunction = false, KeyAction.Insert("."), weight = 1f),
        KeySpec("⏎", null, isFunction = true, KeyAction.Enter, weight = 1.2f)
    )
}
