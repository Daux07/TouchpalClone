package com.daux.t9keyboard.engine

/**
 * A word proposed for a given T9 digit sequence.
 *
 * [weight] is on a comparable scale across sources (base corpus, learned words,
 * and — in Phase 2 — a second language), so candidate lists from different sources
 * can be merged by a simple sort. Higher weight = proposed first.
 */
data class Candidate(
    val word: String,
    val sequence: String,
    val weight: Long
)
