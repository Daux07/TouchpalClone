package com.daux.t9keyboard.engine

/**
 * Two languages typed without switching between them (plan §8).
 *
 * The keypad is the same in both — 2=ABC everywhere in ITU-T E.161 — so nothing about
 * the *input* is bilingual. Only the ranking is, and that is all this class does.
 *
 * **The secondary language never outranks the primary.** Every Italian word that the
 * keys spell exactly comes first, in its own order; English follows behind, in its own
 * order. Deliberately **not** a merge by frequency, even though the weights would allow
 * it: [CorpusDictionaryEngine] normalises both corpora to occurrences per million, so
 * the two scales really are comparable — and that is precisely the danger. `the` is
 * about 42,000 per million and would lead almost every sequence it touches, pushing
 * Italian words down the bar of a keyboard used mostly in Italian. The user chose the
 * safe order, and it has a property worth keeping: **no sequence that worked before can
 * rank differently now**. English can only ever appear where Italian has stopped.
 *
 * A word both languages know is kept once, on the primary side (`radio`, `bar`,
 * `film`): the same word, not two candidates.
 *
 * Learning stays a single mixed dictionary, as the plan allows: it sits *above* this
 * engine, so a confirmed word wins whatever language it came from, and `learned_words`
 * needs no `lang` column.
 */
class BilingualDictionaryEngine(
    private val primary: DictionaryEngine,
    private val secondary: DictionaryEngine
) : DictionaryEngine {

    override fun lookup(sequence: String): List<Candidate> =
        concat(primary.lookup(sequence), secondary.lookup(sequence))

    /**
     * Completions follow the same order for the same reason: an English word the keys
     * are the beginning of is an offer, and it belongs behind the Italian ones.
     */
    override fun completions(prefix: String, limit: Int): List<Candidate> {
        val first = primary.completions(prefix, limit)
        if (first.size >= limit) return first
        return concat(first, secondary.completions(prefix, limit)).take(limit)
    }

    private fun concat(first: List<Candidate>, second: List<Candidate>): List<Candidate> {
        if (second.isEmpty()) return first
        if (first.isEmpty()) return second
        val seen = first.mapTo(HashSet()) { it.word }
        return first + second.filter { seen.add(it.word) }
    }
}
