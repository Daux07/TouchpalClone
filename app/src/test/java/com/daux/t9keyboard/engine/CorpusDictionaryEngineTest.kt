package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpusDictionaryEngineTest {

    private val engine = CorpusDictionaryEngine.build(
        sequenceOf(
            "# comment line, ignored",
            "",
            "casa 900",
            "cara 400",
            "bara 90",
            "come 950"
        )
    )

    @Test
    fun lookup_returnsCandidatesOrderedByWeightDescending() {
        val words = engine.lookup("2272").map { it.word }
        assertEquals(listOf("casa", "cara", "bara"), words)
    }

    @Test
    fun lookup_returnsEmptyForUnknownSequence() {
        assertTrue(engine.lookup("99999").isEmpty())
    }

    @Test
    fun lookup_groupsOnlySameSequenceTogether() {
        // "come" is 2663, must not appear under 2272.
        assertEquals(listOf("come"), engine.lookup("2663").map { it.word })
        assertTrue(engine.lookup("2272").none { it.word == "come" })
    }

    @Test
    fun aSingleLetterIsNeverAProperNoun() {
        // The corpus really does flag `b` and `c`: in news prose a lone letter is an
        // initial or a list marker, never the letter. Left in, pressing `2` offers
        // "a B C à" and the capitals read as the important options.
        val built = CorpusDictionaryEngine.build(
            sequenceOf("b 34 P", "c 38 P", "roma 500 P", "casa 900")
        )

        assertTrue(built.properNouns.contains("roma")) // a real one still is
        assertFalse(built.properNouns.contains("b"))
        assertFalse(built.properNouns.contains("c"))
    }

    @Test
    fun completions_findLongerWordsStartingWithTheSequence() {
        val longer = CorpusDictionaryEngine.build(
            sequenceOf(
                "contemporaneamente 11",
                "contemporanea 10",
                "contemporaneo 4",
                "casa 100" // a different prefix entirely
            )
        )

        // The sequence of "contempora" — ten keys that spell no word by themselves.
        val words = longer.completions("2668367672", 5).map { it.word }

        assertEquals(listOf("contemporaneamente", "contemporanea", "contemporaneo"), words)
        assertTrue(longer.completions("2668367672", 5).all { it.completion })
    }

    @Test
    fun completions_excludeTheTypedWordItself() {
        val words = CorpusDictionaryEngine.build(
            sequenceOf("casa 100", "casale 3")
        )
        // "casa" is what was typed, not a completion of it.
        assertEquals(listOf("casale"), words.completions("2272", 5).map { it.word })
    }

    @Test
    fun completions_areEmptyWhenNothingContinuesTheSequence() {
        assertTrue(engine.completions("99999", 5).isEmpty())
    }

    @Test
    fun completions_respectTheLimit() {
        val many = CorpusDictionaryEngine.build(
            sequenceOf("casa 1", "casale 2", "casalinga 3", "casata 4", "casate 5")
        )
        assertEquals(2, many.completions("2272", 2).size)
    }
}
