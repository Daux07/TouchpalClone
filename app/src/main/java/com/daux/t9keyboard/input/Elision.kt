package com.daux.t9keyboard.input

import com.daux.t9keyboard.model.T9Keypad

/**
 * When an apostrophe belongs **inside** a word and when it is a quotation mark.
 *
 * In Italian the apostrophe of an elision joins two words into one — `l'albero`,
 * `un'amica`, `quest'anno`, `dell'acqua`. It is not punctuation between words: it is
 * part of the word, as much as a letter is. The same character used as a quote —
 * `'ciao'` — joins nothing at all.
 *
 * The two are told apart by position, which is all the difference there is: an
 * apostrophe **with a letter on both sides** is an elision, anything else is a quote.
 * That single distinction is what lets the dictionary remember `l'aveva` as one word
 * instead of throwing away the half that made it worth remembering.
 */
object Elision {

    /** Both the typewriter apostrophe and the typographic one. */
    private const val APOSTROPHES = "'’"

    fun isApostrophe(c: Char?): Boolean = c != null && c in APOSTROPHES

    /**
     * True when the character at [index] is an elision's apostrophe: a letter on each
     * side. Out-of-range indices and characters that are not apostrophes are false.
     */
    fun isElisionAt(text: CharSequence, index: Int): Boolean {
        if (index !in text.indices || !isApostrophe(text[index])) return false
        val before = text.getOrNull(index - 1)
        val after = text.getOrNull(index + 1)
        return before?.isLetter() == true && after?.isLetter() == true
    }

    /**
     * The elided head that [before] ends with — `l'`, `un'`, `quest'` — without its
     * apostrophe, or null when [before] does not end in one that joins a word.
     *
     * The head must itself end in letters: an apostrophe after a space, a digit or
     * punctuation is opening a quotation, and has no word to join.
     */
    fun headOf(before: CharSequence): String? {
        if (!isApostrophe(before.lastOrNull())) return null
        val head = before.dropLast(1).takeLastWhile { it.isLetter() }
        return head.ifEmpty { null }?.toString()
    }

    /**
     * [word] as the dictionary should remember it, given the text that precedes it:
     * joined to its elided head when there is one, unchanged otherwise.
     *
     * Learning `aveva` out of `l'aveva` is not wrong so much as useless — it stores the
     * half that the dictionary already knew.
     *
     * **A tail of one plain letter is not joined.** Typing `l` + `'` + `a` composed
     * `l'a` — three characters, so the dictionary accepted it — and it landed on
     * sequence `52` next to `la`. Reproduced on the emulator: for the hour that the
     * recency boost lasts, `52` offered `l'a` **ahead of `la`**, one of the commonest
     * words in the language.
     *
     * The tail has to be accented rather than merely long, because length cannot tell
     * the cases apart: `c'è` is one letter joined to one letter, exactly like `l'a`.
     * What separates them is the accent — the short elisions Italian really has are
     * `c'è`, `n'è`, `s'è`, `v'è`, all ending in one, while the rubbish is always a
     * plain vowel (`l'a`, `l'e`, `l'o`). And `c'è` is worth protecting precisely here:
     * no corpus contains an apostrophe, so being learned is the **only** way it can
     * ever be offered.
     */
    fun join(before: CharSequence, word: String): String {
        val head = headOf(before) ?: return word
        if (word.length == 1 && !T9Keypad.isAccented(word[0])) return word
        return "$head'$word"
    }
}
