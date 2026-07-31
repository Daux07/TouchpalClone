package com.daux.t9keyboard.engine

import android.content.Context
import com.daux.t9keyboard.model.T9Keypad

/**
 * In-memory Italian dictionary: the whole word list is indexed by T9 digit
 * sequence and held in RAM, so lookups during typing do no I/O (plan §5).
 *
 * Phase 1.2 loads a small test word list from assets. Phase 1.6 replaces the
 * source with the compiled Leipzig corpus (same in-RAM shape).
 */
class ItalianDictionaryEngine private constructor(
    private val index: Map<String, List<Candidate>>,
    /**
     * Words the corpus writes with a capital nearly always — see the `P` flag in
     * `tools/ConvertLeipzig.java`. Measured rather than listed by hand, which is why
     * "roma" is here and "rosa" is not.
     */
    val properNouns: Set<String>
) : DictionaryEngine {

    /**
     * Every indexed sequence, sorted. Lets a prefix be found by binary search instead
     * of scanning 50k entries: the matches of a prefix are always a **contiguous
     * run** in sorted order, so one search finds where it starts and the scan stops
     * at the first sequence that no longer begins with it.
     */
    private val sortedSequences: List<String> = index.keys.sorted()

    override fun lookup(sequence: String): List<Candidate> =
        index[sequence] ?: emptyList()

    override fun completions(prefix: String, limit: Int): List<Candidate> {
        if (prefix.isEmpty() || limit <= 0) return emptyList()

        var i = sortedSequences.binarySearch(prefix).let { if (it < 0) -it - 1 else it }
        val found = ArrayList<Candidate>()
        while (i < sortedSequences.size) {
            val sequence = sortedSequences[i]
            if (!sequence.startsWith(prefix)) break // past the run: nothing else can match
            i++
            // The typed word itself is not a completion of itself.
            if (sequence.length == prefix.length) continue
            index[sequence]?.let { found += it }
        }
        // Sorting the whole run and taking the head is simpler than a bounded heap, and
        // the run is small by the time completions are offered (a few hundred entries).
        return found.sortedByDescending { it.weight }
            .take(limit)
            .map { it.copy(completion = true) }
    }

    companion object {

        /** Loads the word list from an assets file. */
        fun fromAssets(context: Context, path: String): ItalianDictionaryEngine =
            context.assets.open(path).bufferedReader().useLines { build(it) }

        /**
         * Builds the sequence index from a `word weight [P]` line sequence (one entry
         * per line; blank lines and `#` comments ignored). Pure — no Android deps —
         * so it is unit-testable without assets.
         */
        fun build(lines: Sequence<String>): ItalianDictionaryEngine {
            val grouped = HashMap<String, MutableList<Candidate>>()
            val properNouns = HashSet<String>()
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val parts = line.split(WHITESPACE)
                val word = parts[0]
                val weight = parts.getOrNull(1)?.toLongOrNull() ?: 1L
                val seq = T9Keypad.sequenceFor(word) ?: continue
                // A single letter is never a proper noun, whatever the corpus measured.
                // `b` and `c` come out flagged because in news prose a lone letter is an
                // initial (`B. Rossi`) or a list marker (`a) b) c)`) — never the letter
                // itself. Left in, pressing `2` offers "a B C à", and the capitals read
                // as the important options when the plain letter is the answer. Same
                // principle as `learn()` and `SingleLetterEngine`: at one character the
                // corpus stops measuring what we are asking it.
                if (word.length > 1 && parts.getOrNull(2) == PROPER_NOUN_FLAG) {
                    properNouns.add(word)
                }
                grouped.getOrPut(seq) { mutableListOf() }
                    .add(Candidate(word, seq, weight))
            }
            val index = grouped.mapValues { (_, list) ->
                list.sortedByDescending { it.weight }
            }
            return ItalianDictionaryEngine(index, properNouns)
        }

        private const val PROPER_NOUN_FLAG = "P"

        private val WHITESPACE = Regex("\\s+")
    }
}
