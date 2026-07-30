package com.daux.t9keyboard.model

/**
 * What each key offers when held down (Gboard-style popup).
 *
 * The cells are ordinary [KeySpec]s, so the popup is drawn by the same key factory as
 * every other surface and each cell simply carries the action it performs. Two
 * semantics live side by side, on purpose:
 *
 * - on the digit keys 2–9 a letter cell **forces** that letter into the word
 *   ([KeyAction.ForceLetter]) — the same operation as tapping the disambiguation
 *   column, so the popup is a positional shortcut for it rather than a second
 *   mechanism with its own rules;
 * - everything else **inserts** text, committing the word in progress first.
 *
 * The letters come from [T9Keypad.columnLetters], the single source of truth the
 * column already uses, so popup and column cannot drift apart.
 */
object LongPressKeys {

    /**
     * Cells per row before wrapping. Beyond this a panel gets wider than the thumb can
     * comfortably sweep — eight across covered most of the screen — so long lists become
     * a small grid instead.
     *
     * **Four, down from five in Step 1.12k**, which puts the five-cell popups (`2 a b c à`,
     * `7 p q r s`) on 3+2 instead of one long row. That is not only a matter of taste: with
     * the gesture starting on the first cell, the last cell of a row of five is four cell
     * pitches away — ~188dp of panel, ~125dp of finger at the horizontal gain — and on the
     * **right-hand column of keys** (`6`, `9`) there is only ~117dp of screen left of the
     * key, so the last cell could not be reached without running the finger off the edge.
     * Wrapping halves the horizontal reach and spends the difference on a downward step,
     * which has room.
     *
     * Not lower than four: [rows] balances the split, so at four a six-cell panel is still
     * 3+3 and an eight-cell one 4+4, while at three the eight-cell `1` popup would grow to
     * three rows — taller, and height is the scarce dimension (see `KeyboardView`).
     */
    const val MAX_PER_ROW = 4

    private fun letter(digit: Int, c: Char) =
        KeySpec(c.toString(), null, isFunction = false, KeyAction.ForceLetter(digit, c))

    private fun sym(text: String) =
        KeySpec(text, null, isFunction = false, KeyAction.Insert(text))

    private fun pair(open: String, close: String) =
        KeySpec(open + close, null, isFunction = false, KeyAction.InsertPair(open, close))

    /**
     * The key's own number, always the **first** cell so its position never moves, and
     * marked as a function key so it is drawn in the accent colour — the same teal as
     * the little number in the key's corner, which is what it stands for.
     *
     * First rather than last (the user's call, changed in Step 1.12f): a panel can be one
     * row or two, and only the opening cell is in the same place in both — the last cell
     * moves from "end of the row" to "bottom right" as soon as the list wraps.
     *
     * Without these cells the digits are unreachable without switching to `12#`: the
     * keypad has no 0–9 keys at all, its number labels being just labels.
     */
    private fun digitCell(n: Int) =
        KeySpec(n.toString(), null, isFunction = true, KeyAction.Insert(n.toString()))

    /**
     * The `1` key: what the other surfaces make expensive. Brackets are split across
     * both `12#` pages, currency and maths live on page 2 (three gestures away).
     *
     * Deliberately **not** here: everyday punctuation (`,` and `.` are their own keys,
     * `? ! - ' "` are favourites), and the rarer maths/brackets, which stay on `12#`.
     * `@` is on the key face too, but it is the key's defining symbol and seeing it in
     * the popup is what tells you it is there.
     */
    private val key1: List<KeySpec> = listOf(
        digitCell(1),
        sym("@"), pair("(", ")"), sym("/"), sym("%"), sym("+"), sym("="), sym("€")
    )

    /** In an email or URL field the same key turns into the parts of an address. */
    private val key1Email: List<KeySpec> = listOf(
        digitCell(1),
        sym("@"), sym(".com"), sym(".it"), sym(".net"), sym(".org"), sym("/")
    )

    /**
     * The comma also hosts **0**: the space bar would be its natural home (in
     * ITU-T E.161 the zero key *is* space), but that long-press is reserved for
     * sliding the cursor — a feature worth more than a shorter path to one digit.
     * The key shows a small `0` in its corner like every other numbered key.
     */
    private val comma: List<KeySpec> = listOf(
        digitCell(0), sym(","), sym(";"), sym(":"), sym("\"")
    )

    /**
     * The alternatives for a key, or an empty list if it has none.
     *
     * [favourites] are passed in rather than read here because they are user data:
     * the `.` popup shows the very same symbols as the column at rest, so changing a
     * favourite changes both, by construction. [emailField] switches the `1` key to
     * address parts, the way Gboard and iOS surface `.com` only where it is wanted.
     */
    fun forKey(
        action: KeyAction,
        favourites: List<String> = FavouriteSymbols.DEFAULTS,
        emailField: Boolean = false
    ): List<KeySpec> = when {
        action is KeyAction.Digit && action.n == 1 -> if (emailField) key1Email else key1
        action is KeyAction.Digit && action.n in 2..9 ->
            listOf(digitCell(action.n)) + T9Keypad.columnLetters(action.n).map { letter(action.n, it) }
        action is KeyAction.Insert && action.text == "," -> comma
        // The favourites, reachable here even mid-word — which the column is not, since
        // it turns into letters as soon as you start composing.
        action is KeyAction.Insert && action.text == "." -> favourites.map { sym(it) }
        else -> emptyList()
    }

    /** Split into rows for display, keeping the given order. */
    fun rows(cells: List<KeySpec>): List<List<KeySpec>> {
        if (cells.size <= MAX_PER_ROW) return listOf(cells)
        val rows = (cells.size + MAX_PER_ROW - 1) / MAX_PER_ROW
        val perRow = (cells.size + rows - 1) / rows
        return cells.chunked(perRow)
    }
}
