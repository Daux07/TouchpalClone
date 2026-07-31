package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposeStateTest {

    private val state = ComposeState()

    @Test
    fun adopt_takesOverAWrittenWordAsForcedLetters() {
        assertTrue(state.adopt("far"))

        assertEquals("327", state.sequenceString())
        // Forced, not merely typed: the letters must survive the ranking.
        assertEquals("far", state.forcedText())
        assertTrue(state.isForcing())
        assertNull(state.activeColumnDigit())
    }

    @Test
    fun adopt_keepsTheCaseOutOfTheStateButNotTheLetters() {
        assertTrue(state.adopt("Farla"))
        assertEquals("farla", state.forcedText())
        assertEquals("32752", state.sequenceString())
    }

    @Test
    fun adopt_readsAccentsBackToTheirKey() {
        assertTrue(state.adopt("farà"))
        assertEquals("3272", state.sequenceString())
        assertEquals("farà", state.forcedText())
    }

    @Test
    fun adopt_refusesWhatTheKeypadCannotWrite() {
        assertFalse(state.adopt("far.")) // punctuation has no digit
        assertTrue(state.isEmpty())      // and the state is left untouched
    }

    @Test
    fun adoptedWordThenDigits_showsTheWholeWordNotJustTheForcedPart() {
        // The user parks the cursor after "far" and types l-a to reach "farla".
        assertTrue(state.adopt("far"))
        state.pressDigit(5)
        state.pressDigit(2)

        assertEquals("32752", state.sequenceString())
        // The forced prefix is kept and the new keys are visible — as their *default*
        // letters, which is all this class can know: "farja", not "farla". Turning that
        // tail into the real word is the dictionary's job (the service looks 32752 up
        // and keeps only the candidates starting with "far"). What matters here is that
        // the two keys are not invisible, which is what forcedText() alone would give.
        assertEquals("farja", state.forcedPreview())
        assertEquals("far", state.forcedText())
    }

    @Test
    fun forcedPreview_fillsUnresolvedDigitsWithTheirDefaultLetter() {
        state.pressDigit(2)
        assertTrue(state.chooseLetter('c'))
        state.pressDigit(2) // unresolved → its default letter 'a'
        state.pressDigit(7) // unresolved → its default letter 'p'

        assertEquals("cap", state.forcedPreview())
        assertEquals("c", state.forcedText()) // the forced part is unchanged
    }

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
    fun accentedVowels_canBeForcedFromTheirOwnKey() {
        // "perché": the é is offered by key 3, alongside d/e/f.
        state.pressDigit(7); state.chooseLetter('p')
        state.pressDigit(3); state.chooseLetter('e')
        state.pressDigit(7); state.chooseLetter('r')
        state.pressDigit(2); state.chooseLetter('c')
        state.pressDigit(4); state.chooseLetter('h')
        state.pressDigit(3)
        assertTrue(state.chooseLetter('é'))

        assertEquals("perché", state.forcedText())
        // The sequence stays plain, so the word is looked up and learned normally.
        assertEquals("737243", state.sequenceString())
    }

    @Test
    fun accentedVowel_isRejectedOnTheWrongKey() {
        state.pressDigit(2) // a/b/c + à
        assertFalse(state.chooseLetter('è')) // è belongs to key 3
        assertTrue(state.chooseLetter('à'))
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
