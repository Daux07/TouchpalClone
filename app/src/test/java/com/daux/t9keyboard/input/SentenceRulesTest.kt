package com.daux.t9keyboard.input

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceRulesTest {

    /** The whole point: `ecc.` looks exactly like the end of a sentence, and is not. */
    @Test
    fun `common abbreviations are not sentence endings`() {
        for (abbreviation in listOf("ecc", "dott", "prof", "ing", "sig", "pag", "cap",
            "rif", "ca", "art", "sac", "cfr", "v", "vd")) {
            assertTrue(abbreviation, SentenceRules.endsWithAbbreviation("scrivo $abbreviation."))
        }
    }

    /** `p.v.` carries its own dot inside, so the token has to keep it. */
    @Test
    fun `an abbreviation with an internal dot is recognised`() {
        assertTrue(SentenceRules.endsWithAbbreviation("il 12 p.v."))
        assertTrue(SentenceRules.endsWithAbbreviation("alla c.a."))
        // `sig.ra` ends in a letter: there is no dot there to mistake for a full stop.
        assertFalse(SentenceRules.endsWithAbbreviation("la sig.ra"))
    }

    @Test
    fun `a real sentence ending is not mistaken for one`() {
        assertFalse(SentenceRules.endsWithAbbreviation("sono andato a casa."))
        assertFalse(SentenceRules.endsWithAbbreviation("ho fatto tutto."))
        assertFalse(SentenceRules.endsWithAbbreviation("finito"))
    }

    @Test
    fun `case does not matter`() {
        assertTrue(SentenceRules.endsWithAbbreviation("il Dott."))
        assertTrue(SentenceRules.endsWithAbbreviation("ECC."))
    }

    /** `Ciao. "Come stai` — the capital belongs to the word inside the quotes. */
    @Test
    fun `an opening quote after a full stop passes the capital through`() {
        assertTrue(SentenceRules.afterOpeningAtSentenceStart("Ciao. \""))
        assertTrue(SentenceRules.afterOpeningAtSentenceStart("Ciao. «"))
        assertTrue(SentenceRules.afterOpeningAtSentenceStart("Ciao! ("))
        assertTrue(SentenceRules.afterOpeningAtSentenceStart("\"")) // start of the field
    }

    @Test
    fun `an opening quote mid-sentence does not`() {
        assertFalse(SentenceRules.afterOpeningAtSentenceStart("mi ha detto \""))
        assertFalse(SentenceRules.afterOpeningAtSentenceStart("una parola («"))
        assertFalse(SentenceRules.afterOpeningAtSentenceStart("casa"))
    }

    @Test
    fun `the ellipsis is recognised whole and in progress`() {
        assertTrue(SentenceRules.endsWithEllipsis("forse..."))
        assertTrue(SentenceRules.endsWithEllipsis("forse…"))
        assertFalse(SentenceRules.endsWithEllipsis("forse.."))

        assertTrue(SentenceRules.endsWithPartialEllipsis("forse.."))
        assertFalse(SentenceRules.endsWithPartialEllipsis("forse..."))
        assertFalse(SentenceRules.endsWithPartialEllipsis("forse."))
    }
}
