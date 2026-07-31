package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElisionTest {

    @Test
    fun anApostropheBetweenLettersIsAnElision() {
        assertTrue(Elision.isElisionAt("l'aveva", 1))
        assertTrue(Elision.isElisionAt("un'amica", 2))
        assertTrue(Elision.isElisionAt("quest'anno", 5))
        assertTrue(Elision.isElisionAt("dell’acqua", 4)) // typographic apostrophe too
    }

    @Test
    fun anApostropheWithoutLettersOnBothSidesIsAQuote() {
        assertFalse(Elision.isElisionAt("'ciao", 0))   // opens a quotation
        assertFalse(Elision.isElisionAt("ciao'", 4))   // closes one
        assertFalse(Elision.isElisionAt("po' di", 2))  // truncation, not an elision
        assertFalse(Elision.isElisionAt("l'aveva", 0)) // not an apostrophe at all
    }

    @Test
    fun headOf_findsTheWordTheApostropheIsJoinedTo() {
        assertEquals("l", Elision.headOf("l'"))
        assertEquals("un", Elision.headOf("un'"))
        assertEquals("quest", Elision.headOf("dice quest'"))
    }

    @Test
    fun headOf_refusesAnApostropheWithNoWordToJoin() {
        assertNull(Elision.headOf("ciao '"))   // after a space: opening a quotation
        assertNull(Elision.headOf("'"))        // start of the field
        assertNull(Elision.headOf("3'"))       // after a digit: minutes, not an elision
        assertNull(Elision.headOf("casa"))     // no apostrophe at all
    }

    @Test
    fun join_learnsTheWholeElidedWord() {
        // The point of the whole rule: what was confirmed is "l'aveva", not "aveva".
        assertEquals("l'aveva", Elision.join("l'", "aveva"))
        assertEquals("quest'anno", Elision.join("dice quest'", "anno"))
    }

    @Test
    fun join_leavesAnOrdinaryWordAlone() {
        assertEquals("aveva", Elision.join("ciao ", "aveva"))
        assertEquals("aveva", Elision.join("ciao '", "aveva")) // a quotation joins nothing
        assertEquals("aveva", Elision.join("", "aveva"))
    }
}
