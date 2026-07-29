package com.daux.t9keyboard.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoSpaceTest {

    /** The case that decides whether the feature is liked: `casa .` must not happen. */
    @Test
    fun `sentence punctuation hugs the word before it`() {
        for (mark in listOf(".", ",", ";", ":", "!", "?", "…")) {
            assertTrue(mark, AutoSpace.hugsPreviousWord(mark))
        }
    }

    @Test
    fun `closing brackets and quotes hug it too`() {
        for (mark in listOf(")", "]", "}", "»", "”")) {
            assertTrue(mark, AutoSpace.hugsPreviousWord(mark))
        }
    }

    @Test
    fun `opening brackets take a space before and none after`() {
        for (mark in listOf("(", "[", "{", "«", "“")) {
            assertTrue(mark, AutoSpace.deservesPrecedingSpace(mark, 'a'))
            assertFalse(mark, AutoSpace.deservesFollowingSpace(mark, "casa"))
        }
        // Not at the start of a line, and not after another opening symbol.
        assertFalse(AutoSpace.deservesPrecedingSpace("(", null))
        assertFalse(AutoSpace.deservesPrecedingSpace("(", ' '))
    }

    @Test
    fun `closing brackets take a space after and none before`() {
        for (mark in listOf(")", "]", "}", "»", "”")) {
            assertFalse(mark, AutoSpace.deservesPrecedingSpace(mark, 'a'))
            assertTrue(mark, AutoSpace.deservesFollowingSpace(mark, "(casa"))
        }
    }

    /** In Italian the apostrophe joins two words: `l'albero`, never `l' albero`. */
    @Test
    fun `the apostrophe takes no space at all`() {
        assertTrue(AutoSpace.hugsPreviousWord("'"))
        assertFalse(AutoSpace.deservesFollowingSpace("'", "l"))
        assertFalse(AutoSpace.deservesPrecedingSpace("'", 'l'))
    }

    /**
     * What keeps numbers and addresses intact: only after a letter does a mark reliably
     * end a phrase.
     */
    @Test
    fun `a mark after a digit gets no space`() {
        assertTrue(AutoSpace.deservesFollowingSpace(".", "casa"))
        assertFalse(AutoSpace.deservesFollowingSpace(".", "3"))
        assertFalse(AutoSpace.deservesFollowingSpace(",", "3"))
        assertFalse(AutoSpace.deservesFollowingSpace(":", "10"))
        assertFalse(AutoSpace.deservesFollowingSpace(".", ""))
    }

    /** The ellipsis is one mark: only its third dot ends the phrase. */
    @Test
    fun `the ellipsis gets its space only at the third dot`() {
        assertTrue(AutoSpace.deservesFollowingSpace(".", "forse"))   // first dot
        assertFalse(AutoSpace.deservesFollowingSpace(".", "forse."))  // second
        assertTrue(AutoSpace.deservesFollowingSpace(".", "forse.."))  // third completes it
    }

    /** `ecc.` is not the end of a sentence, and must not be given one's spacing. */
    @Test
    fun `an abbreviation's dot ends nothing`() {
        assertFalse(AutoSpace.deservesFollowingSpace(".", "pane, pasta, ecc"))
        assertFalse(AutoSpace.deservesFollowingSpace(".", "il dott"))
    }

    /** The one symbol that is both: after a word it can only be closing. */
    @Test
    fun `a straight quote reads its role from the text`() {
        assertTrue(AutoSpace.straightQuoteCloses('a'))
        assertTrue(AutoSpace.straightQuoteCloses('!'))
        assertFalse(AutoSpace.straightQuoteCloses(' '))
        assertFalse(AutoSpace.straightQuoteCloses(null))
        assertFalse(AutoSpace.straightQuoteCloses('('))
    }

    @Test
    fun `ordinary symbols bring no space of their own`() {
        for (mark in listOf("@", "-", "/", "€", "+")) {
            assertFalse(mark, AutoSpace.deservesFollowingSpace(mark, "casa"))
            assertFalse(mark, AutoSpace.deservesPrecedingSpace(mark, 'a'))
        }
    }
}
