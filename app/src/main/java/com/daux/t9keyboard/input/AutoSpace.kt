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

    /** Punctuation that ends a phrase, and so deserves a space after it. */
    private val ENDS_A_PHRASE = setOf(".", ",", ";", ":", "!", "?", "…")

    /** Brackets and quotes that open: a space belongs *before* them, never after. */
    private val OPENS = setOf("(", "[", "{", "«", "“")

    /** Brackets and quotes that close: nothing before, a space after. */
    private val CLOSES = setOf(")", "]", "}", "»", "”")

    /**
     * In Italian the apostrophe joins two words — `l'albero`, `un'amica`, `quest'anno` —
     * so it takes no space on either side, ever.
     */
    const val APOSTROPHE = "'"

    /**
     * Punctuation that belongs tight against the previous word, so a provisional space
     * before it must go. Closing brackets and quotes are here for the same reason:
     * `(casa )` is wrong exactly as `casa .` is.
     */
    fun hugsPreviousWord(text: String): Boolean =
        text in ENDS_A_PHRASE || text in CLOSES || text == APOSTROPHE

    fun endsAPhrase(text: String): Boolean = text in ENDS_A_PHRASE

    fun opensAPair(text: String): Boolean = text in OPENS

    fun closesAPair(text: String): Boolean = text in CLOSES

    /**
     * Which role a straight double quote is playing. It is the one symbol that both opens
     * and closes, so the text decides: after a letter or a digit it can only be closing
     * one, anywhere else it is opening one.
     */
    fun straightQuoteCloses(precedingChar: Char?): Boolean =
        precedingChar != null && (precedingChar.isLetterOrDigit() || precedingChar in ".,;:!?»”)")

    /**
     * Whether a space belongs after [text] just typed, given the text before it (not yet
     * including [text] itself).
     *
     * The check on what precedes is what keeps numbers and addresses intact: in `3,14`,
     * `10:30` or `www.sito.it` the mark follows a digit or another dot, and a space there
     * would be actively wrong. Only after a letter does a mark reliably end a phrase.
     *
     * The ellipsis is treated as one mark: the second dot gets no space, the third does.
     */
    fun deservesFollowingSpace(text: String, before: CharSequence): Boolean {
        if (text == APOSTROPHE) return false
        if (opensAPair(text)) return false
        if (closesAPair(text)) return true
        if (!endsAPhrase(text)) return false

        if (text == ".") {
            // Mid-ellipsis: `..` is on its way to `...` and must stay tight.
            if (SentenceRules.endsWithPartialEllipsis("$before$text")) return false
            // The third dot completes it, and an ellipsis does end a phrase.
            if (SentenceRules.endsWithEllipsis("$before$text")) return true
            // An abbreviation's dot is not the end of anything.
            if (SentenceRules.endsWithAbbreviation("$before$text")) return false
        }

        return before.lastOrNull()?.isLetter() == true
    }

    /**
     * Whether a space belongs *before* [text]: only for an opening bracket or quote, and
     * only when a word ends right there.
     */
    fun deservesPrecedingSpace(text: String, precedingChar: Char?): Boolean =
        opensAPair(text) && precedingChar != null && precedingChar.isLetterOrDigit()
}
