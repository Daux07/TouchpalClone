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

        override fun save(
            word: String,
            sequence: String,
            uses: Long,
            lastUsed: Long,
            display: String?
        ) {
            saved[word] = LearnedWordsEngine.Entry(word, sequence, uses, lastUsed, display)
        }

        override fun delete(word: String) {
            saved.remove(word)
        }
    }

    // --- Single letters: never in, and swept out if already there -----------------

    @Test
    fun `a single letter is never learned`() {
        val engine = LearnedWordsEngine(FakeStore())

        assertFalse(engine.learn("b", 1L))
        assertTrue(engine.lookup("2").isEmpty())
    }

    @Test
    fun `a single letter stored by an older build is dropped on load`() {
        // Exactly the user's phone: `b` learned before the rule existed, outranking the
        // whole corpus on key 2 ever since — and `a` is one of the commonest words there is.
        val store = FakeStore(
            listOf(
                LearnedWordsEngine.Entry("b", "2", 4L),
                LearnedWordsEngine.Entry("bau", "228", 3L)
            )
        )
        val engine = LearnedWordsEngine(store)

        engine.load()

        assertTrue("the stale letter must not be proposed", engine.lookup("2").isEmpty())
        // …and it must be gone for good, not merely ignored until the next load.
        assertFalse("the stale letter must be deleted", store.saved.containsKey("b"))
        // Everything else survives untouched.
        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
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

        // `lastUsed` is asserted too since Step 3.6: the fake used to drop it on the way
        // in, so nothing here could have caught it going astray.
        assertEquals(LearnedWordsEngine.Entry("bau", "228", 2L, 2L), store.saved["bau"])
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
    fun `a word confirmed once weighs like a real word, not like a decree`() {
        // The corpus, measured: most frequent 29.311 · 100th 1.268 · 500th 208 · median 2.
        // One confirmation should read as "a word this person writes", not as the top of
        // the language — which is what 1.000.000 used to say.
        val weight = LearnedWordsEngine.weightFor(uses = 1, lastUsed = 0L, now = 0L)

        assertTrue("must beat the long tail", weight > 100L)
        assertTrue("must not beat the commonest words", weight < 1_000L)
    }

    @Test
    fun `habit climbs above the corpus, but not by an order of magnitude`() {
        val once = LearnedWordsEngine.weightFor(1, 0L, 0L)
        val often = LearnedWordsEngine.weightFor(10, 0L, 0L)
        val always = LearnedWordsEngine.weightFor(500, 0L, 0L)

        assertTrue(often > once)
        assertTrue("ten uses reach the top of the language", often > 2_000L)
        assertEquals("and habit has a ceiling", LearnedWordsEngine.MAX_HABIT_WEIGHT, always)
    }

    @Test
    fun `a word just used comes back to the top`() {
        val now = 1_000_000_000L
        val justNow = LearnedWordsEngine.weightFor(1, now, now)

        // Above the whole corpus — deliberately, and briefly. It is what keeps a term
        // repeated in one conversation at hand, and what brings a word learned by
        // mistake back where it can be seen and forgotten.
        assertTrue(justNow > 29_311L)
    }

    @Test
    fun `the boost fades with time, leaving only the habit`() {
        val now = 1_000_000_000L
        val hour = 60L * 60 * 1000
        val fresh = LearnedWordsEngine.weightFor(1, now, now)
        val today = LearnedWordsEngine.weightFor(1, now - 2 * hour, now)
        val lastWeek = LearnedWordsEngine.weightFor(1, now - 3 * 24 * hour, now)
        val longAgo = LearnedWordsEngine.weightFor(1, now - 60 * 24 * hour, now)

        assertTrue(fresh > today)
        assertTrue(today > lastWeek)
        assertTrue(lastWeek > longAgo)
        assertEquals("what is left is the habit alone", LearnedWordsEngine.BASE_WEIGHT, longAgo)
    }

    // --- Forgetting ----------------------------------------------------------------

    @Test
    fun `a word learned by mistake can be forgotten, from RAM and from the store`() {
        val store = FakeStore()
        val engine = LearnedWordsEngine(store)
        engine.learn("bau", 1L)

        assertTrue(engine.forget("bau"))

        assertTrue("must stop being proposed", engine.lookup("228").isEmpty())
        assertFalse("and must be gone from the archive", store.saved.containsKey("bau"))
    }

    @Test
    fun `forgetting something never learned changes nothing`() {
        val engine = LearnedWordsEngine(FakeStore())
        engine.learn("bau", 1L)

        // "casa" belongs to the corpus, which is not the user's to edit.
        assertFalse(engine.forget("casa"))
        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
    }

    // --- How a word is written (Step 3.6) -----------------------------------------

    @Test
    fun `a capital in the middle is kept, because no rule could have put it there`() {
        val engine = LearnedWordsEngine(FakeStore())

        engine.learn("xD", 1L)

        assertEquals(listOf("xD"), engine.lookup("93").map { it.word })
    }

    @Test
    fun `an ordinary capital is not kept`() {
        val engine = LearnedWordsEngine(FakeStore())

        // A full stop, a fresh field or a proper noun all produce this: remembering it
        // would capitalise for ever every word that once started a sentence.
        engine.learn("Bau", 1L)

        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
    }

    @Test
    fun `shouting is not a spelling`() {
        val engine = LearnedWordsEngine(FakeStore())

        engine.learn("BAU", 1L)

        assertEquals(listOf("bau"), engine.lookup("228").map { it.word })
    }

    @Test
    fun `an ordinary capital does not erase one that was meant`() {
        val engine = LearnedWordsEngine(FakeStore())
        engine.learn("xD", 1L)

        // Same word at the start of a sentence: the automatic capital must not overwrite
        // the form the user chose by hand.
        engine.learn("Xd", 2L)

        assertEquals(listOf("xD"), engine.lookup("93").map { it.word })
    }

    @Test
    fun `the written form survives a reload`() {
        val store = FakeStore()
        LearnedWordsEngine(store).learn("iPhone", 1L)

        val reloaded = LearnedWordsEngine(store).apply { load() }

        assertEquals(listOf("iPhone"), reloaded.lookup("474663").map { it.word })
    }

    @Test
    fun `the written form does not split the word in two`() {
        val engine = LearnedWordsEngine(FakeStore())

        engine.learn("xD", 1L)
        engine.learn("xd", 2L)

        // One word, counted twice — the key stays lowercase so lookups stay case-blind.
        assertEquals(1, engine.lookup("93").size)
    }
}
