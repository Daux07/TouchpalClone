package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletingDictionaryEngineTest {

    /** A stand-in index: exact matches and completions given straight, per sequence. */
    private class FakeEngine(
        val exact: Map<String, List<Candidate>> = emptyMap(),
        val longer: Map<String, List<Candidate>> = emptyMap()
    ) : DictionaryEngine {
        override fun lookup(sequence: String) = exact[sequence].orEmpty()
        override fun completions(prefix: String, limit: Int) =
            longer[prefix].orEmpty().take(limit)
    }

    private fun word(w: String, weight: Long, fuzzy: Boolean = false) =
        Candidate(w, "0", weight, fuzzy = fuzzy)

    private fun completion(w: String, weight: Long) =
        Candidate(w, "0", weight, completion = true)

    @Test
    fun offersLongerWordsWhenNothingMatchesExactly() {
        // The case that motivated the feature: ten keys that spell no word at all.
        val engine = CompletingDictionaryEngine(
            FakeEngine(longer = mapOf("2668367672" to listOf(completion("contemporaneamente", 11))))
        )

        val result = engine.lookup("2668367672")

        assertEquals(listOf("contemporaneamente"), result.map { it.word })
        assertTrue(result.single().completion)
        assertFalse(result.single().isExact) // an offer, never what the preview commits
    }

    @Test
    fun exactMatchesComeFirst_completionsAfterThem() {
        val engine = CompletingDictionaryEngine(
            FakeEngine(
                exact = mapOf("2272" to listOf(word("casa", 100))),
                // Heavier than the exact match, and still behind it: what was actually
                // typed outranks a guess at what is being typed.
                longer = mapOf("2272" to listOf(completion("casalinga", 9_000)))
            )
        )

        assertEquals(listOf("casa", "casalinga"), engine.lookup("2272").map { it.word })
    }

    @Test
    fun completionsSitBetweenExactMatchesAndTypoGuesses() {
        val engine = CompletingDictionaryEngine(
            FakeEngine(
                exact = mapOf("2272" to listOf(word("casa", 100), word("cara", 5, fuzzy = true))),
                longer = mapOf("2272" to listOf(completion("casalinga", 4)))
            )
        )

        // A longer word the user may well be typing beats a word they might have mistyped.
        assertEquals(listOf("casa", "casalinga", "cara"), engine.lookup("2272").map { it.word })
    }

    @Test
    fun aWordAlreadyProposedIsNotRepeatedAsACompletion() {
        val engine = CompletingDictionaryEngine(
            FakeEngine(
                exact = mapOf("2272" to listOf(word("casa", 100))),
                longer = mapOf("2272" to listOf(completion("casa", 100), completion("casale", 3)))
            )
        )

        assertEquals(listOf("casa", "casale"), engine.lookup("2272").map { it.word })
    }

    @Test
    fun tooFewKeysAreNotCompleted() {
        // Three digits put ~2,000 corpus words under the prefix: that is not a
        // prediction, it is a list of the language.
        val engine = CompletingDictionaryEngine(
            FakeEngine(longer = mapOf("227" to listOf(completion("casalinga", 9))))
        )

        assertTrue(engine.lookup("227").isEmpty())
    }

    @Test
    fun theNumberOfOffersIsCapped() {
        val many = (1..20).map { completion("parola$it", it.toLong()) }
        val engine = CompletingDictionaryEngine(FakeEngine(longer = mapOf("2272" to many)), limit = 3)

        assertEquals(3, engine.lookup("2272").size)
    }

    @Test
    fun completionsArePassedThroughForWhoeverWrapsThis() {
        // It decorates lookup; it is not itself a source of completions.
        val engine = CompletingDictionaryEngine(
            FakeEngine(longer = mapOf("2272" to listOf(completion("casalinga", 9))))
        )

        assertEquals(listOf("casalinga"), engine.completions("2272", 5).map { it.word })
    }
}
