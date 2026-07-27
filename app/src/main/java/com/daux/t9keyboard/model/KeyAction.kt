package com.daux.t9keyboard.model

/**
 * A logical action produced by tapping a key. The service decides what each one
 * does; the view only reports them. Kept intentionally small for Phase 1.1
 * (multi-tap). Mode switch, shift, symbols etc. are added in later phases.
 */
sealed interface KeyAction {
    /** A numeric key 0..9. In T9, 0 produces a space. */
    data class Digit(val n: Int) : KeyAction
    data object Backspace : KeyAction
    data object Enter : KeyAction
}
