package com.daux.t9keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LongPressKeysTest {

    private fun popupFor(digit: Int, emailField: Boolean = false) =
        LongPressKeys.forKey(KeyAction.Digit(digit), emailField = emailField)

    /**
     * The reason the digit cells exist: the keypad has no 0–9 keys, so without them a
     * number cannot be typed without switching to `12#`. "Exactly one" also catches a
     * digit accidentally offered from two places.
     */
    @Test
    fun `every digit is reachable from exactly one popup`() {
        val keys: List<KeyAction> = listOf(KeyAction.Insert(","), KeyAction.Insert(".")) +
            (1..9).map { KeyAction.Digit(it) }

        for (digit in 0..9) {
            val sources = keys.filter { action ->
                LongPressKeys.forKey(action).any { it.action == KeyAction.Insert(digit.toString()) }
            }
            assertEquals("digit $digit", 1, sources.size)
        }
    }

    @Test
    fun `zero lives on the comma, whose key shows it in the corner`() {
        val comma = LongPressKeys.forKey(KeyAction.Insert(","))
        assertEquals(KeyAction.Insert("0"), comma.first().action)

        val commaKey = T9Layout.bottomRow.first { it.action == KeyAction.Insert(",") }
        assertEquals("0", commaKey.number)
    }

    /**
     * First, not last: it is the only position that means the same thing whether the
     * panel fits on one row or wraps onto two.
     */
    @Test
    fun `the digit is always the first cell and marked as its own kind`() {
        for (digit in 1..9) {
            val cells = popupFor(digit)
            val first = cells.first()
            assertEquals("digit $digit", KeyAction.Insert(digit.toString()), first.action)
            assertTrue("digit $digit is set apart", first.isFunction)
        }
        val email = popupFor(1, emailField = true).first()
        assertEquals(KeyAction.Insert("1"), email.action)
    }

    /** Letter cells force, exactly like the column; only the digit inserts. */
    @Test
    fun `letter keys offer their letters as forced letters`() {
        val cells = popupFor(3).drop(1)
        assertEquals(
            T9Keypad.columnLetters(3),
            cells.map { (it.action as KeyAction.ForceLetter).letter }
        )
        assertTrue(cells.all { (it.action as KeyAction.ForceLetter).digit == 3 })
    }

    @Test
    fun `every accented vowel is reachable from its key's popup`() {
        for ((digit, accents) in T9Keypad.accentedLetters) {
            val offered = popupFor(digit).mapNotNull { (it.action as? KeyAction.ForceLetter)?.letter }
            for (accent in accents) {
                assertTrue("$accent on key $digit", accent in offered)
            }
        }
    }

    /** The invariant SymbolLayoutTest protects, extended to both popup semantics. */
    @Test
    fun `a cell does exactly what it shows`() {
        val all = (1..9).flatMap { popupFor(it) } +
            popupFor(1, emailField = true) +
            LongPressKeys.forKey(KeyAction.Insert(",")) +
            LongPressKeys.forKey(KeyAction.Insert("."), favourites = listOf("§"))

        for (cell in all) {
            when (val action = cell.action) {
                is KeyAction.Insert -> assertEquals(cell.mainLabel, action.text)
                is KeyAction.ForceLetter ->
                    assertEquals(cell.mainLabel, action.letter.toString())
                is KeyAction.InsertPair ->
                    assertEquals(cell.mainLabel, action.open + action.close)
                else -> throw AssertionError("unexpected action on ${cell.mainLabel}")
            }
        }
    }

    @Test
    fun `the bracket cell inserts both halves`() {
        val pair = popupFor(1).map { it.action }.filterIsInstance<KeyAction.InsertPair>().single()
        assertEquals("(", pair.open)
        assertEquals(")", pair.close)
    }

    @Test
    fun `an address field turns key 1 into address parts`() {
        val email = popupFor(1, emailField = true).map { it.mainLabel }
        assertTrue(".com" in email)
        assertTrue("/" in email) // useful in both kinds of field, so present in both
        assertTrue("/" in popupFor(1).map { it.mainLabel })
        assertTrue("€" !in email)
    }

    @Test
    fun `the period offers the favourites it is given`() {
        val favourites = listOf("@", "?", "!")
        val cells = LongPressKeys.forKey(KeyAction.Insert("."), favourites = favourites)
        assertEquals(favourites, cells.map { it.mainLabel })
    }

    /** Backspace owns its touch stream (hold to repeat), so it must stay popup-free. */
    @Test
    fun `keys with a gesture of their own have no alternatives`() {
        assertTrue(LongPressKeys.forKey(KeyAction.Backspace).isEmpty())
        assertTrue(LongPressKeys.forKey(KeyAction.Space).isEmpty())
        assertTrue(LongPressKeys.forKey(KeyAction.Enter).isEmpty())
        assertTrue(LongPressKeys.forKey(KeyAction.Shift).isEmpty())
    }

    @Test
    fun `rows keep the order and stay within the maximum`() {
        val cells = (1..17).map { KeySpec("$it", null, false, KeyAction.Insert("$it")) }
        val rows = LongPressKeys.rows(cells)
        assertTrue(rows.all { it.size <= LongPressKeys.MAX_PER_ROW })
        assertEquals(cells, rows.flatten())
        assertNotNull(LongPressKeys.rows(emptyList()))
    }

    /** A wrapped panel should read as a grid, not as a full row plus a stray cell. */
    @Test
    fun `wrapping splits evenly`() {
        fun sizes(count: Int) =
            LongPressKeys.rows((1..count).map { KeySpec("$it", null, false, KeyAction.Insert("$it")) })
                .map { it.size }

        // Five is the case that made the maximum drop to four: one row of five put its
        // last cell beyond the screen edge on the right-hand column of keys.
        assertEquals(listOf(3, 2), sizes(5))
        assertEquals(listOf(3, 3), sizes(6))
        assertEquals(listOf(4, 4), sizes(8))
        assertEquals(listOf(4, 3), sizes(7))
        // Up to the maximum nothing wraps: one sweep of the thumb is the whole gesture.
        assertEquals(listOf(LongPressKeys.MAX_PER_ROW), sizes(LongPressKeys.MAX_PER_ROW))
    }

    /** No popup should be wide enough to stretch across the screen. */
    @Test
    fun `no popup row is wider than the maximum`() {
        val popups: List<List<KeySpec>> = (1..9).map { popupFor(it) } + listOf(
            popupFor(1, emailField = true),
            LongPressKeys.forKey(KeyAction.Insert(",")),
            LongPressKeys.forKey(KeyAction.Insert("."))
        )

        for (cells in popups) {
            assertTrue(
                cells.joinToString(" ") { it.mainLabel },
                LongPressKeys.rows(cells).all { it.size <= LongPressKeys.MAX_PER_ROW }
            )
        }
    }
}
