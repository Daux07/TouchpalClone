package com.daux.t9keyboard.model

/**
 * The numbers/symbols pages reached from `12#`, laid out **QWERTY-style**: ten
 * narrow keys per row, full width, instead of the T9 3×3 keypad.
 *
 * Two pages: the first holds digits and the punctuation used every day, the second
 * the rarer signs — they do not fit comfortably on one page at a thumb-friendly key
 * size. `1/2` and `2/2` (bottom-left of the key area) swap between them, `abc`
 * returns to T9.
 *
 * Same [KeyGrid] shape a QWERTY letter layout needs, which is the point: adding one
 * later means adding a grid here, not a new view.
 */
object SymbolLayout {

    private fun sym(text: String, weight: Float = 1f) =
        KeySpec(text, null, isFunction = false, KeyAction.Insert(text), weight)

    private fun func(label: String, action: KeyAction, weight: Float = 1f) =
        KeySpec(label, null, isFunction = true, action, weight)

    /** Bottom row, identical on both pages and echoing the T9 one. */
    private fun bottomRow(): KeyRow = KeyRow(
        listOf(
            func("abc", KeyAction.Mode(KeyboardMode.T9), weight = 1.6f),
            sym(","),
            KeySpec("space", null, isFunction = false, KeyAction.Space, weight = 4.2f),
            sym("."),
            func("⏎", KeyAction.Enter, weight = 1.6f)
        ),
        weight = 0.8f // thinner, like the T9 bottom row
    )

    private fun rowOf(vararg labels: String) = KeyRow(labels.map { sym(it) })

    /** Digits and the punctuation used every day. */
    val page1 = KeyGrid(
        listOf(
            rowOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            rowOf("@", "#", "€", "_", "&", "-", "+", "(", ")", "/"),
            KeyRow(
                listOf(
                    func("1/2", KeyAction.Mode(KeyboardMode.SYMBOLS_2), weight = 1.5f),
                    sym("*"), sym("\""), sym("'"), sym(":"),
                    sym(";"), sym("!"), sym("?"),
                    func("⌫", KeyAction.Backspace, weight = 1.5f)
                )
            ),
            bottomRow()
        )
    )

    /** The rarer signs: currencies, maths, brackets, marks. */
    val page2 = KeyGrid(
        listOf(
            rowOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"),
            rowOf("£", "¢", "$", "¥", "^", "°", "=", "{", "}", "\\"),
            KeyRow(
                listOf(
                    func("2/2", KeyAction.Mode(KeyboardMode.SYMBOLS_1), weight = 1.5f),
                    sym("%"), sym("©"), sym("®"), sym("™"),
                    sym("✓"), sym("["), sym("]"),
                    func("⌫", KeyAction.Backspace, weight = 1.5f)
                )
            ),
            bottomRow()
        )
    )

    fun forMode(mode: KeyboardMode): KeyGrid = when (mode) {
        KeyboardMode.SYMBOLS_2 -> page2
        else -> page1
    }
}
