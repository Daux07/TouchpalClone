package com.daux.t9keyboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnedWordsEngineTest {

    /** In-memory stand-in for the Room store, so these tests need no Android. */
    private class FakeStore(
        initial: List<LearnedWordsEngine.Entry> = emptyList()
    ) : LearnedWordsEngine.Store {
        val saved = LinkedHashMap<String, LearnedWordsEngine.Entry>()
        var loads = 0

        init {
            initial.forEach { saved[it.word] = it }
        }

        override fun loadAll(): List<LearnedWordsEngine.Entry> {
            loads++
            return saved.values.toList()
        }

        override fun save(word: String, sequence: String, uses: Long, lastUsed: Long) {
            saved[word] = LearnedWordsEngine.Entry(word, sequence, uses)
        }
    }

    @Test
    fun `learned word becomes a candidate for its sequence`() {
        val engine = LearnedWordsEngine(FakeStore())

        assertTrue(engine.learn("bau", 1L))

        val candidates = engine.lookup("228")
        assertEquals(listOf("bau"), candidates.map { it.word })
        assertEquals("228", candidates.first().sequence)
    }

    @Test
    fun `learned word is persisted with its sequence and use count`() {
        val store = FakeStore()
        val engine = LearnedWordsEngine(store)

        engine.learn("bau", 1L)
        engine.learn("bau", 2L)

        assertEquals(LearnedWordsEngine.Entry("bau", "228", 2L), store.saved["bau"])
    }

    @Test
    fun `learning is case-insensitive and trims`() {
        val engine = LearnedWordsEngine(FakeStore())

        engine.learn("  Bau ", 1L)

        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
    }

    @Test
    fun `words the keypad cannot map are not learned`() {
        val store = FakeStore()
        val engine = LearnedWordsEngine(store)

        assertFalse(engine.learn("123", 1L))
        assertFalse(engine.learn("", 1L))
        assertFalse(engine.learn("   ", 1L))
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `more used words are proposed first`() {
        val engine = LearnedWordsEngine(FakeStore())

        engine.learn("bau", 1L) // 1 use
        engine.learn("cau", 1L)
        engine.learn("cau", 2L) // 2 uses, same sequence 228

        assertEquals(listOf("cau", "bau"), engine.lookup("228").map { it.word })
    }

    @Test
    fun `load restores the personal dictionary`() {
        val store = FakeStore(listOf(LearnedWordsEngine.Entry("bau", "228", 3L)))
        val engine = LearnedWordsEngine(store)

        assertEquals(emptyList<Candidate>(), engine.lookup("228"))
        engine.load()

        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
        // Confirming again continues from the stored count instead of restarting.
        engine.learn("bau", 9L)
        assertEquals(4L, store.saved["bau"]?.uses)
    }

    @Test
    fun `learned weights outrank corpus frequencies`() {
        val engine = LearnedWordsEngine(FakeStore())
        engine.learn("bau", 1L)

        // "di", the most frequent Italian word in the corpus, weighs ~75k.
        assertTrue(engine.lookup("228").first().weight > 100_000L)
    }
}
