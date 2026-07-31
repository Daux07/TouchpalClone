package com.daux.t9keyboard.engine

import com.daux.t9keyboard.model.T9Keypad

/**
 * The personal dictionary as a [DictionaryEngine] (plan §6).
 *
 * Learned words live in RAM in the same shape as the corpus index, so typing never
 * touches the database; persistence is a write-behind mirror handled by [store],
 * whose calls happen off the main thread (see [Store]).
 *
 * Weights start above the highest corpus frequency ([BASE_WEIGHT]) and grow with the
 * use count, so a word the user confirmed is proposed *before* any dictionary word
 * sharing its sequence, and words used often climb above words used once.
 *
 * Pure Kotlin (no Android): unit-testable with an in-memory [Store].
 */
class LearnedWordsEngine(private val store: Store) : DictionaryEngine {

    /** Sequence → (word → use count). Guarded by the instance lock. */
    private val index = HashMap<String, HashMap<String, Long>>()

    /** Persistence seam; implemented on top of Room by the app, faked in tests. */
    interface Store {
        fun loadAll(): List<Entry>
        fun save(word: String, sequence: String, uses: Long, lastUsed: Long)
    }

    data class Entry(val word: String, val sequence: String, val uses: Long)

    /** Reads the whole personal dictionary into RAM. Call off the main thread. */
    fun load() {
        val entries = store.loadAll()
        synchronized(this) {
            for (entry in entries) {
                index.getOrPut(entry.sequence) { HashMap() }[entry.word] = entry.uses
            }
        }
    }

    override fun lookup(sequence: String): List<Candidate> {
        val words = synchronized(this) { index[sequence]?.toMap() } ?: return emptyList()
        return words.map { (word, uses) -> Candidate(word, sequence, weightFor(uses)) }
            .sortedByDescending { it.weight }
    }

    /**
     * Completions from the personal dictionary. A plain scan of the keys, with no
     * sorted index behind it: this dictionary holds the words *one person* has
     * confirmed, which is orders of magnitude smaller than the corpus — an index here
     * would be machinery to maintain on every learned word for nothing.
     */
    override fun completions(prefix: String, limit: Int): List<Candidate> {
        if (prefix.isEmpty() || limit <= 0) return emptyList()
        val found = ArrayList<Candidate>()
        synchronized(this) {
            for ((sequence, words) in index) {
                if (sequence.length == prefix.length || !sequence.startsWith(prefix)) continue
                for ((word, uses) in words) {
                    found += Candidate(word, sequence, weightFor(uses), completion = true)
                }
            }
        }
        return found.sortedByDescending { it.weight }.take(limit)
    }

    /**
     * Record that the user confirmed [word] (space, enter, punctuation, or picking a
     * suggestion). Bumps its use count and mirrors it to [store]. Words the keypad
     * cannot map back to a sequence (digits, symbols) are ignored.
     *
     * Returns false when nothing was learned.
     */
    fun learn(word: String, now: Long): Boolean {
        val normalized = word.trim().lowercase()
        if (normalized.isEmpty()) return false
        val sequence = T9Keypad.sequenceFor(normalized) ?: return false

        val uses = synchronized(this) {
            val bySequence = index.getOrPut(sequence) { HashMap() }
            val next = (bySequence[normalized] ?: 0L) + 1L
            bySequence[normalized] = next
            next
        }
        store.save(normalized, sequence, uses, now)
        return true
    }

    companion object {
        /**
         * Above the highest frequency of the Leipzig corpus (~75k for "di"), so a
         * learned word always outranks corpus words for the same sequence.
         */
        const val BASE_WEIGHT = 1_000_000L

        /** Extra weight per confirmation, to order learned words among themselves. */
        const val USE_WEIGHT = 1_000L

        fun weightFor(uses: Long): Long = BASE_WEIGHT + uses * USE_WEIGHT
    }
}
