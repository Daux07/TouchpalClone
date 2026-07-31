package com.daux.t9keyboard.settings

import android.content.Context
import com.daux.t9keyboard.model.FavouriteSymbols
import com.daux.t9keyboard.model.Language

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

    /**
     * Which secondary dictionaries are loaded alongside the primary one, by code,
     * without switching language by hand
     * (plan §8). English is on by default — it is the point of Phase 2 — and the whole
     * thing is a set rather than a flag so a third language costs an entry in
     * [Language.SECONDARIES] and nothing here.
     */
    var secondaryLanguages: Set<String>
        // Copied out: the set SharedPreferences hands back must not be modified, and a
        // caller has no way of knowing that.
        get() = prefs.getStringSet(KEY_LANGUAGES, null)?.toSet()
            ?: Language.DEFAULT_SECONDARY_CODES
        set(value) = prefs.edit().putStringSet(KEY_LANGUAGES, value).apply()

    /** The declared languages that are switched on, in the order they are offered. */
    fun enabledSecondaries(): List<Language> {
        val codes = secondaryLanguages
        return Language.SECONDARIES.filter { it.code in codes }
    }

    /**
     * How long the tick under the finger lasts, in milliseconds; `0` switches it off.
     *
     * Stored as a duration rather than a boolean because the whole reason for driving
     * the `Vibrator` ourselves is that it can be **tuned** — see [com.daux.t9keyboard.ui.Haptics].
     * The default is a short tap: enough to feel, short enough not to blur when typing fast.
     */
    var hapticMs: Int
        get() = prefs.getInt(KEY_HAPTIC_MS, DEFAULT_HAPTIC_MS).coerceIn(0, MAX_HAPTIC_MS)
        set(value) = prefs.edit().putInt(KEY_HAPTIC_MS, value.coerceIn(0, MAX_HAPTIC_MS)).apply()

    companion object {
        const val DEFAULT_HAPTIC_MS = 18

        /** Beyond this it stops reading as a key and starts reading as a buzz. */
        const val MAX_HAPTIC_MS = 60

        private const val FILE = "keyboard_settings"
        private const val KEY_HAPTIC_MS = "haptic_ms"
        private const val KEY_LANGUAGES = "secondary_languages"
        private const val KEY_FAVOURITES = "favourite_symbols"
        private const val KEY_AUTO_CAPS = "auto_capitalise"
        private const val KEY_AUTO_SPACE = "auto_space"

        /** A character no symbol can contain, so the list survives a round trip. */
        private const val SEPARATOR = "\n"
    }
}
