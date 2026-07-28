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

        val fuzzy = ArrayList<Candidate>()
        for (variant in variantsOf(sequence)) {
            for (candidate in delegate.lookup(variant)) {
                if (seen.add(candidate.word)) {
                    fuzzy += candidate.copy(weight = candidate.weight / PENALTY, fuzzy = true)
                }
            }
        }
        if (fuzzy.isEmpty()) return exact

        fuzzy.sortByDescending { it.weight }
        return exact + fuzzy.take(maxCandidates)
    }

    /** Every sequence at edit distance 1 from [sequence]. */
    private fun variantsOf(sequence: String): List<String> {
        val out = ArrayList<String>(
            sequence.length + sequence.length * (DIGITS.length - 1) +
                (sequence.length + 1) * DIGITS.length
        )
        // One key too many: drop each position in turn.
        for (i in sequence.indices) {
            out += sequence.removeRange(i, i + 1)
        }
        // Wrong key: replace each position with every other digit.
        for (i in sequence.indices) {
            for (digit in DIGITS) {
                if (digit != sequence[i]) {
                    out += sequence.replaceRange(i, i + 1, digit.toString())
                }
            }
        }
        // Missing key: insert every digit at every gap, ends included.
        for (i in 0..sequence.length) {
            for (digit in DIGITS) {
                out += sequence.substring(0, i) + digit + sequence.substring(i)
            }
        }
        return out
    }

    companion object {
        /** The keys that can carry letters — 0 is space and 1 is punctuation. */
        private const val DIGITS = "23456789"

        /** Shortest typed sequence worth correcting. */
        const val MIN_LENGTH = 3

        /** How far a fuzzy candidate is pushed down the weight scale. */
        const val PENALTY = 1_000L

        /** Cap on the tail, so the bar stays about the typed word. */
        const val MAX_FUZZY = 6
    }
}
