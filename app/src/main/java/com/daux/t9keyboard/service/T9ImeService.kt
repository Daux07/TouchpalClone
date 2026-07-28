package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.engine.DictionaryEngine
import com.daux.t9keyboard.engine.FuzzyDictionaryEngine
import com.daux.t9keyboard.engine.ItalianDictionaryEngine
import com.daux.t9keyboard.engine.LearnedWordsEngine
import com.daux.t9keyboard.engine.MergingDictionaryEngine
import com.daux.t9keyboard.R
import com.daux.t9keyboard.input.ComposeState
import com.daux.t9keyboard.learning.RoomLearnedWordsStore
import com.daux.t9keyboard.model.FavouriteSymbols
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeyboardMode
import com.daux.t9keyboard.model.T9Keypad
import com.daux.t9keyboard.settings.KeyboardSettings
import com.daux.t9keyboard.ui.KeyboardView

/**
 * Entry point of the T9 keyboard.
 *
 * Phase 1.3: predictive T9 plus the manual disambiguation column (the project's
 * core feature). Digit keys 2–9 extend a sequence; the engine predicts words for
 * it (shown as composing text + in the suggestion bar). In parallel the column
 * shows the letters of the current position's digit — tapping them forces a word
 * letter by letter, even one the dictionary doesn't know. Space/0 commits, enter
 * runs the editor action, backspace pops the last (digit, letter) pair.
 *
 * Phase 1.5 adds learning: every word actually confirmed goes into the personal
 * dictionary (Room), which is looked up *before* the corpus — so a word forced
 * through the column only has to be typed the hard way once.
 *
 * Not yet here: favourite symbols in the column's rest state (Phase 3).
 */
class T9ImeService : InputMethodService() {

    /**
     * Personal dictionary; available from the start (the corpus is not) so words
     * can be learned while the corpus is still loading.
     */
    private lateinit var learned: LearnedWordsEngine

    /** Learned + corpus. Reassigned once the corpus has been parsed. */
    @Volatile
    private var engine: DictionaryEngine? = null
    private var keyboardView: KeyboardView? = null

    private val state = ComposeState()
    private var candidates: List<Candidate> = emptyList()

    private lateinit var settings: KeyboardSettings

    /** The column's rest-state symbols, mirrored in RAM. */
    private var favourites: List<String> = FavouriteSymbols.DEFAULTS

    /** Slot waiting for a replacement symbol, while the symbol pages are open. */
    private var pendingFavouriteSlot: Int? = null

    override fun onCreate() {
        super.onCreate()
        settings = KeyboardSettings(this)
        favourites = settings.favouriteSymbols()

        val learnedEngine = LearnedWordsEngine(RoomLearnedWordsStore(this))
        learned = learnedEngine
        engine = FuzzyDictionaryEngine(learnedEngine)
        // ~50k-word Italian dictionary: parse off the main thread so the keyboard
        // shows instantly (predictions appear once loading completes, ~a moment).
        Thread {
            learnedEngine.load()
            val corpus = ItalianDictionaryEngine.fromAssets(this, "dict/it.txt")
            engine = FuzzyDictionaryEngine(
                MergingDictionaryEngine(listOf(learnedEngine, corpus))
            )
        }.apply { name = "dict-loader"; isDaemon = true }.start()
    }

    override fun onCreateInputView(): View {
        val view = KeyboardView(
            context = this,
            onKey = ::onKey,
            onPickCandidate = ::onPickCandidate,
            onPickLetter = ::onPickLetter,
            onPickSymbol = ::onInsert,
            onEditSymbol = ::onEditFavourite
        )
        keyboardView = view
        render()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // A new field starts on letters, wherever the previous one left the keyboard.
        keyboardView?.setMode(KeyboardMode.T9)
        resetComposition()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        currentInputConnection?.finishComposingText()
        resetComposition()
    }

