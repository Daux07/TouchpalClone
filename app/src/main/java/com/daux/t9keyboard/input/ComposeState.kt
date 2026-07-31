package com.daux.t9keyboard.input

import com.daux.t9keyboard.model.T9Keypad

/**
 * State of the word currently being composed, driving the manual disambiguation
 * column (plan §3).
 *
 * Two parallel lists with the invariant `chosen.length <= digits.size`:
 * - [digits]: the digit keys pressed, in order (the "original sequence").
 * - [chosen]: the letters the user has forced via the column, one per digit,
 *   resolved left-to-right.
 *
 * A resolved position `i` is the pair (digits[i], chosen[i]) — the plan's "stack
 * of (digit, letter) pairs". The column always addresses the first *unresolved*
 * position ([activeColumnDigit]); picking a letter there advances by one. Because
 * every mutation keeps the two lists consistent, extending a word and correcting
 * its last letter are the same operation (plan §3.7–3.8): backspace then re-press.
 */
class ComposeState {

    private val digits = ArrayList<Int>()
    private val chosen = StringBuilder()

    /** Append a pressed digit key to the sequence. */
    fun pressDigit(digit: Int) {
        digits.add(digit)
    }

    /**
     * Force [letter] for the first unresolved position. Returns false (and changes
     * nothing) if there is no position to resolve or [letter] does not belong to
     * that position's digit.
     */
    fun chooseLetter(letter: Char): Boolean {
        val pos = chosen.length
        if (pos >= digits.size) return false
        val lower = letter.lowercaseChar()
        // Accented vowels count as letters of their key (à belongs to 2, è to 3, …).
        if (lower !in T9Keypad.columnLetters(digits[pos])) return false
        chosen.append(lower)
        return true
    }

    /**
     * Remove from the end: pops the last (digit, letter) pair if fully resolved, or
     * the last unresolved trailing digit otherwise. Returns false when there is
     * nothing to remove (so the caller can delete a character from the field).
     */
    fun backspace(): Boolean {
        if (digits.isEmpty()) return false
        digits.removeAt(digits.size - 1)
        if (chosen.length > digits.size) chosen.deleteCharAt(chosen.length - 1)
        return true
    }

    /** The digit whose letters the column should show, or null when at rest. */
    fun activeColumnDigit(): Int? {
        val pos = chosen.length
        return if (pos < digits.size) digits[pos] else null
    }

    /** The forced letters chosen so far (the word being built via the column). */
    fun forcedText(): String = chosen.toString()

    /**
     * The word as it currently reads while forcing: the letters resolved so far,
     * then the default letter of every digit still unresolved.
     *
     * Without that tail a digit pressed after forcing would be **invisible** — the
     * preview would keep showing only the resolved part, and the key would look
     * dead until its letter was picked from the column.
     */
    fun forcedPreview(): String {
        val sb = StringBuilder(chosen)
        for (i in chosen.length until digits.size) {
            sb.append(T9Keypad.letters[digits[i]]?.firstOrNull() ?: ' ')
        }
        return sb.toString()
    }

    /**
     * Take over a word already written in the field, so composing continues on it
     * instead of starting a new one (the user moved the cursor to the end of a word
     * and kept typing).
     *
     * The letters are adopted as **forced**, not re-predicted: what is written must
     * stay written. Adopting "dar" as a bare sequence would let the more frequent
     * "far" win the lookup and silently rewrite the user's text.
     *
     * Returns false, leaving the state untouched, when [word] has no digit sequence
     * (it contains a character outside the keypad).
     */
    fun adopt(word: String): Boolean {
        val sequence = T9Keypad.sequenceFor(word) ?: return false
        val letters = word.lowercase()
        // Lowercasing is per-character for the alphabets we map, but a locale that
        // grows a character would desync the two lists; refuse rather than guess.
        if (letters.length != sequence.length) return false
        reset()
        for (i in sequence.indices) {
            digits.add(sequence[i] - '0')
            if (!chooseLetter(letters[i])) {
                reset()
                return false
            }
        }
        return true
    }

    /** The full digit sequence pressed (e.g. "2272"), for dictionary lookup. */
    fun sequenceString(): String = digits.joinToString("")

    /**
     * Letters-only fallback preview when nothing in the dictionary matches: the
     * default (first) letter of each pressed digit. Used so typing always shows
     * letters, never the raw digits — the column then corrects individual letters.
     */
    fun defaultLetters(): String {
        val sb = StringBuilder(digits.size)
        for (digit in digits) {
            sb.append(T9Keypad.letters[digit]?.firstOrNull() ?: ' ')
        }
        return sb.toString()
    }

    /** True once the user has forced at least one letter via the column. */
    fun isForcing(): Boolean = chosen.isNotEmpty()

    /** True when no digit has been pressed for the current word. */
    fun isEmpty(): Boolean = digits.isEmpty()

    fun reset() {
        digits.clear()
        chosen.setLength(0)
    }
}
