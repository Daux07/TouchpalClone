package com.daux.t9keyboard.engine

/**
 * The single seam between the UI/composition logic and the dictionary.
 *
 * Everything talks to the dictionary only through this interface. That keeps the
 * Phase 2 switch to bilingual painless: a `LanguagePriorityEngine` will merge
 * two sources behind the same `lookup`, and nothing else has to change (plan §5/§8).
 */
interface DictionaryEngine {

    /**
     * Candidates for a T9 digit sequence (e.g. "2272"), most likely first.
     * Returns an empty list when nothing matches.
     */
    fun lookup(sequence: String): List<Candidate>

    /**
     * Words **longer** than what has been typed, whose sequence starts with [prefix]:
     * ten keys for `contemporaneamente`, which is the whole point of a T9 keyboard on
     * a long word. At most [limit], most likely first.
     *
     * Separate from [lookup] rather than folded into it, because it must not be
     * multiplied by the decorators: [FuzzyDictionaryEngine] looks up a hundred-odd
     * sequence variants per keypress, and completing each of them would turn one
     * prefix scan into a hundred.
     *
     * Default empty: only the real indices can answer it, and every decorator that
     * does not forward it simply has no completions to offer.
     */
    fun completions(prefix: String, limit: Int): List<Candidate> = emptyList()
}
