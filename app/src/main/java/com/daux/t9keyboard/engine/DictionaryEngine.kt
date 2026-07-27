package com.daux.t9keyboard.engine

/**
 * The single seam between the UI/composition logic and the dictionary.
 *
 * Everything talks to the dictionary only through this interface. That keeps the
 * Phase 2 switch to bilingual painless: a `BilingualDictionaryEngine` will merge
 * two sources behind the same `lookup`, and nothing else has to change (plan §5/§8).
 */
interface DictionaryEngine {

    /**
     * Candidates for a T9 digit sequence (e.g. "2272"), most likely first.
     * Returns an empty list when nothing matches.
     */
    fun lookup(sequence: String): List<Candidate>
}
