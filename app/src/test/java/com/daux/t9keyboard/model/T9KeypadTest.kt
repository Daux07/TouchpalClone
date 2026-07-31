package com.daux.t9keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class T9KeypadTest {

    @Test
    fun sequenceFor_mapsLettersToDigits() {
        assertEquals("2272", T9Keypad.sequenceFor("casa"))
        assertEquals("2663", T9Keypad.sequenceFor("come"))
        assertEquals("966", T9Keypad.sequenceFor("zoo"))
    }

    @Test
    fun sequenceFor_skipsTheApostropheOfAnElision() {
        // `l'aveva` is one word typed as its letters: 5 then 2-8-3-8-2, with the
        // apostrophe written back by the keyboard rather than keyed.
        assertEquals("528382", T9Keypad.sequenceFor("l'aveva"))
        assertEquals("8626422", T9Keypad.sequenceFor("un'amica"))
        assertEquals(T9Keypad.sequenceFor("dellacqua"), T9Keypad.sequenceFor("dell’acqua"))
    }

    @Test
    fun sequenceFor_refusesAnApostropheUsedAsAQuote() {
        assertNull(T9Keypad.sequenceFor("'ciao"))
        assertNull(T9Keypad.sequenceFor("ciao'"))
        assertNull(T9Keypad.sequenceFor("po'")) // troncamento: non unisce due parole
    }

    @Test
    fun sequenceFor_isCaseInsensitive() {
        assertEquals(T9Keypad.sequenceFor("casa"), T9Keypad.sequenceFor("CaSa"))
    }

    @Test
    fun sequenceFor_foldsItalianAccents() {
        // Accented word shares the sequence of its base form (plan §1).
        assertEquals(T9Keypad.sequenceFor("perche"), T9Keypad.sequenceFor("perché"))
        assertEquals("83", T9Keypad.sequenceFor("tè"))
    }

    @Test
    fun sequenceFor_returnsNullForUnmappableChars() {
        // Space / punctuation are not letters -> not part of a word sequence.
        assertNull(T9Keypad.sequenceFor("ci ao"))
        assertNull(T9Keypad.sequenceFor("c!"))
    }
}
