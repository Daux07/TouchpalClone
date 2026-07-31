package com.daux.t9keyboard.engine

/**
 * Typo tolerance (Phase 1.7): wraps any [DictionaryEngine] and, after its exact
 * matches, offers words whose sequence is **one keypress away** from the typed one:
 *
 * - a **deletion** — one key pressed too many in the middle of the word;
 * - an **insertion** — one key missed;
 * - a **substitution** — the wrong key hit (a neighbour on the keypad).
 *
 * Method: instead of scanning the dictionary, the *sequence* variants are generated
 * (a few dozen strings) and looked up in the existing index, so the cost stays a
 * handful of O(1) hash lookups per keypress — no fuzzy matching over 50k words.
 *
 * Fuzzy candidates are marked ([Candidate.fuzzy]), weighted down by [PENALTY] and
 * appended **after** every exact match, so normal typing is never disturbed: they
 * only reorder the tail of the suggestion bar, and the word committed on space
 * still comes from an exact match (see `T9ImeService.currentPreview`).
 *
 * Sits outside [MergingDictionaryEngine], so it tolerates typos on learned words
 * and — from Phase 2 — on both languages at once.
 */
class FuzzyDictionaryEngine(
    private val delegate: DictionaryEngine,
    private val maxCandidates: Int = MAX_FUZZY
) : DictionaryEngine {

    override fun lookup(sequence: String): List<Candidate> {
        val exact = delegate.lookup(sequence)
        // Below a few digits everything is one edit from everything: pure noise.
        if (sequence.length < MIN_LENGTH) return exact

        val seen = HashSet<String>()
        exact.forEach { seen.add(it.word) }

        val near = collect(variantsOf(sequence), seen, PENALTY)
        if (near.isNotEmpty()) return exact + near.take(maxCandidates)
        if (exact.isNotEmpty()) return exact

        // Nothing at all — not the typed keys, not one keypress away. Only now is it
        // worth reaching for two wrong keys, and only on a word long enough that two
        // errors still leave enough right to identify it. The cost is paid exactly
        // when there is nothing to show anyway, never while normal typing works.
        // Same penalty as a near miss, and not a heavier one: these results are only
        // ever reached when the near list is empty, so the two never share a list and
        // a second penalty would express a distinction nothing can observe.
        if (sequence.length < DEEP_MIN_LENGTH) return exact
        return collect(twoWrongKeys(sequence), seen, PENALTY).take(maxCandidates)
    }

    /**
     * Look each variant up, keeping the first sighting of every word, weighted down by
     * [penalty] and marked as a guess. Sorted best-first.
     */
    private fun collect(
        variants: Sequence<String>,
        seen: MutableSet<String>,
        penalty: Long
    ): List<Candidate> {
        val found = ArrayList<Candidate>()
        for (variant in variants) {
            for (candidate in delegate.lookup(variant)) {
                if (seen.add(candidate.word)) {
                    found += candidate.copy(weight = candidate.weight / penalty, fuzzy = true)
                }
            }
        }
        found.sortByDescending { it.weight }
        return found
    }

    /**
     * Pass-through to the real index. Typo tolerance has nothing to add to a prefix —
     * a misspelled prefix of an unfinished word is a guess on top of a guess.
     */
    override fun completions(prefix: String, limit: Int): List<Candidate> =
        delegate.completions(prefix, limit)

    /** Every sequence one slip away from [typed]. */
    private fun variantsOf(typed: String): Sequence<String> = sequence {
        // One key too many: drop each position in turn.
        for (i in typed.indices) {
            yield(typed.removeRange(i, i + 1))
        }
        // Wrong key: replace each position with every other digit.
        for (i in typed.indices) {
            for (digit in DIGITS) {
                if (digit != typed[i]) yield(typed.replaceRange(i, i + 1, digit.toString()))
            }
        }
        // Missing key: insert every digit at every gap, ends included.
        for (i in 0..typed.length) {
            for (digit in DIGITS) {
                yield(typed.substring(0, i) + digit + typed.substring(i))
            }
        }
        // Two adjacent keys hit in the wrong order — the commonest slip of all, and
        // the one this engine used to miss entirely: a swap is *two* edits in the
        // deletion/insertion/substitution metric, so it fell outside distance one.
        for (i in 0 until typed.length - 1) {
            if (typed[i] == typed[i + 1]) continue // swapping equal digits changes nothing
            val swapped = StringBuilder(typed)
            swapped[i] = typed[i + 1]
            swapped[i + 1] = typed[i]
            yield(swapped.toString())
        }
    }

    /**
     * Sequences with **two wrong keys** — the same digit count, two positions hit
     * wrong. Deliberately *not* the whole of edit distance 2: that would be every
     * variant of every variant, tens of thousands of strings built on a keypress. Two
     * wrong keys is the double slip that actually happens on a long word, and it costs
     * a couple of thousand lookups.
     */
    private fun twoWrongKeys(typed: String): Sequence<String> = sequence {
        for (i in typed.indices) {
            for (j in i + 1 until typed.length) {
                for (first in DIGITS) {
                    if (first == typed[i]) continue
                    for (second in DIGITS) {
                        if (second == typed[j]) continue
                        val out = StringBuilder(typed)
                        out[i] = first
                        out[j] = second
                        yield(out.toString())
                    }
                }
            }
        }
    }

    companion object {
        /** The keys that can carry letters — 0 is space and 1 is punctuation. */
        private const val DIGITS = "23456789"

        /** Shortest typed sequence worth correcting. */
        const val MIN_LENGTH = 3

        /**
         * Shortest sequence worth reaching for **two** wrong keys. On a short word two
         * errors leave too little that is right: half of `casa` misspelled is not a
         * typo, it is a different word.
         */
        const val DEEP_MIN_LENGTH = 6

        /** How far a fuzzy candidate is pushed down the weight scale. */
        const val PENALTY = 1_000L

        /** Cap on the tail, so the bar stays about the typed word. */
        const val MAX_FUZZY = 6
    }
}
