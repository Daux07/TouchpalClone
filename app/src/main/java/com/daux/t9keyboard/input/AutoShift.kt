package com.daux.t9keyboard.input

/**
 * Who decides the capitalisation: the keyboard or the user.
 *
 * Automatic capitals are only welcome while they stay out of the way. The awkward case
 * is not "should this be a capital" — Android answers that through `getCursorCapsMode`,
 * which also knows about fields that want every word capitalised, like a name — but
 * what to do when the user has already said otherwise. Turning shift off at the start of
 * a sentence is a deliberate act, and a keyboard that immediately turns it back on is
 * fighting its user.
 *
 * So the rule is ownership: the keyboard may change only what the keyboard set. A state
 * the user chose is left alone until the word is committed, after which the next word is
 * a fresh decision.
 */
object AutoShift {

    /**
     * The state to move to, or null to leave things as they are.
     *
     * @param current what shift is now
     * @param wanted what the field says would be right here
     * @param automatic whether [current] was set by this mechanism rather than by the user
     */
    fun resolve(current: ShiftState, wanted: ShiftState, automatic: Boolean): ShiftState? = when {
        current == wanted -> null
        // Ours to change back, including switching it off again once past the capital.
        automatic -> wanted
        // Nothing of the user's to lose.
        current == ShiftState.OFF -> wanted
        // The user asked for this. Leave it.
        else -> null
    }
}