    /** Always show our soft keyboard, even with a hardware keyboard attached. */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return true
    }

    // --- Key handling ---------------------------------------------------------

    private fun onKey(action: KeyAction) {
        if (consumedByFavouritePick(action)) return
        when (action) {
            is KeyAction.Digit -> onDigit(action.n)
            KeyAction.Space -> onSpace()
            KeyAction.Backspace -> onBackspace()
            KeyAction.Enter -> onEnter()
            is KeyAction.Insert -> onInsert(action.text)
            is KeyAction.Mode -> onModeSwitch(action.target)
            // Wired for real in Phase 3; no-ops for now (present for layout fidelity).
            KeyAction.Shift, KeyAction.Emoji, KeyAction.Mic -> Unit
        }
    }

    /**
     * Switch surface (T9 ↔ symbol pages). The word in progress is committed first:
     * leaving the keypad mid-word would strand a composing text no key can finish.
     */
    private fun onModeSwitch(target: KeyboardMode) {
        if (!state.isEmpty()) commitCurrentWord()
        keyboardView?.setMode(target)
        render()
    }

    private fun onSpace() {
        val ic = currentInputConnection ?: return
        if (!state.isEmpty()) commitCurrentWord()
        ic.commitText(" ", 1)
    }

    private fun onInsert(text: String) {
        val ic = currentInputConnection ?: return
        if (!state.isEmpty()) commitCurrentWord()
        ic.commitText(text, 1)
    }

    private fun onDigit(n: Int) {
        val ic = currentInputConnection ?: return
        when (n) {
            0 -> { // space: commit the word in progress, then a space
                if (!state.isEmpty()) commitCurrentWord()
                ic.commitText(" ", 1)
            }
            1 -> { // punctuation key: handled in Phase 3; for now just commit
                if (!state.isEmpty()) commitCurrentWord()
            }
            else -> { // 2–9: extend the sequence
                state.pressDigit(n)
                render()
            }
        }
    }

    // --- Favourite symbols ----------------------------------------------------

    /** Long-press on a column slot: open the symbol pages to choose its new symbol. */
    private fun onEditFavourite(slot: Int) {
        if (!state.isEmpty()) commitCurrentWord()
        pendingFavouriteSlot = slot
        keyboardView?.setMode(KeyboardMode.SYMBOLS_1)
        keyboardView?.setHint(getString(R.string.pick_favourite_symbol, slot + 1))
    }

    /**
     * While a slot is waiting, symbol keys assign instead of typing. Page switches
     * (`1/2`) are let through so the second page stays reachable; anything else —
     * including `abc` — cancels and behaves normally.
     *
     * Returns true when the key was fully consumed here.
     */
    private fun consumedByFavouritePick(action: KeyAction): Boolean {
        val slot = pendingFavouriteSlot ?: return false
        return when {
            action is KeyAction.Insert -> {
                favourites = settings.setFavouriteSymbol(slot, action.text)
                endFavouritePick()
                keyboardView?.setMode(KeyboardMode.T9)
                render()
                true
            }
            // Staying inside the symbol pages keeps the question open.
            action is KeyAction.Mode && action.target != KeyboardMode.T9 -> false
            else -> {
                endFavouritePick()
                false
            }
        }
    }

    private fun endFavouritePick() {
        pendingFavouriteSlot = null
        keyboardView?.setHint(null)
    }

    /** Tap on a letter in the disambiguation column: force it into the word. */
    private fun onPickLetter(letter: Char) {
        if (state.chooseLetter(letter)) render()
    }

    private fun onBackspace() {
        val ic = currentInputConnection ?: return
        if (state.backspace()) {
            render()
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun onEnter() {
        if (!state.isEmpty()) commitCurrentWord()
        sendDefaultEditorAction(true)
    }

    private fun onPickCandidate(candidate: Candidate) {
        val ic = currentInputConnection ?: return
        ic.setComposingText(candidate.word, 1)
        ic.finishComposingText()
        learn(candidate.word)
        resetComposition()
    }

    // --- Rendering & commit ---------------------------------------------------

    /**
     * Push the current state to the field (composing preview) and the keyboard
     * (suggestion bar + column). The preview is the forced word while the user is
     * disambiguating, otherwise the best prediction (or the raw digits if unknown).
     */
    private fun render() {
        val ic = currentInputConnection ?: return

        candidates = if (state.isEmpty()) emptyList()
        else engine?.lookup(state.sequenceString()).orEmpty()

        // Composing → letters to force; at rest → the favourite symbols.
        val columnDigit = state.activeColumnDigit()
        if (columnDigit != null) {
            keyboardView?.setColumnLetters(T9Keypad.letters[columnDigit].orEmpty())
        } else {
            keyboardView?.setColumnFavourites(favourites)
        }
        keyboardView?.setSuggestions(candidates)

        val preview = currentPreview()
        if (preview.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            ic.setComposingText(preview, 1)
        }
    }

    /**
     * What should currently appear (as composing text) in the field.
     *
     * Only *exact* candidates are previewed: a typo-tolerant one (Phase 1.7) is an
     * offer to tap, never something to commit behind the user's back — otherwise
     * typing a word the dictionary doesn't know would silently turn into a similar
     * one, which is exactly what the column exists to prevent.
     */
    private fun currentPreview(): String = when {
        state.isForcing() -> state.forcedText()
        else -> candidates.firstOrNull { !it.fuzzy }?.word
            ?: state.defaultLetters() // letters, never raw digits
    }

    private fun commitCurrentWord() {
        val ic = currentInputConnection ?: return
        val word = currentPreview()
        if (word.isNotEmpty()) {
            ic.setComposingText(word, 1)
            ic.finishComposingText()
            learn(word)
        }
        resetComposition()
    }

    /**
     * Remember a word the user confirmed, so it is proposed first next time — the
     * whole point of the column: force a word once, then just type its digits.
     * Cheap: in-RAM bump plus a queued database write.
     */
    private fun learn(word: String) {
        learned.learn(word, System.currentTimeMillis())
    }

    private fun resetComposition() {
        state.reset()
        candidates = emptyList()
        keyboardView?.setColumnFavourites(favourites)
        keyboardView?.setSuggestions(emptyList())
    }
}
