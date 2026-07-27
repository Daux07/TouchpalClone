package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.View
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.T9Keypad
import com.daux.t9keyboard.ui.T9KeyboardView

/**
 * Entry point of the T9 keyboard.
 *
 * Phase 1.1: classic multi-tap text entry, as a first tangible, testable slice
 * that exercises the whole pipeline (grid → keys → InputConnection, backspace,
 * space, enter). This multi-tap logic is a stepping stone: Phase 1.2 replaces it
 * with predictive T9 + the manual disambiguation column, reusing the same grid.
 */
class T9ImeService : InputMethodService() {

    private val handler = Handler(Looper.getMainLooper())
    private val commitRunnable = Runnable { finalizePending() }

    /** Digit whose letters are currently being cycled, or -1 if none pending. */
    private var pendingDigit: Int = -1
    private var pendingIndex: Int = 0

    override fun onCreateInputView(): View =
        T9KeyboardView(this) { action -> onKey(action) }

    override fun onFinishInput() {
        super.onFinishInput()
        finalizePending()
    }

    private fun onKey(action: KeyAction) {
        when (action) {
            is KeyAction.Digit -> onDigit(action.n)
            KeyAction.Backspace -> onBackspace()
            KeyAction.Enter -> onEnter()
        }
    }

    private fun onDigit(n: Int) {
        val ic = currentInputConnection ?: return
        val letters = T9Keypad.letters[n] ?: return
        handler.removeCallbacks(commitRunnable)

        // 0 is a space: commit any pending char first, then a space.
        if (n == 0) {
            finalizePending()
            ic.commitText(" ", 1)
            return
        }

        if (pendingDigit == n) {
            // Same key again: cycle to the next letter in place.
            pendingIndex = (pendingIndex + 1) % letters.size
        } else {
            // Different key: commit the previous pending char, start a new one.
            finalizePending()
            pendingDigit = n
            pendingIndex = 0
        }
        ic.setComposingText(letters[pendingIndex].toString(), 1)
        handler.postDelayed(commitRunnable, MULTITAP_TIMEOUT_MS)
    }

    private fun onBackspace() {
        val ic = currentInputConnection ?: return
        if (pendingDigit != -1) {
            // Remove the in-progress (composing) char without committing it.
            ic.setComposingText("", 1)
            ic.finishComposingText()
            resetPending()
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    private fun onEnter() {
        finalizePending()
        // Respects the field's action (search/done/next) and inserts a newline in
        // multiline fields, instead of blindly committing "\n".
        sendDefaultEditorAction(true)
    }

    /** Commit whatever char is currently being cycled and clear the pending state. */
    private fun finalizePending() {
        if (pendingDigit != -1) {
            currentInputConnection?.finishComposingText()
            resetPending()
        }
        handler.removeCallbacks(commitRunnable)
    }

    private fun resetPending() {
        pendingDigit = -1
        pendingIndex = 0
    }

    companion object {
        private const val MULTITAP_TIMEOUT_MS = 800L
    }
}
