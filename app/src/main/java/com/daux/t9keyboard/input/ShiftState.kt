package com.daux.t9keyboard.input

/**
 * Capitalisation, T9-style: it applies to the **word**, not to single keypresses,
 * because in predictive input the letters are decided by the dictionary, not by the
 * keys. Tapping `⇧` cycles OFF → ONCE → LOCK → OFF.
 */
enum class ShiftState {
    /** Everything lowercase. */
    OFF,

    /** Next word capitalised ("Casa"); consumed once that word is committed. */
    ONCE,

    /** Everything uppercase until switched off ("CASA"). */
    LOCK;

    fun next(): ShiftState = when (this) {
        OFF -> ONCE
        ONCE -> LOCK
        LOCK -> OFF
    }

    /** [word] rendered according to this state. */
    fun apply(word: String): String = when {
        word.isEmpty() -> word
        this == OFF -> word
        this == LOCK -> word.uppercase()
        else -> word.replaceFirstChar { it.uppercaseChar() }
    }

    /** What the state becomes once a word has been committed. */
    fun afterCommit(): ShiftState = if (this == ONCE) OFF else this

    /** The glyph on the shift key. */
    fun label(): String = if (this == LOCK) "⇪" else "⇧"
}
