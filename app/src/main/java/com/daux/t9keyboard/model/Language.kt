package com.daux.t9keyboard.model

/**
 * A dictionary the keyboard can load, and the single place a new language is declared.
 *
 * Adding one is meant to be **an asset and a line here** — nothing else. The engine that
 * reads a dictionary (`CorpusDictionaryEngine`) has nothing language-specific in it, the
 * keypad is the same in every ITU-T E.161 language, and the ranking treats every
 * secondary the same way. What is missing for, say, Spanish is the corpus, not the code.
 *
 * [PRIMARY] is not in [SECONDARIES] on purpose: it is the language the keyboard is
 * *for*, always loaded, and it always ranks first (see `LanguagePriorityEngine`).
 */
data class Language(
    /** Stable id, stored in the preferences — never show this to the user. */
    val code: String,
    /** What the settings screen displays, in the language's own name. */
    val label: String,
    val asset: String
) {
    companion object {
        val PRIMARY = Language("it", "Italiano", "dict/it.txt")

        /** Every language that can be switched on alongside the primary one. */
        val SECONDARIES = listOf(
            Language("en", "English", "dict/en.txt")
        )

        /** On by default: the bilingual keyboard is what Phase 2 set out to build. */
        val DEFAULT_SECONDARY_CODES: Set<String> = setOf("en")

        fun secondary(code: String): Language? = SECONDARIES.firstOrNull { it.code == code }
    }
}
