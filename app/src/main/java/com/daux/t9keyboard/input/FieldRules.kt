package com.daux.t9keyboard.input

import android.text.InputType

/**
 * Where the writing aids must keep quiet.
 *
 * Capitals and automatic spaces help in prose and actively damage everything else: a
 * space after the dot in `nome.cognome@posta.it`, a capital at the start of a password,
 * a space inside `3,14`. The field itself says which kind it is, so the safest rule is
 * to help only where the field is plain text.
 */
object FieldRules {

    data class Allowed(val autoCapitalise: Boolean, val autoSpace: Boolean) {
        companion object {
            val ALL = Allowed(autoCapitalise = true, autoSpace = true)
            val NONE = Allowed(autoCapitalise = false, autoSpace = false)
        }
    }

    fun forInputType(inputType: Int): Allowed {
        val cls = inputType and InputType.TYPE_MASK_CLASS
        // Numbers, phone numbers and dates: every separator is part of the value.
        if (cls != InputType.TYPE_CLASS_TEXT) return Allowed.NONE

        // Terminals, code editors and identifier fields ask for no help at all.
        if (inputType and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return Allowed.NONE

        return when (inputType and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_FILTER -> Allowed.NONE

            else -> Allowed.ALL
        }
    }
}
