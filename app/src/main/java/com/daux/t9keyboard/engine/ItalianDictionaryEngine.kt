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

    override fun lookup(sequence: String): List<Candidate> =
        index[sequence] ?: emptyList()

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
                if (parts.getOrNull(2) == PROPER_NOUN_FLAG) properNouns.add(word)
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
