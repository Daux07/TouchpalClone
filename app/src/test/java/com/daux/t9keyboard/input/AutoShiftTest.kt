package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutoShiftTest {

    @Test
    fun `a capital is offered when nothing is set`() {
        assertEquals(
            ShiftState.ONCE,
            AutoShift.resolve(ShiftState.OFF, ShiftState.ONCE, automatic = false)
        )
    }

    /** Ours to give, ours to take away: past the first word the capital must go. */
    @Test
    fun `an automatic capital is withdrawn again`() {
        assertEquals(
            ShiftState.OFF,
            AutoShift.resolve(ShiftState.ONCE, ShiftState.OFF, automatic = true)
        )
    }

    /**
     * The case that decides whether the feature is help or nuisance: switching shift off
     * at the start of a sentence is deliberate, and must not be undone a moment later.
     */
    @Test
    fun `a capital the user turned off is not turned back on`() {
        assertNull(AutoShift.resolve(ShiftState.OFF, ShiftState.OFF, automatic = false))
    }

    @Test
    fun `a state the user chose is left alone`() {
        assertNull(AutoShift.resolve(ShiftState.LOCK, ShiftState.OFF, automatic = false))
        assertNull(AutoShift.resolve(ShiftState.ONCE, ShiftState.OFF, automatic = false))
        assertNull(AutoShift.resolve(ShiftState.LOCK, ShiftState.ONCE, automatic = false))
    }

    @Test
    fun `nothing to do when the state already matches`() {
        for (state in ShiftState.entries) {
            assertNull(AutoShift.resolve(state, state, automatic = true))
            assertNull(AutoShift.resolve(state, state, automatic = false))
        }
    }

    /** Fields that want everything capitalised (rare, but the platform reports them). */
    @Test
    fun `a field asking for capitals everywhere gets the lock`() {
        assertEquals(
            ShiftState.LOCK,
            AutoShift.resolve(ShiftState.OFF, ShiftState.LOCK, automatic = false)
        )
    }
}
