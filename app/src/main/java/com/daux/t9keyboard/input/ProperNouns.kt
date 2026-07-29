package com.daux.t9keyboard.input

/**
 * Words that are written with a capital wherever they appear.
 *
 * The list is not written by hand: it comes from the corpus, which for every word records
 * the share of occurrences that were capitalised (the `P` flag produced by
 * `tools/ConvertLeipzig.java`). Measured evidence rather than judgement, and the
 * difference shows exactly where a hand-written list would have failed — "roma" is a name
 * at 99%, while "rosa" (17%), "viola" (27%) and "bianca" (44%) are the flower, the colour
 * and the adjective, even though each is also a first name. Months and weekdays sit near
 * 5% and are simply never flagged.
 *
 * The set arrives with the corpus, on the loader thread, a moment after the keyboard
 * appears; until then nothing is capitalised automatically, which is the harmless way to
 * be wrong.
 */
object ProperNouns {

    @Volatile
    private var known: Set<String> = emptySet()

    /** Called once the corpus has been parsed. */
    fun setKnown(words: Set<String>) {
        known = words
    }

    fun isProperNoun(word: String): Boolean = word.lowercase() in known

    /** [word] as it should appear in the text: capitalised when it is a proper noun. */
    fun display(word: String): String =
        if (isProperNoun(word)) word.replaceFirstChar { it.uppercaseChar() } else word
}
