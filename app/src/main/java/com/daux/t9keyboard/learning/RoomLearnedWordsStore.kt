package com.daux.t9keyboard.learning

import android.content.Context
import com.daux.t9keyboard.engine.LearnedWordsEngine
import java.util.concurrent.Executors

/**
 * Room-backed implementation of the personal dictionary's persistence seam.
 *
 * Writes are queued on a single background thread: confirming a word happens on the
 * key press path, which must never block on disk. Ordering is preserved because the
 * executor is single-threaded, so the last saved use count wins.
 */
class RoomLearnedWordsStore(context: Context) : LearnedWordsEngine.Store {

    private val dao = LearnedWordsDatabase.get(context).learnedWords()
    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "learned-words-writer").apply { isDaemon = true }
    }

    override fun loadAll(): List<LearnedWordsEngine.Entry> =
        dao.all().map { LearnedWordsEngine.Entry(it.word, it.sequence, it.uses, it.lastUsed) }

    override fun save(word: String, sequence: String, uses: Long, lastUsed: Long) {
        writer.execute { dao.upsert(LearnedWord(word, sequence, uses, lastUsed)) }
    }

    /** Queued like a write, for the same reason: nothing on the key path waits on disk. */
    override fun delete(word: String) {
        writer.execute { dao.delete(word) }
    }
}
