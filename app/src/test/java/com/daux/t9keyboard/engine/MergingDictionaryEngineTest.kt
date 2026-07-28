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

    @Test
    fun `no match anywhere gives an empty list`() {
        val merged = MergingDictionaryEngine(listOf(engineOf(Candidate("casa", "2272", 900L))))

        assertEquals(emptyList<Candidate>(), merged.lookup("999"))
    }
}
