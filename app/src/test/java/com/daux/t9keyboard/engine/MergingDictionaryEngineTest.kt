package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class MergingDictionaryEngineTest {

    private fun engineOf(vararg candidates: Candidate) = object : DictionaryEngine {
        override fun lookup(sequence: String): List<Candidate> =
            candidates.filter { it.sequence == sequence }
    }

    @Test
    fun `candidates from all sources are merged by weight`() {
        val merged = MergingDictionaryEngine(
            listOf(
                engineOf(Candidate("bau", "228", 1_001_000L)),
                engineOf(Candidate("casa", "2272", 900L), Candidate("cara", "2272", 500L))
            )
        )

        assertEquals(listOf("casa", "cara"), merged.lookup("2272").map { it.word })
        assertEquals(listOf("bau"), merged.lookup("228").map { it.word })
    }

    @Test
    fun `a learned word outranks the corpus for the same sequence`() {
        val merged = MergingDictionaryEngine(
            listOf(
                engineOf(Candidate("cara", "2272", 1_001_000L)), // learned
                engineOf(Candidate("casa", "2272", 900L), Candidate("cara", "2272", 500L))
            )
        )

        assertEquals(listOf("cara", "casa"), merged.lookup("2272").map { it.word })
    }

    @Test
    fun `a word present in two sources appears once with its best weight`() {
        val merged = MergingDictionaryEngine(
            listOf(
                engineOf(Candidate("casa", "2272", 1_001_000L)),
                engineOf(Candidate("casa", "2272", 900L))
            )
        )

        val result = merged.lookup("2272")
        assertEquals(1, result.size)
        assertEquals(1_001_000L, result.first().weight)
    }

    /**
     * The question this answers: the corpus weights are fixed, so does a word I write
     * often but that the corpus considers rare stay at the tail for ever?
     *
     * No — because writing it *is* learning it, and the personal weight grows with use
     * while the corpus one does not. The merge keeps the higher of the two, so the word
     * climbs past the corpus on its own. This test pins **how long that takes**, which
     * is the part that actually matters to someone typing.
     */
    @Test
    fun `a rare word written often overtakes a common one, and here is when`() {
        // "bara" is rare in the corpus (50) and shares 2272 with "casa" (900).
        fun corpus() = engineOf(
            Candidate("casa", "2272", 900L),
            Candidate("cara", "2272", 500L),
            Candidate("bara", "2272", 50L)
        )
        // Habit alone: lastUsed = 0 takes the recency boost out of the picture, so this
        // measures the lasting position, not the hour after typing it.
        fun afterUses(n: Long) = MergingDictionaryEngine(
            listOf(
                engineOf(
                    Candidate("bara", "2272", LearnedWordsEngine.weightFor(n, 0L, 0L))
                ),
                corpus()
            )
        ).lookup("2272").map { it.word }

        assertEquals("una volta: dietro le più comuni", listOf("casa", "cara", "bara"), afterUses(1))
        assertEquals("tre volte: supera 'cara'", listOf("casa", "bara", "cara"), afterUses(3))
        assertEquals("quattro volte: prima", listOf("bara", "casa", "cara"), afterUses(4))
    }

    @Test
    fun `no match anywhere gives an empty list`() {
        val merged = MergingDictionaryEngine(listOf(engineOf(Candidate("casa", "2272", 900L))))

        assertEquals(emptyList<Candidate>(), merged.lookup("999"))
    }
}
