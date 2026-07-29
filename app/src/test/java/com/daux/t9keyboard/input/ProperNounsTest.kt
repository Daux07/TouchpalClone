package com.daux.t9keyboard.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProperNounsTest {

    @Test
    fun `places and holidays are capitalised wherever they fall`() {
        assertEquals("Roma", ProperNouns.display("roma"))
        assertEquals("Italia", ProperNouns.display("italia"))
        assertEquals("Natale", ProperNouns.display("natale"))
        assertEquals("Toscana", ProperNouns.display("toscana"))
    }

    /** In Italian these stay lowercase, and getting it wrong would be visible daily. */
    @Test
    fun `months and weekdays stay lowercase`() {
        for (word in listOf(
            "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno", "luglio",
            "agosto", "settembre", "ottobre", "novembre", "dicembre",
            "lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica"
        )) {
            assertEquals(word, ProperNouns.display(word))
        }
    }

    /**
     * The reason first names are not in the list: a wrong capital on an ordinary word is
     * more annoying than a missing one on a name.
     */
    @Test
    fun `words that are also common nouns are not capitalised`() {
        for (word in listOf("rosa", "viola", "bianca", "vera", "marco", "prato", "potenza")) {
            assertEquals(word, ProperNouns.display(word))
            assertFalse(word, ProperNouns.isProperNoun(word))
        }
    }

    @Test
    fun `an already capitalised word is left as it is`() {
        assertEquals("Roma", ProperNouns.display("Roma"))
        assertTrue(ProperNouns.isProperNoun("ROMA"))
    }
}
