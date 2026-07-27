package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.engine.DictionaryEngine
import com.daux.t9keyboard.engine.ItalianDictionaryEngine
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.ui.T9KeyboardView

/**
 * Entry point of the T9 keyboard.
 *
 * Phase 1.2: predictive T9. Digit keys 2–9 build a numeric sequence; the engine
 * maps it to dictionary words ordered by frequency. The best guess is shown as
 * composing text and the full list in the suggestion bar (tap to pick). Space/0
 * commits the current word, backspace shortens the sequence, enter runs the
 * editor action.
 *
 * Not yet here: the manual disambiguation column (Phase 1.3) and learning/Room
 * (Phase 1.5). Until the column exists, an unknown sequence (no dictionary match)
 * previews the raw digits.
 */
class T9ImeService : InputMethodService() {

    private lateinit var engine: DictionaryEngine
    private var keyboardView: T9KeyboardView? = null

    /** Digits typed for the word currently being composed. */
    private val sequence = StringBuilder()
    private var candidates: List<Candidate> = emptyList()

    override fun onCreate() {
        super.onCreate()
        engine = ItalianDictionaryEngine.fromAssets(this, "dict/it_test.txt")
    }

    override fun onCreateInputView(): View {
        val view = T9KeyboardView(
            context = this,
            onKey = ::onKey,
            onPickCandidate = ::onPickCandidate
        )
        keyboardView = view
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

    // --- Key handling ---------------------------------------------------------

    private fun onKey(action: KeyAction) {
        when (action) {
            is KeyAction.Digit -> onDigit(action.n)
            KeyAction.Backspace -> onBackspace()
            KeyAction.Enter -> onEnter()
        }
    }

    private fun onDigit(n: Int) {
        val ic = currentInputConnection ?: return
        when (n) {
            0 -> { // space: commit the word in progress, then a space
                if (sequence.isNotEmpty()) commitCurrentWord()
                ic.commitText(" ", 1)
            }
            1 -> { // punctuation key: handled in Phase 3; for now just commit
                if (sequence.isNotEmpty()) commitCurrentWord()
            }
            else -> { // 2–9: extend the sequence and refresh predictions
                sequence.append(n)
                refreshPredictions()
            }
        }
    }

    private fun onBackspace() {
        val ic = currentInputConnection ?: return
        if (sequence.isNotEmpty()) {
            sequence.deleteCharAt(sequence.length - 1)
            if (sequence.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                setSuggestions(emptyList())
            } else {
                refreshPredictions()
            }
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun onEnter() {
        if (sequence.isNotEmpty()) commitCurrentWord()
        sendDefaultEditorAction(true)
    }

    // --- Composition helpers --------------------------------------------------

    /** Look up the current sequence and show the best guess + the candidate list. */
    private fun refreshPredictions() {
        val ic = currentInputConnection ?: return
        candidates = engine.lookup(sequence.toString())
        val preview = candidates.firstOrNull()?.word ?: sequence.toString()
        ic.setComposingText(preview, 1)
        setSuggestions(candidates)
    }

    /** Commit the current best guess (or raw digits if no match) and reset. */
    private fun commitCurrentWord() {
        val ic = currentInputConnection ?: return
        val word = candidates.firstOrNull()?.word ?: sequence.toString()
        ic.setComposingText(word, 1)
        ic.finishComposingText()
        resetComposition()
    }

    private fun onPickCandidate(candidate: Candidate) {
        val ic = currentInputConnection ?: return
        ic.setComposingText(candidate.word, 1)
        ic.finishComposingText()
        resetComposition()
    }

    private fun resetComposition() {
        sequence.setLength(0)
        candidates = emptyList()
        setSuggestions(emptyList())
    }

    private fun setSuggestions(list: List<Candidate>) {
        keyboardView?.setSuggestions(list)
    }
}
