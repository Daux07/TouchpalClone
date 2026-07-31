package com.daux.t9keyboard.settings

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.daux.t9keyboard.model.Language

/**
 * The settings screen, opened from the launcher icon.
 *
 * The first slice of Phase 3, brought forward because a preference nobody can reach is
 * not a preference. Only the languages are here: the rest (keyboard height, haptic
 * duration, candidate text size, column side) already exist in [KeyboardSettings] and
 * join this screen when Phase 3 proper arrives.
 *
 * Built in code rather than XML on purpose: the whole keyboard is, and a layout file for
 * a checkbox and two labels would be the only one in the project.
 */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = KeyboardSettings(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            // Without this the checkbox takes the initial focus and the scroll view
            // jumps to it, opening the screen already past its own heading.
            isFocusableInTouchMode = true
            requestFocus()
        }

        root.addView(
            title("T9 ${versionName()} — Impostazioni").apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            }
        )
        root.addView(title("Lingue"))
        root.addView(
            note(
                "L'italiano è sempre attivo. Una lingua in più viene proposta " +
                    "**dopo** le parole italiane, mai al loro posto: attivarla non cambia " +
                    "l'ordine di ciò che scrivi già."
            )
        )

        for (language in Language.SECONDARIES) {
            root.addView(
                CheckBox(this).apply {
                    text = language.label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    isChecked = language.code in settings.secondaryLanguages
                    setOnCheckedChangeListener { _, checked ->
                        val codes = settings.secondaryLanguages.toMutableSet()
                        if (checked) codes.add(language.code) else codes.remove(language.code)
                        settings.secondaryLanguages = codes
                    }
                }
            )
        }

        root.addView(
            note(
                "Il dizionario si ricarica alla prossima apertura della tastiera. " +
                    "Una lingua spenta non viene nemmeno letta: non occupa memoria."
            )
        )

        val scroll = ScrollView(this).apply {
            addView(
                root,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isFillViewport = true
        }
        // targetSdk 35 draws the window edge to edge, so the status and navigation bars
        // sit *over* this screen unless it says otherwise — exactly as `KeyboardView`
        // already does for the keyboard.
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        setContentView(scroll)
    }

    private fun versionName(): String =
        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
            .getOrNull() ?: ""

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
        gravity = Gravity.START
        setPadding(0, 0, 0, dp(8))
    }

    private fun note(text: String) = TextView(this).apply {
        this.text = text.replace("**", "")
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        alpha = 0.7f
        setPadding(0, dp(8), 0, dp(16))
    }

    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
}
