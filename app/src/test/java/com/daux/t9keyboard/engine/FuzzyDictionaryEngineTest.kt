package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyDictionaryEngineTest {

    // "casa" = 2272, "cara" = 2272, "cast" = 2278, "casta" = 22782, "sano" = 7266.
    private val dictionary = CorpusDictionaryEngine.build(
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

    // --- Two keys in the wrong order, and two keys wrong ----------------------

    @Test
    fun `two keys hit in the wrong order still find the word`() {
        // "casta" is 22782 typed as 22872: the 7 and the 8 came out swapped. This is
        // two edits in the deletion/insertion/substitution metric, which is exactly
        // why it used to be missed.
        assertTrue(words("22872").contains("casta"))
    }

    @Test
    fun `a swap is treated as one slip, not as a last resort`() {
        // It is found among the ordinary near misses — so it is offered even when other
        // words already match, not only when the bar would otherwise be empty. "casta"
        // is 5 digits, below DEEP_MIN_LENGTH, so the deep search cannot be what found it.
        assertTrue("22872".length < FuzzyDictionaryEngine.DEEP_MIN_LENGTH)

        val swapped = engine.lookup("22872").first { it.word == "casta" }
        assertTrue(swapped.fuzzy)
    }

    @Test
    fun `two wrong keys are found on a long enough word`() {
        val long = CorpusDictionaryEngine.build(sequenceOf("problema 900"))
        val engine = FuzzyDictionaryEngine(long)

        // "problema" = 77625362, typed with the 6 and the 5 both wrong: 77623362.
        assertTrue(engine.lookup("77623362").map { it.word }.contains("problema"))
    }

    @Test
    fun `two wrong keys are not reached for on a short word`() {
        // "casa" with two of its four keys wrong is not a typo, it is another word.
        assertTrue(engine.lookup("3372").isEmpty())
    }

    @Test
    fun `two wrong keys are a last resort, never a disturbance`() {
        val long = CorpusDictionaryEngine.build(sequenceOf("problema 900", "problemi 800"))
        val engine = FuzzyDictionaryEngine(long)

        // Typed correctly, the exact match leads and nothing displaces it. ("problemi"
        // still appears behind it: it is one wrong key away, which it always was — the
        // near miss, not the deep search, is what offers it.)
        val result = engine.lookup("77625362")
        assertEquals("problema", result.first().word)
        assertTrue(result.first().isExact)
        assertTrue(result.drop(1).all { it.fuzzy })
    }
}
