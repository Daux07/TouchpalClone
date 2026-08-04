package com.daux.t9keyboard.engine

import com.daux.t9keyboard.model.T9Keypad

/**
 * The personal dictionary as a [DictionaryEngine] (plan §6).
 *
 * Learned words live in RAM in the same shape as the corpus index, so typing never
 * touches the database; persistence is a write-behind mirror handled by [store],
 * whose calls happen off the main thread (see [Store]).
 *
 * **A learned word competes with the corpus, it does not replace it** ([weightFor]).
 * Until version 2.3 every learned word weighed 1,000,000 — thirty-four times the most
 * frequent word in Italian — so a word confirmed once by accident sat ahead of `casa`
 * for the rest of the dictionary's life. The weight now grows with use and fades with
 * time, on the corpus's own scale.
 *
 * Pure Kotlin (no Android): unit-testable with an in-memory [Store] and a fake clock.
 */
class LearnedWordsEngine(
    private val store: Store,
    /** Injected so the decay can be tested without waiting for real days to pass. */
    private val clock: () -> Long = System::currentTimeMillis
) : DictionaryEngine {

    /** What is known about a word: how often, and when last. */
    private data class Use(val count: Long, val lastUsed: Long)

    /** Sequence → (word → use). Guarded by the instance lock. */
    private val index = HashMap<String, HashMap<String, Use>>()

    /** Persistence seam; implemented on top of Room by the app, faked in tests. */
    interface Store {
        fun loadAll(): List<Entry>
        fun save(word: String, sequence: String, uses: Long, lastUsed: Long)
        fun delete(word: String)
    }

    data class Entry(
        val word: String,
        val sequence: String,
        val uses: Long,
        val lastUsed: Long = 0L
    )

    /**
     * Reads the whole personal dictionary into RAM. Call off the main thread.
     *
     * Single letters stored by an older build are **thrown away here, and deleted**.
     * The rule that they are never learned ([isLearnable]) arrived in Phase 1.14 and only
     * stops new ones: an `a` or a `b` written once before that is still on file, and a
     * learned word outranked the entire corpus — so key 2 proposed `b` ahead of `a`, one
     * of the commonest words in the language, and went on doing it forever. A rule that
     * only applies to the future leaves the damage in place.
     */
    fun load() {
        val entries = store.loadAll()
        val stale = entries.filterNot { isLearnable(it.word) }
        synchronized(this) {
            for (entry in entries) {
                if (!isLearnable(entry.word)) continue
                index.getOrPut(entry.sequence) { HashMap() }[entry.word] =
                    Use(entry.uses, entry.lastUsed)
            }
        }
        for (entry in stale) store.delete(entry.word)
    }

    override fun lookup(sequence: String): List<Candidate> {
        val now = clock()
        val words = synchronized(this) { index[sequence]?.toMap() } ?: return emptyList()
        return words.map { (word, use) -> Candidate(word, sequence, weightOf(use, now)) }
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
        val now = clock()
        val found = ArrayList<Candidate>()
        synchronized(this) {
            for ((sequence, words) in index) {
                if (sequence.length == prefix.length || !sequence.startsWith(prefix)) continue
                for ((word, use) in words) {
                    found += Candidate(word, sequence, weightOf(use, now), completion = true)
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
        if (!isLearnable(normalized)) return false
        val sequence = T9Keypad.sequenceFor(normalized) ?: return false

        val uses = synchronized(this) {
            val bySequence = index.getOrPut(sequence) { HashMap() }
            val next = (bySequence[normalized]?.count ?: 0L) + 1L
            bySequence[normalized] = Use(next, now)
            next
        }
        store.save(normalized, sequence, uses, now)
        return true
    }

    /**
     * Forget [word] entirely — from RAM and from the store.
     *
     * The counterpart of learning, and the reason the recency boost is worth having: a
     * word confirmed by mistake comes back to the top of its sequence for a while, which
     * is when it can be seen and thrown out. Without this, surfacing the mistake would
     * only be a way of showing the user something they could not fix.
     *
     * Returns false when the word was not in the dictionary.
     */
    fun forget(word: String): Boolean {
        val normalized = word.trim().lowercase()
        val removed = synchronized(this) {
            val sequence = T9Keypad.sequenceFor(normalized)
            val bySequence = if (sequence == null) null else index[sequence]
            val gone = bySequence?.remove(normalized) != null
            if (gone && bySequence!!.isEmpty()) index.remove(sequence)
            gone
        }
        if (removed) store.delete(normalized)
        return removed
    }

    private fun weightOf(use: Use, now: Long): Long = weightFor(use.count, use.lastUsed, now)

    companion object {
        /**
         * What may enter the personal dictionary at all.
         *
         * **Never a single letter** — kept in Phase 2.3, but for a different reason than
         * the one it was written for. The old reason was that a learned word outweighed
         * the entire corpus (`BASE_WEIGHT` was 1.000.000), so a `b` written once demoted
         * `a` **forever**. That is gone: a letter learned once would now weigh 200
         * against the 15.038 of `a`.
         *
         * The reason it stays is **predictability**. A key pressed on its own should read
         * the same way every time, and [RECENT_WEIGHT] would break exactly that: 200 plus
         * 50.000 puts a just-written `b` ahead of `a` for an hour, then lets it fall back
         * — the Phase 2.2 symptom again, briefly and repeatedly instead of permanently.
         * What a lone key offers is decided by `SingleLetterEngine` from the keypad, not
         * by history, and that is the whole point of that class.
         *
         * Note what this rule does *not* carry: an accent never precedes its plain letter
         * because `SingleLetterEngine` orders on that flag before it ever looks at a
         * weight. That defence would survive this rule being lifted.
         *
         * The rule lives here, with the data, rather than in the caller that happened to
         * need it first: that is exactly how single letters got in before Phase 1.14, and
         * why they had to be swept out again in [load].
         */
        fun isLearnable(word: String): Boolean = word.length >= 2

        // --- The weight of a personal word ---------------------------------------
        //
        // Everything below is in the corpus's own unit — occurrences per million — so
        // the numbers can be read against the real distribution of Italian:
        //
        //   most frequent word 29.311 · 100th 1.268 · 500th 208 · 1.000th 96 · median 2

        /** A word confirmed once: a real word, around the 500th most frequent. */
        const val BASE_WEIGHT = 200L

        /** Each further confirmation. Ten uses reach the top forty of the language. */
        const val USE_WEIGHT = 300L

        /** Habit has a ceiling: above the corpus, but not by an order of magnitude. */
        const val MAX_HABIT_WEIGHT = 30_000L

        /**
         * What a word gets for having *just* been used, on top of its habit weight.
         *
         * Above the whole corpus on purpose, and for a short while only. Two things fall
         * out of it: a word repeated inside one conversation stays at hand, and a word
         * learned **by mistake** comes back where it can be seen — and forgotten
         * ([forget]) — instead of sinking into the archive unnoticed and staying there.
         */
        const val RECENT_WEIGHT = 50_000L

        private const val HOUR = 60L * 60 * 1000
        private const val DAY = 24 * HOUR
        private const val WEEK = 7 * DAY

        /**
         * Habit plus recency.
         *
         * The decay is in steps rather than a curve: three thresholds that can be stated
         * in words — *just now*, *today*, *this week* — are easier to reason about, to
         * test, and to explain than a half-life nobody can picture.
         */
        fun weightFor(uses: Long, lastUsed: Long, now: Long): Long {
            val habit = (BASE_WEIGHT + (uses - 1).coerceAtLeast(0) * USE_WEIGHT)
                .coerceAtMost(MAX_HABIT_WEIGHT)
            val age = now - lastUsed
            val recency = when {
                lastUsed <= 0L || age < 0 -> 0L // unknown or a clock that went backwards
                age <= HOUR -> RECENT_WEIGHT
                age <= DAY -> RECENT_WEIGHT / 10
                age <= WEEK -> RECENT_WEIGHT / 100
                else -> 0L
            }
            return habit + recency
        }
    }
}
