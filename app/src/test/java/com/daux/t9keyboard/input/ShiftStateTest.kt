package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftStateTest {

    @Test
    fun `the key cycles off, once, lock`() {
        assertEquals(ShiftState.ONCE, ShiftState.OFF.next())
        assertEquals(ShiftState.LOCK, ShiftState.ONCE.next())
        assertEquals(ShiftState.OFF, ShiftState.LOCK.next())
    }

    @Test
    fun `capitalisation follows the state`() {
        assertEquals("casa", ShiftState.OFF.apply("casa"))
        assertEquals("Casa", ShiftState.ONCE.apply("casa"))
        assertEquals("CASA", ShiftState.LOCK.apply("casa"))
    }

    @Test
    fun `accented words capitalise too`() {
        assertEquals("È", ShiftState.ONCE.apply("è"))
        assertEquals("PERCHÉ", ShiftState.LOCK.apply("perché"))
    }

    @Test
    fun `an empty word is left alone`() {
        for (state in ShiftState.entries) assertEquals("", state.apply(""))
    }

    @Test
    fun `one-shot shift is spent by a commit, caps lock is not`() {
        assertEquals(ShiftState.OFF, ShiftState.ONCE.afterCommit())
        assertEquals(ShiftState.LOCK, ShiftState.LOCK.afterCommit())
        assertEquals(ShiftState.OFF, ShiftState.OFF.afterCommit())
    }

    @Test
    fun `caps lock has its own glyph`() {
        assertEquals("⇧", ShiftState.OFF.label())
        assertEquals("⇧", ShiftState.ONCE.label())
        assertEquals("⇪", ShiftState.LOCK.label())
    }
}
