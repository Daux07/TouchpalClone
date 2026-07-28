package com.daux.t9keyboard.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FavouriteSymbolsTest {

    @Test
    fun `defaults fill every slot`() {
        assertEquals(FavouriteSymbols.COUNT, FavouriteSymbols.DEFAULTS.size)
        assertEquals(FavouriteSymbols.DEFAULTS, FavouriteSymbols.normalize(emptyList()))
    }

    @Test
    fun `a short or blank stored list falls back per slot`() {
        val stored = listOf("€", "", "£")

        val result = FavouriteSymbols.normalize(stored)

        assertEquals(FavouriteSymbols.COUNT, result.size)
        assertEquals("€", result[0])
        assertEquals(FavouriteSymbols.DEFAULTS[1], result[1]) // blank → default
        assertEquals("£", result[2])
        assertEquals(FavouriteSymbols.DEFAULTS[6], result[6])
    }

    @Test
    fun `replacing a slot leaves the others alone`() {
        val result = FavouriteSymbols.replace(FavouriteSymbols.DEFAULTS, 2, "€")

        assertEquals("€", result[2])
        assertEquals(FavouriteSymbols.DEFAULTS[0], result[0])
        assertEquals(FavouriteSymbols.DEFAULTS[6], result[6])
    }

    @Test
    fun `picking a symbol the column already has swaps the two slots`() {
        // This is how reordering works: no drag needed, just replace with a
        // favourite that is already there.
        val before = FavouriteSymbols.DEFAULTS // @ ? ! / - ' "

        val after = FavouriteSymbols.replace(before, 0, before[3]) // slot 0 ← "/"

        assertEquals("/", after[0])
        assertEquals("@", after[3])
        assertEquals(before.toSet(), after.toSet()) // nothing lost, nothing duplicated
    }

    @Test
    fun `an out of range slot or a blank symbol changes nothing`() {
        assertEquals(FavouriteSymbols.DEFAULTS, FavouriteSymbols.replace(FavouriteSymbols.DEFAULTS, 9, "€"))
        assertEquals(FavouriteSymbols.DEFAULTS, FavouriteSymbols.replace(FavouriteSymbols.DEFAULTS, -1, "€"))
        assertEquals(FavouriteSymbols.DEFAULTS, FavouriteSymbols.replace(FavouriteSymbols.DEFAULTS, 1, " "))
    }

    @Test
    fun `every default symbol is reachable from the symbol pages`() {
        // Otherwise a slot could be changed but never changed back.
        val available = listOf(SymbolLayout.page1, SymbolLayout.page2)
            .flatMap { it.rows }
            .flatMap { it.keys }
            .mapNotNull { (it.action as? KeyAction.Insert)?.text }
            .toSet()

        assertEquals(emptySet<String>(), FavouriteSymbols.DEFAULTS.toSet() - available)
    }
}
