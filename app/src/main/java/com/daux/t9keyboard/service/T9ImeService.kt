package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.view.View
import com.daux.t9keyboard.ui.PlaceholderKeyboardView

/**
 * Entry point of the T9 keyboard.
 *
 * Phase 0: shows a placeholder view only, to prove the IME installs, can be
 * enabled in system settings, selected, and rendered on screen. The real 12-key
 * grid + disambiguation column arrive in Phase 1.
 */
class T9ImeService : InputMethodService() {

    override fun onCreateInputView(): View {
        return PlaceholderKeyboardView(this)
    }
}
