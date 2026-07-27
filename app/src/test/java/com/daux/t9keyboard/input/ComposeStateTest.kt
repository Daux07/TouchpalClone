package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeStateTest {

    private val state = ComposeState()

    @Test
    fun forcingLetterByLetter_buildsWordAndAdvancesColumn() {
        // Type 2-2-7-2, force "cara".
        state.pressDigit(2)
        assertEquals(2, state.activeColumnDigit()) // column shows key 2 (a/b/c)
        assertTrue(state.chooseLetter('c'))

        state.pressDigit(2)
        assertTrue(state.chooseLetter('a'))
        state.pressDigit(7)
        assertTrue(state.chooseLetter('r'))
        state.pressDigit(2)
        assertTrue(state.chooseLetter('a'))

        assertEquals("cara", state.forcedText())
        assertEquals("2272", state.sequenceString())
        assertNull(state.activeColumnDigit()) // all positions resolved
        assertTrue(state.isForcing())
    }

    @Test
    fun typeAheadThenWalk_columnFollowsPositions() {
        // Whole sequence first, then resolve left-to-right (plan §3.4).
        state.pressDigit(2)
        state.pressDigit(2)
        state.pressDigit(7)
        assertEquals(2, state.activeColumnDigit()) // position 0 -> digit 2
        state.chooseLetter('b')
        assertEquals(2, state.activeColumnDigit()) // position 1 -> digit 2
        state.chooseLetter('a')
        assertEquals(7, state.activeColumnDigit()) // position 2 -> digit 7
        state.chooseLetter('r')
        assertEquals("bar", state.forcedText())
    }

    @Test
    fun chooseLetter_rejectsLetterNotOnActiveDigit() {
        state.pressDigit(2) // a/b/c
        assertFalse(state.chooseLetter('z')) // z is on key 9
        assertEquals("", state.forcedText())
    }

    @Test
    fun backspace_popsWholePair_notJustLetter() {
        state.pressDigit(2); state.chooseLetter('c')
        state.pressDigit(2); state.chooseLetter('a')
        assertEquals("ca", state.forcedText())

        assertTrue(state.backspace()) // removes (2,'a') pair entirely
        assertEquals("c", state.forcedText())
        assertEquals("2", state.sequenceString())
    }

    @Test
    fun backspace_onUnresolvedTrailingDigit_removesDigitOnly() {
        state.pressDigit(2); state.chooseLetter('c')
        state.pressDigit(7) // unresolved trailing digit
        assertTrue(state.backspace())
        assertEquals("c", state.forcedText())
        assertEquals("2", state.sequenceString())
    }

    @Test
    fun backspace_returnsFalseWhenEmpty() {
        assertFalse(state.backspace()) // nothing to delete -> caller handles
    }

    @Test
    fun correctingLastLetter_isSameAsExtending() {
        // Force "ca", then correct the 'a' to 'b': backspace + re-press + choose.
        state.pressDigit(2); state.chooseLetter('c')
        state.pressDigit(2); state.chooseLetter('a')
        state.backspace()               // pop (2,'a')
        state.pressDigit(2); state.chooseLetter('b')
        assertEquals("cb", state.forcedText())
    }

    @Test
    fun defaultLetters_usesFirstLetterOfEachDigit_neverDigits() {
        // Regression: an unknown sequence must preview letters, never raw digits.
        state.pressDigit(2); state.pressDigit(2); state.pressDigit(7); state.pressDigit(2)
        assertEquals("aapa", state.defaultLetters()) // 2→a, 2→a, 7→p, 2→a
    }

    @Test
    fun reset_clearsEverything() {
        state.pressDigit(2); state.chooseLetter('c')
        state.reset()
        assertTrue(state.isEmpty())
        assertFalse(state.isForcing())
        assertEquals("", state.sequenceString())
    }
}
