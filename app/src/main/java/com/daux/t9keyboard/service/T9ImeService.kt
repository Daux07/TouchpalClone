package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.engine.DictionaryEngine
import com.daux.t9keyboard.engine.ItalianDictionaryEngine
import com.daux.t9keyboard.input.ComposeState
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.T9Keypad
import com.daux.t9keyboard.ui.T9KeyboardView

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
 * Not yet here: learning forced words into a personal dictionary (Phase 1.5) and
 * favourite symbols in the column's rest state (Phase 3).
 */
class T9ImeService : InputMethodService() {

    private lateinit var engine: DictionaryEngine
    private var keyboardView: T9KeyboardView? = null

    private val state = ComposeState()
    private var candidates: List<Candidate> = emptyList()

    override fun onCreate() {
        super.onCreate()
        engine = ItalianDictionaryEngine.fromAssets(this, "dict/it_test.txt")
    }

    override fun onCreateInputView(): View {
        val view = T9KeyboardView(
            context = this,
            onKey = ::onKey,
            onPickCandidate = ::onPickCandidate,
            onPickLetter = ::onPickLetter
        )
        keyboardView = view
        render()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
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
        when (action) {
            is KeyAction.Digit -> onDigit(action.n)
            KeyAction.Space -> onSpace()
            KeyAction.Backspace -> onBackspace()
            KeyAction.Enter -> onEnter()
            is KeyAction.Insert -> onInsert(action.text)
            // Wired for real in Phase 3; no-ops for now (present for layout fidelity).
            KeyAction.Shift, KeyAction.ModeSwitch, KeyAction.Emoji, KeyAction.Mic -> Unit
        }
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
        else engine.lookup(state.sequenceString())

        val columnDigit = state.activeColumnDigit()
        keyboardView?.setColumnLetters(
            if (columnDigit != null) T9Keypad.letters[columnDigit].orEmpty() else emptyList()
        )
        keyboardView?.setSuggestions(candidates)

        val preview = currentPreview()
        if (preview.isEmpty()) {
            ic.setComposingText("", 1)
            ic.finishComposingText()
        } else {
            ic.setComposingText(preview, 1)
        }
    }

    /** What should currently appear (as composing text) in the field. */
    private fun currentPreview(): String = when {
        state.isForcing() -> state.forcedText()
        candidates.isNotEmpty() -> candidates.first().word
        else -> state.sequenceString()
    }

    private fun commitCurrentWord() {
        val ic = currentInputConnection ?: return
        val word = currentPreview()
        if (word.isNotEmpty()) {
            ic.setComposingText(word, 1)
            ic.finishComposingText()
        }
        resetComposition()
    }

    private fun resetComposition() {
        state.reset()
        candidates = emptyList()
        keyboardView?.setColumnLetters(emptyList())
        keyboardView?.setSuggestions(emptyList())
    }
}
