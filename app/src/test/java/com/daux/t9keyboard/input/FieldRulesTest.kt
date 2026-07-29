package com.daux.t9keyboard.input

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldRulesTest {

    private fun text(variation: Int = 0, flags: Int = 0) =
        FieldRules.forInputType(InputType.TYPE_CLASS_TEXT or variation or flags)

    @Test
    fun `plain prose gets both aids`() {
        val allowed = text()
        assertTrue(allowed.autoCapitalise)
        assertTrue(allowed.autoSpace)
    }

    /** A space after the dot in an address, or a capital in a password, is damage. */
    @Test
    fun `addresses and passwords get neither`() {
        for (variation in listOf(
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        )) {
            assertFalse(variation.toString(), text(variation).autoCapitalise)
            assertFalse(variation.toString(), text(variation).autoSpace)
        }
    }

    /** In `3,14` the comma is part of the number, not punctuation. */
    @Test
    fun `numbers, phones and dates get neither`() {
        for (cls in listOf(
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME
        )) {
            assertFalse(cls.toString(), FieldRules.forInputType(cls).autoSpace)
            assertFalse(cls.toString(), FieldRules.forInputType(cls).autoCapitalise)
        }
    }

    /** Terminals and code editors say so by asking for no suggestions. */
    @Test
    fun `a field that wants no suggestions is left alone`() {
        val allowed = text(flags = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        assertFalse(allowed.autoCapitalise)
        assertFalse(allowed.autoSpace)
    }
}
