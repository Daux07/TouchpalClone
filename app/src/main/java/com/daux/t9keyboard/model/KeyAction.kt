package com.daux.t9keyboard.model

/**
 * A logical action produced by tapping a key. The service decides what each one
 * does; the view only reports them.
 *
 * [Mic] is present for layout fidelity with the original TouchPal but is wired for
 * real in Phase 3; for now it is a no-op.
 */
sealed interface KeyAction {
    /** A numeric key 0..9 (builds the T9 sequence; 0 also acts as space). */
    data class Digit(val n: Int) : KeyAction
    data object Space : KeyAction
    data object Backspace : KeyAction

    /**
     * Delete a whole word. Not on any key: it is what holding [Backspace] turns
     * into once deleting one character at a time has stopped being useful.
     */
    data object DeleteWord : KeyAction
    data object Enter : KeyAction
    /** Insert literal text such as punctuation (commits the word in progress first). */
    data class Insert(val text: String) : KeyAction

    /**
     * Insert a pair and leave the cursor **between** the two halves — what you
     * actually want from brackets: open, type, close becomes a single gesture.
     */
    data class InsertPair(val open: String, val close: String) : KeyAction

    /**
     * Force [letter] as the resolution of a [digit] press — exactly what tapping the
     * disambiguation column does, reached from a key's long-press popup instead.
     *
     * The digit travels with the letter because it cannot always be derived from it:
     * accented vowels have no entry in the reverse letter→digit map.
     */
    data class ForceLetter(val digit: Int, val letter: Char) : KeyAction

    /** Switch the whole input surface to another [KeyboardMode] ("12#", "abc", "1/2"). */
    data class Mode(val target: KeyboardMode) : KeyAction

    /** Cycle capitalisation: off → next word capitalised → caps lock. */
    data object Shift : KeyAction

    data object Mic : KeyAction          // voice input — Phase 3
}
