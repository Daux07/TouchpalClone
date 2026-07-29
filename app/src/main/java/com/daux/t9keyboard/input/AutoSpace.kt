package com.daux.t9keyboard.input

/**
 * The rules that make an automatic space feel invisible instead of annoying.
 *
 * Adding a space after a chosen word is the easy half: you can start the next word
 * straight away. The half that decides whether the feature is liked or hated is what
 * happens when the next thing typed is **punctuation** — without a rule, choosing
 * "casa" and typing a full stop leaves `casa .`, and the user ends up deleting a space
 * they never asked for. So an automatic space is provisional: punctuation that hugs the
 * word before it removes the space again, and then puts one after itself, which is where
 * the next sentence begins.
 *
 * Only spaces *we* added are ever removed — a space the user typed is theirs.
 */
object AutoSpace {

    /**
     * Punctuation that belongs tight against the previous word, so a provisional space
     * before it must go. Closing brackets and quotes are in the list for the same
     * reason: `(casa )` is wrong for exactly the reason `casa .` is.
     */
    private val HUGS_PREVIOUS_WORD = setOf(
        ".", ",", ";", ":", "!", "?", "…", ")", "]", "}", "»", "\"", "'", "%"
    )

    /**
     * Punctuation that ends a phrase, and so deserves a space after it. The others
     * (brackets, quotes, apostrophes) do not: `l' altro` and `( casa` would be wrong.
     */
    private val ENDS_A_PHRASE = setOf(".", ",", ";", ":", "!", "?", "…")

    fun hugsPreviousWord(text: String): Boolean = text in HUGS_PREVIOUS_WORD

    fun endsAPhrase(text: String): Boolean = text in ENDS_A_PHRASE

    /**
     * Whether a space belongs after [text] just typed, given the character before it.
     *
     * The check on what precedes is what keeps numbers and addresses intact: in `3.14`
     * or `www.sito.it` the full stop follows a digit or is part of a run, and a space
     * there would be actively wrong. Only after a letter does a full stop reliably mean
     * "end of sentence".
     */
    fun deservesFollowingSpace(text: String, precedingChar: Char?): Boolean =
        endsAPhrase(text) && precedingChar != null && precedingChar.isLetter()
}
