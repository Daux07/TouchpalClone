package com.daux.t9keyboard.model

/**
 * A logical action produced by tapping a key. The service decides what each one
 * does; the view only reports them.
 *
 * [Shift], [Emoji] and [Mic] are present for layout fidelity with the original
 * TouchPal but are wired for real in Phase 3; for now they are no-ops.
 */
sealed interface KeyAction {
    /** A numeric key 0..9 (builds the T9 sequence; 0 also acts as space). */
    data class Digit(val n: Int) : KeyAction
    data object Space : KeyAction
    data object Backspace : KeyAction
    data object Enter : KeyAction
    /** Insert literal text such as punctuation (commits the word in progress first). */
    data class Insert(val text: String) : KeyAction

    /** Switch the whole input surface to another [KeyboardMode] ("12#", "abc", "1/2"). */
    data class Mode(val target: KeyboardMode) : KeyAction

    data object Shift : KeyAction        // Phase 3
    data object Emoji : KeyAction        // emoji panel — Phase 3
    data object Mic : KeyAction          // voice input — Phase 3
}
