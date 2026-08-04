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
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.model.Language
import com.daux.t9keyboard.ui.Haptics
import com.daux.t9keyboard.ui.KeyboardView

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

    /**
     * The live keyboard at the foot of the screen. Built before the controls because the
     * size sliders need something to move — see [slider].
     */
    private lateinit var keyboardPreview: KeyboardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = KeyboardSettings(this)
        keyboardPreview = preview(settings)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            // Without this a control takes the initial focus and the scroll view jumps to
            // it, opening the screen already past its own heading. Claimed at the end of
            // this method, not here: asking an empty view for the focus wins only until
            // its children exist. That went unnoticed until Step 3.2, because until the
            // preview took its share of the screen everything fitted and there was nowhere
            // to scroll to.
            isFocusableInTouchMode = true
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

        root.addView(title("Dimensioni"))
        root.addView(
            slider(
                label = { "Altezza tastiera: $it%" },
                value = settings.bodyHeightPercent,
                min = KeyboardSettings.MIN_BODY_HEIGHT_PERCENT,
                max = KeyboardSettings.MAX_BODY_HEIGHT_PERCENT
            ) { settings.bodyHeightPercent = it }
        )
        root.addView(
            slider(
                label = { "Testo dei candidati: $it sp" },
                value = settings.candidateTextSp,
                min = KeyboardSettings.MIN_CANDIDATE_SP,
                max = KeyboardSettings.MAX_CANDIDATE_SP
            ) { settings.candidateTextSp = it }
        )
        root.addView(
            note(
                "La tastiera qui sotto è vera e cambia mentre muovi i cursori: quello " +
                    "che vedi è quello che avrai. I tasti mantengono le stesse proporzioni " +
                    "fra loro a qualunque altezza."
            )
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
        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(keyboardPreview, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        // targetSdk 35 draws the window edge to edge, so the status bar sits *over* this
        // screen unless it says otherwise.
        //
        // The inset is claimed by the outermost view, not by the scroll view inside it.
        // Until Step 3.2 the scroll view *was* the outermost one and held this itself; once
        // it became a child the padding stopped arriving, and the title spent a build
        // hidden under the clock. Only the top is handled here: the bottom belongs to the
        // preview keyboard, which reads the navigation bar inset for itself.
        ViewCompat.setOnApplyWindowInsetsListener(screen) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, 0)
            insets
        }
        setContentView(screen)
        root.requestFocus()
    }

    /**
     * A **real keyboard**, pinned to the bottom of the settings screen (Step 3.2).
     *
     * Not a picture of one and not a mock-up: this is [KeyboardView], the same class the
     * IME shows, with its callbacks going nowhere. That it can be built here at all is
     * the useful accident — it is a plain `FrameLayout` that takes a `Context` and some
     * lambdas, and never knew anything about `InputMethodService`. A keyboard that could
     * only exist inside its own service would have made this step a rewrite.
     *
     * **What it is for.** Keyboard height and candidate text size (Step 3.3) are *sizes*,
     * and a size chosen blind is chosen wrong — you would set a slider, leave, open a
     * text field, squint, and come back. Here the thing being measured is on the same
     * screen as the control measuring it.
     *
     * It already pays for itself before those sliders exist: the keys are live, so the
     * vibration slider above can be judged by **pressing a key** rather than by the one
     * buzz it fires on release. Same `KeyViewFactory`, so the same `Haptics`.
     *
     * The cog on the preview goes nowhere on purpose: it is already the settings screen,
     * and a control that reopens the screen you are on is a trap, not a shortcut.
     */
    private fun preview(settings: KeyboardSettings): KeyboardView =
        KeyboardView(
            context = this,
            onKey = {},
            onPickCandidate = {},
            onForgetCandidate = {},
            onPickLetter = {},
            onPickSymbol = {},
            onEditSymbol = {},
            onSettings = {},
            keyAlternates = { emptyList() }
        ).apply {
            // Every key is focusable, because in the IME they are real keys. Here that
            // meant one of them took the screen's initial focus and scrolled the settings
            // past their own heading — the same bug the root view already guards against
            // for the checkboxes. Blocked at the preview's root: a preview is something to
            // look at and press, never a stop on the way through the form.
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isFocusable = false
            setColumnFavourites(settings.favouriteSymbols())
            // Words rather than an empty strip: the bar is one of the things being sized,
            // and an empty one shows nothing of what the text size does to it.
            setSuggestions(SAMPLE.mapIndexed { i, word -> Candidate(word, "", (10 - i).toLong()) })
        }

    private fun check(label: String, initial: Boolean, onChange: (Boolean) -> Unit) =
        CheckBox(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            isChecked = initial
            setOnCheckedChangeListener { _, checked -> onChange(checked) }
        }

    /**
     * A labelled slider over a size, with the preview following it (Step 3.3).
     *
     * **The write and the redraw happen on every movement, not on release.** That is the
     * opposite of the vibration slider above, and for the opposite reason: a tick has to
     * be felt one at a time, while a size has to be *watched changing* — half the value of
     * the preview is seeing the keys pass through the height you nearly chose.
     *
     * It costs a `SharedPreferences` write per pixel of travel, which is the honest price
     * and a small one: `apply()` is asynchronous and the file is a handful of values.
     * Writing on release instead would mean the preview and the stored value disagreeing
     * for as long as the finger is down — the kind of gap that becomes a bug the moment
     * something else reads the setting mid-drag.
     *
     * [SeekBar] counts from zero, so [min] is carried in and out by hand rather than
     * leaking an offset into the callers.
     */
    private fun slider(
        label: (Int) -> String,
        value: Int,
        min: Int,
        max: Int,
        onChange: (Int) -> Unit
    ): LinearLayout {
        val readout = note(label(value)).apply { setPadding(0, 0, 0, dp(4)) }
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = value - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    val chosen = progress + min
                    readout.text = label(chosen)
                    onChange(chosen)
                    keyboardPreview.applySizeSettings()
                }

                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
            addView(readout)
            addView(bar)
        }
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

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

        /**
         * What the preview's bar shows. Ordinary Italian words of different lengths —
         * the point is to see the strip full at the size chosen, and a short word next
         * to a long one is what shows when the text stops fitting.
         */
        val SAMPLE = listOf("casa", "cara", "come", "quando", "perché", "insieme")
    }
}
