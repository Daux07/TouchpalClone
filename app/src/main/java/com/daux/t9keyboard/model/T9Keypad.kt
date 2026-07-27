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

    /** Letters shown as the small subtitle under a digit key (e.g. "ABC"). */
    fun subtitleFor(digit: Int): String? = when (digit) {
        in 2..9 -> letters[digit]!!.joinToString("").uppercase()
        else -> null
    }
}

/** One key on screen: what to draw and what it does when tapped. */
data class KeySpec(
    val label: String,
    val subtitle: String?,
    val action: KeyAction
)

/** The default 4×3 keypad layout for Phase 1.1. */
object T9Layout {

    private fun digit(n: Int, label: String = n.toString()) =
        KeySpec(label, T9Keypad.subtitleFor(n), KeyAction.Digit(n))

    val rows: List<List<KeySpec>> = listOf(
        listOf(digit(1), digit(2), digit(3)),
        listOf(digit(4), digit(5), digit(6)),
        listOf(digit(7), digit(8), digit(9)),
        listOf(
            KeySpec("⌫", null, KeyAction.Backspace),   // ⌫
            KeySpec("0", "space", KeyAction.Digit(0)),
            KeySpec("⏎", null, KeyAction.Enter)         // ⏎
        )
    )
}
