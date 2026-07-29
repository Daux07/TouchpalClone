package com.daux.t9keyboard.input

import com.daux.t9keyboard.engine.ItalianDictionaryEngine
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProperNounsTest {

    @After
    fun clear() = ProperNouns.setKnown(emptySet())

    /**
     * The flags come from the dictionary file, so the test feeds the same format the
     * corpus converter writes — including the real capitalisation verdicts.
     */
    private fun loadCorpus(vararg lines: String) {
        val engine = ItalianDictionaryEngine.build(lines.asSequence())
        ProperNouns.setKnown(engine.properNouns)
    }

    @Test
    fun `the flag in the dictionary decides`() {
        loadCorpus("roma 831 P", "rosa 184", "italia 1285 P", "marzo 453")

        assertEquals("Roma", ProperNouns.display("roma"))
        assertEquals("Italia", ProperNouns.display("italia"))
        assertEquals("rosa", ProperNouns.display("rosa"))
        assertEquals("marzo", ProperNouns.display("marzo"))
    }

    /**
     * The reason the corpus decides and not a hand-written list: each of these is also a
     * first name, and each is far more often the common word. Measured, they stay
     * lowercase; listed by hand, they would not have.
     */
    @Test
    fun `words that are also common nouns keep their case`() {
        loadCorpus("rosa 184", "viola 216", "bianca 147", "vera 278", "prato 32", "camera 250")

        for (word in listOf("rosa", "viola", "bianca", "vera", "prato", "camera")) {
            assertEquals(word, ProperNouns.display(word))
            assertFalse(word, ProperNouns.isProperNoun(word))
        }
    }

    /** Nothing is capitalised until the corpus lands — the harmless way to be wrong. */
    @Test
    fun `without a corpus nothing is a proper noun`() {
        assertEquals("roma", ProperNouns.display("roma"))
        assertFalse(ProperNouns.isProperNoun("roma"))
    }

    @Test
    fun `case does not matter when asking`() {
        loadCorpus("roma 831 P")
        assertTrue(ProperNouns.isProperNoun("ROMA"))
        assertEquals("Roma", ProperNouns.display("Roma"))
    }
}
