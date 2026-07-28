package com.daux.t9keyboard.engine

/**
 * Merges several dictionaries behind the single [DictionaryEngine] seam.
 *
 * Used in Phase 1.5 to put the personal dictionary in front of the Italian corpus;
 * the same class serves Phase 2 (IT + EN) unchanged, since weights are on a
 * comparable scale (see [Candidate]).
 *
 * The same word may come from more than one source (a learned word that also exists
 * in the corpus): it is kept once, with its highest weight.
 */
class MergingDictionaryEngine(private val sources: List<DictionaryEngine>) : DictionaryEngine {

    override fun lookup(sequence: String): List<Candidate> {
        val best = LinkedHashMap<String, Candidate>()
        for (source in sources) {
            for (candidate in source.lookup(sequence)) {
                val current = best[candidate.word]
                if (current == null || candidate.weight > current.weight) {
                    best[candidate.word] = candidate
                }
            }
        }
        return best.values.sortedByDescending { it.weight }
    }
}
