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

    /**
     * How tall the keypad is, as a **percentage of the screen** — the number
     * `KeyboardView` measures itself against.
     *
     * Stored as a whole percent rather than a float, because that is what a slider can
     * move in and what a readout can say. The cost is stated rather than hidden: Steps
     * 1.25 and 2.5 measured their way to **28.7%**, and a whole percent cannot hold it, so
     * the default is 29 — about 3px taller on a 2856px screen, which is below the width of
     * the line between two keys.
     */
    var bodyHeightPercent: Int
        get() = prefs.getInt(KEY_BODY_HEIGHT, DEFAULT_BODY_HEIGHT_PERCENT)
            .coerceIn(MIN_BODY_HEIGHT_PERCENT, MAX_BODY_HEIGHT_PERCENT)
        set(value) = prefs.edit()
            .putInt(
                KEY_BODY_HEIGHT,
                value.coerceIn(MIN_BODY_HEIGHT_PERCENT, MAX_BODY_HEIGHT_PERCENT)
            )
            .apply()

    /** [bodyHeightPercent] as the fraction the measuring code actually wants. */
    fun bodyHeightFraction(): Float = bodyHeightPercent / 100f

    /**
     * How wide the keys are, as a **percentage of the screen** (Step 3.4).
     *
     * The panel behind them stays full width whatever this says: what narrows is the keys,
     * so a thumb can reach the far side of a big phone without the keyboard looking like
     * it fell off the edge.
     *
     * **The default is 100 on purpose.** Narrowing costs accuracy — the same keys with
     * less room — so it is worth it only to somebody who cannot reach across their screen,
     * and they can say so. A keyboard that arrives already narrowed would be worse for
     * everyone who did not ask.
     */
    var keyboardWidthPercent: Int
        get() = prefs.getInt(KEY_WIDTH, DEFAULT_WIDTH_PERCENT)
            .coerceIn(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT)
        set(value) = prefs.edit()
            .putInt(KEY_WIDTH, value.coerceIn(MIN_WIDTH_PERCENT, MAX_WIDTH_PERCENT))
            .apply()

    /** [keyboardWidthPercent] as the fraction the measuring code wants. */
    fun keyboardWidthFraction(): Float = keyboardWidthPercent / 100f

    /**
     * Which edge the narrowed keys sit against — left for a left-handed grip, right
     * otherwise. Means nothing at all while [keyboardWidthPercent] is 100.
     */
    var keyboardOnLeft: Boolean
        get() = prefs.getBoolean(KEY_ON_LEFT, false)
        set(value) = prefs.edit().putBoolean(KEY_ON_LEFT, value).apply()

    /**
     * Candidate text size in sp, driving `SuggestionBarView.textSizeSp`.
     *
     * The bar's own height does **not** follow it: `BAR_DP` is fixed, and Step 1.25 sized
     * the two together on purpose so the words never touch the edges of their strip. The
     * ceiling here is what still fits that strip, not what still fits the screen.
     */
    var candidateTextSp: Int
        get() = prefs.getInt(KEY_CANDIDATE_SP, DEFAULT_CANDIDATE_SP)
            .coerceIn(MIN_CANDIDATE_SP, MAX_CANDIDATE_SP)
        set(value) = prefs.edit()
            .putInt(KEY_CANDIDATE_SP, value.coerceIn(MIN_CANDIDATE_SP, MAX_CANDIDATE_SP))
            .apply()

    companion object {
        const val DEFAULT_HAPTIC_MS = 18

        /** Beyond this it stops reading as a key and starts reading as a buzz. */
        const val MAX_HAPTIC_MS = 60

        /** The height Steps 1.25 and 2.5 measured their way to. See [bodyHeightPercent]. */
        const val DEFAULT_BODY_HEIGHT_PERCENT = 29

        /**
         * Below this the keys stop being aimable — Step 1.25 already found the floor by
         * going too far in the other direction and having to come back up in 2.5.
         */
        const val MIN_BODY_HEIGHT_PERCENT = 22

        /**
         * Above this the keys come out **taller than wide**, which Step 1.25 measured and
         * rejected: it reads as a numeric keypad, and it spends on height what the text
         * being written needs more. Left reachable rather than forbidden — it is the
         * user's screen — but the range stops where the shape stops making sense.
         */
        const val MAX_BODY_HEIGHT_PERCENT = 40

        /** Full width: the keyboard nobody asked to narrow. See [keyboardWidthPercent]. */
        const val DEFAULT_WIDTH_PERCENT = 100

        /**
         * Below this the keys are narrower than a thumb is wide on a phone that was not
         * big enough to need this in the first place. The point is reaching the far side
         * of a large screen, not making a small keyboard.
         */
        const val MIN_WIDTH_PERCENT = 60

        const val MAX_WIDTH_PERCENT = 100

        /** `SuggestionBarView.DEFAULT_TEXT_SP`, as an integer the slider can move in. */
        const val DEFAULT_CANDIDATE_SP = 17

        /** Smaller than this and the words are legible only to whoever wrote them. */
        const val MIN_CANDIDATE_SP = 12

        /** Larger than this and the words touch the edges of a strip that does not grow. */
        const val MAX_CANDIDATE_SP = 24

        private const val FILE = "keyboard_settings"
        private const val KEY_HAPTIC_MS = "haptic_ms"
        private const val KEY_LANGUAGES = "secondary_languages"
        private const val KEY_FAVOURITES = "favourite_symbols"
        private const val KEY_AUTO_CAPS = "auto_capitalise"
        private const val KEY_AUTO_SPACE = "auto_space"
        private const val KEY_BODY_HEIGHT = "body_height_percent"
        private const val KEY_CANDIDATE_SP = "candidate_text_sp"
        private const val KEY_WIDTH = "keyboard_width_percent"
        private const val KEY_ON_LEFT = "keyboard_on_left"

        /** A character no symbol can contain, so the list survives a round trip. */
        private const val SEPARATOR = "\n"
    }
}
