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

    /**
     * Completions from every source, merged the same way. Each source is asked for the
     * full [limit] before merging: a word offered by two sources must not eat two of
     * the places, and a source that has nothing must not shrink the answer.
     */
    override fun completions(prefix: String, limit: Int): List<Candidate> {
        val best = LinkedHashMap<String, Candidate>()
        for (source in sources) {
            for (candidate in source.completions(prefix, limit)) {
                val current = best[candidate.word]
                if (current == null || candidate.weight > current.weight) {
                    best[candidate.word] = candidate
                }
            }
        }
        return best.values.sortedByDescending { it.weight }.take(limit)
    }
}
