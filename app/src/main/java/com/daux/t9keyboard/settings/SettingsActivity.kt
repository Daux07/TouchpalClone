package com.daux.t9keyboard.settings

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.daux.t9keyboard.model.Language
import com.daux.t9keyboard.ui.Haptics

/**
 * The settings screen, opened from the launcher icon.
 *
 * Started at Phase 2.1 with the languages alone — a preference nobody can reach is not a
 * preference — and filled out at Step 3.1 with the switches whose preferences were
 * already in [KeyboardSettings] waiting for somewhere to be shown.
 *
 * **Order of the sections: what you change often, first.** Writing aids and the tick
 * under the finger are the ones people try, dislike, and come back to; a language is set
 * once and forgotten. The languages were here first only because they arrived first.
 *
 * What is still missing needs something this screen does not have yet: keyboard height
 * and candidate text size are **sizes**, and a size chosen blind is chosen wrong. Step 3.2
 * puts a live keyboard under these controls, and Step 3.3 adds them above it.
 *
 * Built in code rather than XML on purpose: the whole keyboard is, and a layout file for
 * a few checkboxes and labels would be the only one in the project.
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
        root.addView(title("Scrittura"))
        root.addView(
            check("Maiuscola automatica a inizio frase", settings.autoCapitalise) {
                settings.autoCapitalise = it
            }
        )
        root.addView(
            check("Spazio automatico dopo un candidato", settings.autoSpace) {
                settings.autoSpace = it
            }
        )
        root.addView(
            note(
                "Spenti, la tastiera non tocca più ciò che scrivi da sé: la maiuscola " +
                    "la metti tu e lo spazio pure. Le regole restano quelle di sempre, " +
                    "semplicemente non vengono applicate."
            )
        )

        root.addView(title("Vibrazione"))
        root.addView(hapticControls(settings))

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

    private fun check(label: String, initial: Boolean, onChange: (Boolean) -> Unit) =
        CheckBox(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }

    /**
     * The duration of the tick under the finger, chosen by **feeling it**.
     *
     * This is the one setting that cannot be read off the screen: 12 ms and 24 ms are the
     * same number to look at and a different keyboard to type on. So the slider fires the
     * real [Haptics] at the value just chosen — the same code path a key press uses, so
     * what you feel here is exactly what you will feel there.
     *
     * It buzzes on **release**, not while dragging: a tick per pixel of travel is not
     * feedback, it is a rattle, and it would drown out the one thing being judged.
     */
    private fun hapticControls(settings: KeyboardSettings): LinearLayout {
        val haptics = Haptics(this)
        val readout = note("").apply { setPadding(0, 0, 0, dp(4)) }

        fun describe(ms: Int) {
            readout.text = if (ms == 0) "Spenta" else "$ms ms"
        }
        describe(settings.hapticMs)

        val slider = SeekBar(this).apply {
            max = KeyboardSettings.MAX_HAPTIC_MS
            progress = settings.hapticMs
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) {
                    settings.hapticMs = value
                    describe(value)
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = haptics.keyPress()
            })
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(readout)
            addView(slider)
            addView(
                note(
                    "Lascia andare il cursore per sentire la durata scelta. A zero la " +
                        "tastiera non vibra. Se hai spento il feedback tattile nelle " +
                        "impostazioni di Android, questa non lo riaccende."
                )
            )
        }
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
