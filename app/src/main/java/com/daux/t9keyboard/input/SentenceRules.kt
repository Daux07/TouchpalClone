package com.daux.t9keyboard.input

/**
 * When a full stop really ends a sentence — the part Android's own `getCursorCapsMode`
 * cannot know.
 *
 * The platform sees a dot followed by a space and says "capital". In Italian that is
 * wrong often enough to be irritating: `ecc.`, `dott.`, `pag. 12` are abbreviations, not
 * sentence endings. And it is wrong in the other direction too, after an opening quote
 * or bracket, where the capital belongs to the word that follows.
 */
object SentenceRules {

    /**
     * Abbreviations that end in a dot without ending the sentence. Matched on the token
     * before the dot, so `p.v.` works: its internal dot is part of the token.
     *
     * `v.` and `vd.` are the written forms of "vedi", which has no dot of its own and so
     * could never match.
     */
    private val ABBREVIATIONS = setOf(
        "ecc", "dott", "dott.ssa", "prof", "prof.ssa", "ing", "sig", "sig.ra", "sig.na",
        "pag", "pagg", "cap", "rif", "ca", "p.v", "c.a", "art", "artt", "sac", "cfr",
        "v", "vd", "n", "nn", "es", "sec", "seg", "segg", "tel", "fax", "avv", "geom",
        "rag", "on", "gent", "spett", "egr"
    )

    /** Characters that open a quotation or a bracket, and pass the capital through. */
    private const val OPENING = "\"«“([{'‘"

    /**
     * True when the text ending here is an abbreviation's dot rather than a full stop.
     * [before] is the text up to and including the dot.
     */
    fun endsWithAbbreviation(before: CharSequence): Boolean {
        val trimmed = before.trimEnd()
        if (!trimmed.endsWith('.')) return false

        val token = trimmed.dropLast(1)
            .takeLastWhile { it.isLetter() || it == '.' }
            .toString()
            .trim('.')
            .lowercase()

        return token.isNotEmpty() && token in ABBREVIATIONS
    }

    /**
     * True when the cursor sits after an opening quote or bracket that itself follows the
     * end of a sentence — `Ciao. "` — so the next word starts a sentence of its own.
     */
    fun afterOpeningAtSentenceStart(before: CharSequence): Boolean {
        val trimmedEnd = before.trimEnd { it == ' ' }
        val opener = trimmedEnd.lastOrNull() ?: return false
        if (opener !in OPENING) return false

        val beforeOpener = trimmedEnd.dropLast(1).trimEnd()
        if (beforeOpener.isEmpty()) return true // start of the field
        return beforeOpener.last() in ".!?…" || beforeOpener.last() == '\n'
    }

    /** True when the text ends with three dots — the ellipsis, treated as one mark. */
    fun endsWithEllipsis(before: CharSequence): Boolean =
        before.endsWith("...") || before.endsWith("…")

    /** True when the text ends with one or two dots: an ellipsis still being typed. */
    fun endsWithPartialEllipsis(before: CharSequence): Boolean =
        before.endsWith("..") && !before.endsWith("...")
}
