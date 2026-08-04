package com.daux.t9keyboard.learning

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One word of the personal dictionary (plan §6): a word the user actually typed,
 * either forced letter by letter via the column or picked from the suggestions.
 *
 * [sequence] is stored (not recomputed at load time) so the whole table can be
 * turned into a lookup index in one pass. [uses] counts how many times the word
 * was confirmed; it feeds the candidate weight, so words used more often climb.
 */
@Entity(tableName = "learned_words")
data class LearnedWord(
    @PrimaryKey val word: String,
    val sequence: String,
    val uses: Long,
    val lastUsed: Long,
    /**
     * How the word is **written**, when that differs from [word] (Step 3.6): `xD` for
     * the key `xd`, `iPhone` for `iphone`. Null for the ordinary case, which is almost
     * every word.
     *
     * The key stays lowercase on purpose — "Casa" and "casa" must remain one word for
     * lookup, deduplication and counting. Only the *rendering* is remembered here, and
     * only when it carries a capital no rule could have produced.
     */
    val display: String? = null
)
