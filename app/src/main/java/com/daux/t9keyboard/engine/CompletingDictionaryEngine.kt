package com.daux.t9keyboard.engine

/**
 * Word completion: after the words the keys spell exactly, offer the **longer** words
 * they are the beginning of.
 *
 * This is where a T9 keyboard earns its keep on a long word. Ten keys for
 * `contemporaneamente` — and before this, those ten keys matched *nothing at all*,
 * because the index answers only sequences of the same length: no Italian word of
 * exactly ten letters happens to spell them.
 *
 * **An offer, never an assumption.** The typed keys are a prefix of a completion, not
 * a description of it, so a completion is marked ([Candidate.completion]), sits behind
 * every exact match, and is only ever inserted by being tapped — the preview in the
 * field still follows an exact match (`T9ImeService.previewWord`). Committing an
 * eighteen-letter word off ten keypresses would be exactly the kind of guessing the
 * disambiguation column exists to prevent.
 *
 * **Where it sits:** outside [FuzzyDictionaryEngine], so completions land *between*
 * the exact matches and the typo guesses — a longer word the user may well be typing
 * is worth more than a word they might have mistyped. It asks for completions through
 * [DictionaryEngine.completions] rather than [lookup] precisely so the fuzzy engine's
 * hundred-odd sequence variants do not each drag a prefix scan behind them.
 */
class CompletingDictionaryEngine(
    private val delegate: DictionaryEngine,
    private val limit: Int = MAX_COMPLETIONS,
    private val minLength: Int = MIN_LENGTH
) : DictionaryEngine {

    override fun lookup(sequence: String): List<Candidate> {
        val found = delegate.lookup(sequence)
        // Too few keys and the prefix covers thousands of words: the bar would stop
        // being about what is being typed and become a list of the language.
        if (sequence.length < minLength) return found

        val seen = found.mapTo(HashSet()) { it.word }
        val completions = delegate.completions(sequence, limit)
            .filter { seen.add(it.word) }
        if (completions.isEmpty()) return found

        // Exact matches, then completions, then the fuzzy tail the delegate appended.
        val fuzzyStart = found.indexOfFirst { it.fuzzy }.let { if (it < 0) found.size else it }
        return found.take(fuzzyStart) + completions + found.drop(fuzzyStart)
    }

    /** Pass-through: this class adds completions to [lookup], it is not a source. */
    override fun completions(prefix: String, limit: Int): List<Candidate> =
        delegate.completions(prefix, limit)

    companion object {
        /**
         * Shortest typed sequence worth completing. Measured on the corpus: 2 digits
         * put ~3,700 words under the prefix and 3 digits ~2,000, which is noise; by 4
         * it is ~500 and the head of that list is a real guess at the word.
         */
        const val MIN_LENGTH = 4

        /** Cap on the offers, so the bar stays readable at a glance. */
        const val MAX_COMPLETIONS = 5
    }
}
