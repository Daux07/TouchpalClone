package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagePriorityEngineTest {

    // "casa", "cara" e l'inglese "barb" stanno tutti sulla sequenza 2272.
    private val italian = CorpusDictionaryEngine.build(
        sequenceOf("casa 900", "cara 500", "bar 300", "film 250")
    )

    // "the" carries the weight that makes a frequency merge the wrong idea: on its own
    // scale it dwarfs almost anything Italian.
    private val english = CorpusDictionaryEngine.build(
        sequenceOf("back 800", "barb 100", "the 41924", "bar 200", "film 400")
    )

    private val engine = LanguagePriorityEngine(primary = italian, secondaries = listOf(english))

    @Test
    fun everyItalianWordComesBeforeEveryEnglishOne() {
        assertEquals(listOf("casa", "cara", "barb"), engine.lookup("2272").map { it.word })
    }

    @Test
    fun aHeavyEnglishWordStillDoesNotOutrankItalian() {
        // "the" = 843, "vie" = 843 too. Weighted purely by frequency the English word
        // would lead; the whole point of the chosen order is that it cannot.
        val withVie = LanguagePriorityEngine(
            primary = CorpusDictionaryEngine.build(sequenceOf("vie 190")),
            secondaries = listOf(english)
        )

        assertEquals(listOf("vie", "the"), withVie.lookup("843").map { it.word })
    }

    @Test
    fun aWordBothLanguagesKnowIsKeptOnce_onTheItalianSide() {
        val bar = engine.lookup("227").filter { it.word == "bar" }

        assertEquals(1, bar.size)
        assertEquals(300L, bar.single().weight) // the Italian entry, not the English one
    }

    @Test
    fun englishAloneStillAnswersWhenItalianHasNothing() {
        // 843 is only English here: the secondary is not a tail, it is a dictionary.
        assertEquals(listOf("the"), engine.lookup("843").map { it.word })
    }

    @Test
    fun aThirdLanguageFallsInBehindTheSecond() {
        // The claim the design makes: a further language is a list entry, and it ranks
        // after the ones before it, never among them.
        val threeLanguages = LanguagePriorityEngine(
            primary = CorpusDictionaryEngine.build(sequenceOf("bar 90")),
            secondaries = listOf(
                CorpusDictionaryEngine.build(sequenceOf("car 500")),  // "inglese"
                CorpusDictionaryEngine.build(sequenceOf("cap 700"))   // una terza lingua
            )
        )

        // Tutte e tre sono 227; l'italiana è la più leggera e guida lo stesso.
        assertEquals(listOf("bar", "car", "cap"), threeLanguages.lookup("227").map { it.word })
    }

    @Test
    fun withNoSecondaryAtAllOnlyThePrimaryAnswers() {
        // What switching every language off leaves: the v1 keyboard, unchanged.
        val alone = LanguagePriorityEngine(primary = italian, secondaries = emptyList())

        assertEquals(italian.lookup("2272"), alone.lookup("2272"))
    }

    @Test
    fun nothingAnywhereGivesNothing() {
        assertTrue(engine.lookup("9999").isEmpty())
    }

    @Test
    fun completionsKeepTheSameOrder() {
        val completions = engine.completions("227", 5).map { it.word }

        // Italian completions of 227 first, then the English one.
        assertEquals(listOf("casa", "cara", "barb"), completions)
    }

    @Test
    fun completionsDoNotSpendTheirBudgetOnTheSecondLanguage() {
        // With room for two, both go to Italian: English never displaces it.
        assertEquals(listOf("casa", "cara"), engine.completions("227", 2).map { it.word })
    }
}
