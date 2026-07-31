package com.daux.t9keyboard.engine

/**
 * More than one language typed without switching between them (plan §8).
 *
 * The keypad is the same in all of them — 2=ABC everywhere in ITU-T E.161 — so nothing
 * about the *input* is multilingual. Only the ranking is, and that is all this class
 * does: the disambiguation column stays single, as the plan foresaw.
 *
 * **A secondary language never outranks the primary**, and secondaries never outrank
 * each other out of the order they are listed in. Every Italian word the keys spell
 * exactly comes first, in its own order; English follows, in its own; a third language
 * would follow that. Deliberately **not** a merge by frequency, even though the weights
 * would allow it: [CorpusDictionaryEngine] normalises every corpus to occurrences per
 * million, so the scales really are comparable — and that is precisely the danger. `the`
 * is about 42,000 per million and would lead almost every sequence it touches, pushing
 * Italian words down the bar of a keyboard used mostly in Italian.
 *
 * The order chosen has a property worth more than reach: **no sequence that worked
 * before can rank differently now**. A secondary language can only appear where the
 * primary has stopped.
 *
 * A word two languages share is kept once, on the higher-priority side (`radio`, `bar`,
 * `film`): the same word, not two candidates.
 *
 * Learning stays a single mixed dictionary, as the plan allows: it sits *above* this
 * engine, so a confirmed word wins whatever language proposed it, and `learned_words`
 * needs no `lang` column.
 */
class LanguagePriorityEngine(
    private val primary: DictionaryEngine,
    private val secondaries: List<DictionaryEngine>
) : DictionaryEngine {

    override fun lookup(sequence: String): List<Candidate> =
        secondaries.fold(primary.lookup(sequence)) { found, language ->
            append(found, language.lookup(sequence))
        }

    /**
     * Completions follow the same order for the same reason: a foreign word the keys are
     * the beginning of is an offer, and it belongs behind the ones in the main language.
     * Each language is asked only while places are still free, so a secondary can never
     * spend one the primary would have filled.
     */
    override fun completions(prefix: String, limit: Int): List<Candidate> {
        var found = primary.completions(prefix, limit)
        for (language in secondaries) {
            if (found.size >= limit) break
            found = append(found, language.completions(prefix, limit))
        }
        return found.take(limit)
    }

    private fun append(found: List<Candidate>, more: List<Candidate>): List<Candidate> {
        if (more.isEmpty()) return found
        if (found.isEmpty()) return more
        val seen = found.mapTo(HashSet()) { it.word }
        return found + more.filter { seen.add(it.word) }
    }
}
