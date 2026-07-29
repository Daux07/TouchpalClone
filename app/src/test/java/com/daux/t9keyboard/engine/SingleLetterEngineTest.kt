package com.daux.t9keyboard.engine

import com.daux.t9keyboard.model.T9Keypad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SingleLetterEngineTest {

    /** The real corpus figures for key 3, which are what made the order look odd. */
    private val key3 = fakeEngine(
        "e" to 48146L, "è" to 27734L, "é" to 82L, "d" to 37L, "f" to 9L
    )

    private fun fakeEngine(vararg entries: Pair<String, Long>) = object : DictionaryEngine {
        override fun lookup(sequence: String): List<Candidate> =
            entries.sortedByDescending { it.second }
                .map { (word, weight) -> Candidate(word, sequence, weight) }
    }

    private fun words(engine: DictionaryEngine, sequence: String) =
        SingleLetterEngine(engine).lookup(sequence).map { it.word }

    /** The complaint: a lone keypress must give the plain letter, not the accented one. */
    @Test
    fun `the plain letter comes before its accent`() {
        val ordered = words(key3, "3")
        assertEquals("e", ordered.first())
        assertTrue(ordered.indexOf("e") < ordered.indexOf("è"))
    }

    /** The accent is still right there — second, as requested — not hidden away. */
    @Test
    fun `the accent is the next candidate`() {
        assertEquals("è", words(key3, "3")[1])
    }

    /** Corpus residue (`d`, `f` as initials) must not outrank the key's own letters. */
    @Test
    fun `letters of the key come before rare accents`() {
        val ordered = words(key3, "3")
        assertEquals(listOf("e", "è", "d", "f", "é"), ordered)
    }

    /** `q` never appears alone in the corpus, and used to vanish from key 7 entirely. */
    @Test
    fun `every letter of the key is offered`() {
        for (digit in 2..9) {
            val offered = words(fakeEngine(), digit.toString())
            assertEquals("key $digit", T9Keypad.columnLetters(digit), offered.map { it.single() })
        }
    }

    /** With no word among them, the key reads exactly as it is printed. */
    @Test
    fun `a key with no one-letter words keeps keypad order`() {
        val key7 = fakeEngine("s" to 27L, "r" to 14L, "p" to 9L)
        assertEquals(listOf("p", "q", "r", "s"), words(key7, "7"))
    }

    @Test
    fun `longer sequences are left alone`() {
        val engine = fakeEngine("casa" to 100L, "cara" to 50L)
        assertEquals(listOf("casa", "cara"), words(engine, "2272"))
    }

    /**
     * A learned word outranks the corpus, but it must not be able to put an accent in
     * front of its plain letter — the very thing that made the keyboard write "è".
     */
    @Test
    fun `a learned accent still does not overtake the plain letter`() {
        val learned = fakeEngine("è" to 1_000_000L, "e" to 48146L, "d" to 37L, "f" to 9L)
        assertEquals("e", words(learned, "3").first())
    }
}
