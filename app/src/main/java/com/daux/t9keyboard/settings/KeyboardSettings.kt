package com.daux.t9keyboard.settings

import android.content.Context
import com.daux.t9keyboard.model.FavouriteSymbols

/**
 * Small, local user preferences. Deliberately **not** Room: this is a handful of
 * values read once at startup and written on a tap, where a database would be all
 * cost and no benefit — unlike the personal dictionary, which is a real dataset.
 *
 * The Phase 3 settings screen will grow from here (column side, keyboard height,
 * candidate text size).
 */
class KeyboardSettings(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The column's rest-state symbols, always exactly [FavouriteSymbols.COUNT]. */
    fun favouriteSymbols(): List<String> {
        val stored = prefs.getString(KEY_FAVOURITES, null)
            ?.split(SEPARATOR)
            .orEmpty()
        return FavouriteSymbols.normalize(stored)
    }

    /**
     * Put [symbol] in slot [index] and return the new list. Symbols already in the
     * column swap places (see [FavouriteSymbols.replace]).
     */
    fun setFavouriteSymbol(index: Int, symbol: String): List<String> {
        val updated = FavouriteSymbols.replace(favouriteSymbols(), index, symbol)
        prefs.edit().putString(KEY_FAVOURITES, updated.joinToString(SEPARATOR)).apply()
        return updated
    }

    private companion object {
        const val FILE = "keyboard_settings"
        const val KEY_FAVOURITES = "favourite_symbols"

        /** A character no symbol can contain, so the list survives a round trip. */
        const val SEPARATOR = "\n"
    }
}
