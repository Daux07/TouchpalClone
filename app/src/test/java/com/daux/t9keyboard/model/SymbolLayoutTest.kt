package com.daux.t9keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolLayoutTest {

    private val pages = listOf(SymbolLayout.page1, SymbolLayout.page2)

    private fun insertedText(grid: KeyGrid): List<String> =
        grid.rows.flatMap { it.keys }.mapNotNull { (it.action as? KeyAction.Insert)?.text }

    @Test
    fun `a key inserts exactly what it shows`() {
        // Guards against a label/action mismatch when editing the tables by hand.
        for (page in pages) {
            for (key in page.rows.flatMap { it.keys }) {
                val insert = key.action as? KeyAction.Insert ?: continue
                assertEquals(key.mainLabel, insert.text)
            }
        }
    }

    @Test
    fun `page 1 carries all ten digits`() {
        assertTrue(insertedText(SymbolLayout.page1).containsAll(('0'..'9').map { it.toString() }))
    }

    @Test
    fun `no symbol is repeated within or across the pages`() {
        // Apart from the comma and period, which sit on both bottom rows (and on T9).
        val bottomRow = setOf(",", ".")
        val all = pages.flatMap { insertedText(it) }.filterNot { it in bottomRow }

        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun `every page can reach the other one and get back to letters`() {
        for (page in pages) {
            val targets = page.rows.flatMap { it.keys }
                .mapNotNull { (it.action as? KeyAction.Mode)?.target }
            assertTrue(targets.contains(KeyboardMode.T9))
            assertEquals(1, targets.count { it != KeyboardMode.T9 })
        }
    }

    @Test
    fun `both pages keep backspace, space and enter reachable`() {
        for (page in pages) {
            val actions = page.rows.flatMap { it.keys }.map { it.action }
            assertTrue(actions.contains(KeyAction.Backspace))
            assertTrue(actions.contains(KeyAction.Space))
            assertTrue(actions.contains(KeyAction.Enter))
        }
    }

    @Test
    fun `the mode switch on the T9 keypad opens the first symbol page`() {
        val modeKeys = T9Layout.bottomRow.mapNotNull { (it.action as? KeyAction.Mode)?.target }

        assertEquals(listOf(KeyboardMode.SYMBOLS_1), modeKeys)
    }
}
