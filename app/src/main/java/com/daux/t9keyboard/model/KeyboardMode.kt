package com.daux.t9keyboard.model

/**
 * Which surface the keyboard is showing.
 *
 * [T9] is the predictive keypad with the disambiguation column; the others are
 * plain **key grids** — rows of equal keys spanning the full width, rendered by
 * `GridKeyboardView` from a [KeyGrid]. That is the same shape a QWERTY needs, so
 * adding `QWERTY` here (as an alternative to T9, planned) is a new [KeyGrid] and
 * nothing else: no new view, no new plumbing.
 */
enum class KeyboardMode {
    T9,
    SYMBOLS_1,
    SYMBOLS_2,
    EMOJI
}
