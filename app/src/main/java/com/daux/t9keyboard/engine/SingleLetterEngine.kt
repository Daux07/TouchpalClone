package com.daux.t9keyboard.engine

import com.daux.t9keyboard.model.T9Keypad

/**
 * Makes a **single keypress** behave like a keypress instead of a word lookup.
 *
 * Ranking one-letter results by corpus frequency gives a list that looks arbitrary,
 * because at one letter frequency stops measuring what we want: accented forms land
 * ahead of the key's own plain letters (`e è é d f`), and a letter that never appears
 * alone — `q` — is missing from the key altogether.
 *
 * So for one digit the list is rebuilt from the keypad: **every letter of the key is
 * there**, the ones that are real words come first, and an accent never precedes the
 * plain letter it belongs to. Pressing `3` offers `e`, then `è`, then `d f`, and pressing
 * `7` offers `p q r s` in the order written on the key — nothing missing, nothing
 * surprising.
 *
 * Longer sequences are left exactly as they are: there, frequency is the right answer.
 */
class SingleLetterEngine(private val delegate: DictionaryEngine) : DictionaryEngine {

    /** Pass-through: a single letter's ordering says nothing about longer words. */
    override fun completions(prefix: String, limit: Int): List<Candidate> =
        delegate.completions(prefix, limit)

    override fun lookup(sequence: String): List<Candidate> {
        val candidates = delegate.lookup(sequence)
        if (sequence.length != 1) return candidates

        val digit = sequence.single().digitToIntOrNull() ?: return candidates
        val letters = T9Keypad.columnLetters(digit)
        if (letters.isEmpty()) return candidates

        val weights = candidates.associate { it.word to it.weight }
        val accented = letters.toSet() - T9Keypad.letters[digit].orEmpty().toSet()

        // Words first (an accent still after its plain letter), then the rest of the key
        // in the order printed on it, so the list always reads like the keypad.
        fun weightOf(letter: Char) = weights[letter.toString()] ?: 0L

        val (words, others) = letters.partition { weightOf(it) >= REAL_WORD_WEIGHT }
        val ordered = words.sortedWith(
            compareBy({ it in accented }, { -weightOf(it) })
        ) + others

        return ordered.map { letter ->
            Candidate(letter.toString(), sequence, weightOf(letter))
        }
    }

    private companion object {
        /**
         * Above this a one-letter entry is a word people actually write (`e`, `a`, `è`);
         * below it, corpus residue — initials and abbreviations — that has no business
         * being offered before the key's own letters.
         */
        const val REAL_WORD_WEIGHT = 1_000L
    }
}
