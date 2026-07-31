package com.daux.t9keyboard.engine

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the cost of the deep (two wrong keys) search against the **real** corpus.
 *
 * Not a benchmark to admire: a keypress that stalls is a keyboard that feels broken,
 * and this search runs on the main thread. If a change makes it expensive, this fails.
 */
class FuzzyCostTest {

    private val corpusFile = File("src/main/assets/dict/it.txt")

    @Test
    fun `the deep search stays fast enough for a keypress on the real corpus`() {
        if (!corpusFile.exists()) return // the corpus is not part of every checkout
        val engine = FuzzyDictionaryEngine(
            corpusFile.bufferedReader().useLines { ItalianDictionaryEngine.build(it) }
        )

        // Eight keys that spell nothing and are nowhere near anything: the worst case,
        // where every stage runs to exhaustion before giving up.
        val nonsense = "94949494"
        repeat(3) { engine.lookup(nonsense) } // let the JIT settle

        val start = System.nanoTime()
        repeat(10) { engine.lookup(nonsense) }
        val perLookupMs = (System.nanoTime() - start) / 10 / 1_000_000.0

        assertTrue("deep lookup took $perLookupMs ms", perLookupMs < 50.0)
        println("costo peggiore per pressione: %.1f ms".format(perLookupMs))
    }
}
