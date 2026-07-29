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
        for (mark in listOf(")", "]", "}", "»", "\"")) {
            assertTrue(mark, AutoSpace.hugsPreviousWord(mark))
        }
    }

    @Test
    fun `opening brackets and ordinary symbols do not`() {
        for (mark in listOf("(", "[", "«", "@", "€", "+", "/", "-")) {
            assertFalse(mark, AutoSpace.hugsPreviousWord(mark))
        }
    }

    /** `l' altro` and `( casa` would be wrong, so not everything that hugs also opens. */
    @Test
    fun `only phrase endings deserve a space after them`() {
        assertTrue(AutoSpace.endsAPhrase("."))
        assertTrue(AutoSpace.endsAPhrase("?"))
        assertFalse(AutoSpace.endsAPhrase(")"))
        assertFalse(AutoSpace.endsAPhrase("'"))
        assertFalse(AutoSpace.endsAPhrase("\""))
    }

    /**
     * What keeps numbers and addresses intact: only after a letter does a full stop
     * reliably mean the end of a sentence.
     */
    @Test
    fun `a full stop after a digit gets no space`() {
        assertTrue(AutoSpace.deservesFollowingSpace(".", 'a'))
        assertFalse(AutoSpace.deservesFollowingSpace(".", '3'))
        assertFalse(AutoSpace.deservesFollowingSpace(".", '.'))
        assertFalse(AutoSpace.deservesFollowingSpace(".", ' '))
        assertFalse(AutoSpace.deservesFollowingSpace(".", null))
    }

    @Test
    fun `symbols never bring a space of their own`() {
        assertFalse(AutoSpace.deservesFollowingSpace("@", 'a'))
        assertFalse(AutoSpace.deservesFollowingSpace("(", 'a'))
        assertFalse(AutoSpace.deservesFollowingSpace("-", 'a'))
    }
}
