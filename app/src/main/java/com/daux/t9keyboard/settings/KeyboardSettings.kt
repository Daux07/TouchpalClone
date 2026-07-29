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

    /**
     * Capitalise the first word of a sentence by itself. On by default — but stored as
     * a preference from the start, because automatic typing help is exactly the kind of
     * thing people want to switch off, and the Phase 3 settings screen will offer it.
     */
    var autoCapitalise: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CAPS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CAPS, value).apply()

    /** Add the space between words automatically (see the auto-space rules). */
    var autoSpace: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPACE, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SPACE, value).apply()

    private companion object {
        const val FILE = "keyboard_settings"
        const val KEY_FAVOURITES = "favourite_symbols"
        const val KEY_AUTO_CAPS = "auto_capitalise"
        const val KEY_AUTO_SPACE = "auto_space"

        /** A character no symbol can contain, so the list survives a round trip. */
        const val SEPARATOR = "\n"
    }
}
