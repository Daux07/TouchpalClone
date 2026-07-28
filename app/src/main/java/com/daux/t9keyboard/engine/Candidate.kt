package com.daux.t9keyboard.engine

/**
 * A word proposed for a given T9 digit sequence.
 *
 * [weight] is on a comparable scale across sources (base corpus, learned words,
 * and — in Phase 2 — a second language), so candidate lists from different sources
 * can be merged by a simple sort. Higher weight = proposed first.
 *
 * [fuzzy] marks a candidate that does *not* match the typed sequence exactly but is
 * one keypress away from it (Phase 1.7). Those are offers, never assumptions: they
 * appear at the tail of the suggestion bar and are only inserted if tapped — the
 * preview in the text field always follows an exact match.
 */
data class Candidate(
    val word: String,
    val sequence: String,
    val weight: Long,
    val fuzzy: Boolean = false
)
