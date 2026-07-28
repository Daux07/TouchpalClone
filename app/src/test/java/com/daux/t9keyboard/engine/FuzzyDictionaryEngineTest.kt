package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyDictionaryEngineTest {

    // "casa" = 2272, "cara" = 2272, "cast" = 2278, "casta" = 22782, "sano" = 7266.
    private val dictionary = ItalianDictionaryEngine.build(
        sequenceOf("casa 900", "cara 500", "casta 300", "sano 200")
    )
    private val engine = FuzzyDictionaryEngine(dictionary)

    private fun words(sequence: String) = engine.lookup(sequence).map { it.word }

    @Test
    fun `exact matches come first and are not marked fuzzy`() {
        val result = engine.lookup("2272")

        assertEquals(listOf("casa", "cara"), result.take(2).map { it.word })
        assertFalse(result[0].fuzzy)
        assertFalse(result[1].fuzzy)
    }

    @Test
    fun `one key too many still finds the word`() {
        // "casa" typed as 2-2-7-3-2: the stray 3 has to be dropped.
        assertTrue(words("22732").contains("casa"))
    }

    @Test
    fun `a missing key still finds the word`() {
        // "casa" typed as 2-2-7: the final 2 was never pressed.
        assertTrue(words("227").contains("casa"))
    }

    @Test
    fun `the wrong key still finds the word`() {
        // "casa" typed as 2-2-7-3: 3 hit instead of the neighbouring 2.
        assertTrue(words("2273").contains("casa"))
    }

    @Test
    fun `fuzzy candidates are marked and pushed below the exact ones`() {
        val result = engine.lookup("22782") // exact: casta

        val exact = result.filter { !it.fuzzy }
        val fuzzy = result.filter { it.fuzzy }
        assertEquals(listOf("casta"), exact.map { it.word })
        assertTrue(fuzzy.isNotEmpty())
        // Order: every exact match precedes every fuzzy one.
        assertEquals(exact.size, result.indexOfFirst { it.fuzzy })
        assertTrue(fuzzy.all { it.weight < exact.first().weight })
    }

    @Test
    fun `no duplicates between exact and fuzzy candidates`() {
        val words = words("2272")

        assertEquals(words.size, words.toSet().size)
    }

    @Test
    fun `short sequences are left alone`() {
        // Two digits: everything is one edit from everything, so no guessing.
        assertEquals(dictionary.lookup("22"), engine.lookup("22"))
    }

    @Test
    fun `the tail is capped`() {
        val fuzzy = engine.lookup("2272").count { it.fuzzy }

        assertTrue(fuzzy <= FuzzyDictionaryEngine.MAX_FUZZY)
    }

    @Test
    fun `a sequence with no match anywhere gives nothing`() {
        assertEquals(emptyList<Candidate>(), engine.lookup("55555"))
    }
}
