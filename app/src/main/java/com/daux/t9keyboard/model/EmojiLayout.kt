package com.daux.t9keyboard.model

/**
 * The emoji panel behind the `☺` key — a basic set, on the same [KeyGrid] the symbol
 * pages use (Phase 3 may grow it into categories with a recents row).
 *
 * Eight per row rather than ten: emoji need more room than punctuation to stay
 * recognisable at a glance.
 */
object EmojiLayout {

    private fun emoji(text: String) =
        KeySpec(text, null, isFunction = false, KeyAction.Insert(text))

    private fun rowOf(vararg items: String) = KeyRow(items.map { emoji(it) })

    val page = KeyGrid(
        listOf(
            rowOf("😀", "😂", "🙂", "😉", "😍", "😘", "😎", "🤔"),
            rowOf("😅", "😴", "😢", "😭", "😡", "😱", "🤗", "🙄"),
            rowOf("👍", "👎", "👏", "🙏", "💪", "👌", "✌️", "🤝"),
            KeyRow(
                listOf(
                    emoji("❤️"), emoji("🔥"), emoji("🎉"), emoji("✨"),
                    emoji("⭐"), emoji("☀️"), emoji("🌙"), emoji("💤")
                )
            ),
            KeyRow(
                listOf(
                    KeySpec("abc", null, isFunction = true, KeyAction.Mode(KeyboardMode.T9), 1.6f),
                    KeySpec("⌫", null, isFunction = true, KeyAction.Backspace, 1.2f),
                    KeySpec("space", null, isFunction = false, KeyAction.Space, 3.4f),
                    KeySpec("⏎", null, isFunction = true, KeyAction.Enter, 1.6f)
                ),
                weight = 0.8f
            )
        )
    )
}
