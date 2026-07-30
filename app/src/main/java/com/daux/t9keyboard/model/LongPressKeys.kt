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
     * Five, not four: it keeps the commonest popups (`a b c à 2`, `p q r s 7`) on one
     * row, where a horizontal sweep is all the gesture needs, and splits only the long
     * ones. [rows] balances the split, so six cells give 3+3 rather than 5+1.
     */
    const val MAX_PER_ROW = 5

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
