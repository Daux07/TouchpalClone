package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItalianDictionaryEngineTest {

    private val engine = ItalianDictionaryEngine.build(
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
}
