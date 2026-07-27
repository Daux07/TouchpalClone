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
 */
data class KeySpec(
    val mainLabel: String,
    val number: String?,
    val isFunction: Boolean,
    val action: KeyAction
)

/** The default 4×3 keypad layout. */
object T9Layout {

    private fun letterKey(n: Int) =
        KeySpec(T9Keypad.labelFor(n)!!, n.toString(), isFunction = false, KeyAction.Digit(n))

    val rows: List<List<KeySpec>> = listOf(
        listOf(
            KeySpec("@", "1", isFunction = false, KeyAction.Digit(1)),
            letterKey(2), letterKey(3)
        ),
        listOf(letterKey(4), letterKey(5), letterKey(6)),
        listOf(letterKey(7), letterKey(8), letterKey(9)),
        listOf(
            KeySpec("⌫", null, isFunction = true, KeyAction.Backspace),
            KeySpec("space", null, isFunction = false, KeyAction.Digit(0)),
            KeySpec("⏎", null, isFunction = true, KeyAction.Enter)
        )
    )
}
