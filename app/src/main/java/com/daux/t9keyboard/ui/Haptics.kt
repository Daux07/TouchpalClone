package com.daux.t9keyboard.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.daux.t9keyboard.settings.KeyboardSettings

/**
 * The tick under the finger when a key goes down.
 *
 * Deliberately the **`Vibrator` with an explicit duration** rather than
 * `performHapticFeedback(KEYBOARD_TAP)`: the system tick is whatever the phone decides
 * and cannot be tuned, and a tick that is too faint to feel is the same as no tick at
 * all. The cost of choosing it is that respecting the user's system-wide setting
 * becomes *our* job — [systemFeedbackOn] — because we are no longer going through the
 * platform call that would have honoured it for us. A keyboard that buzzes after you
 * have switched haptics off is a keyboard you uninstall.
 *
 * Duration lives in [KeyboardSettings] (`0` = off) and the Phase 3 settings screen will
 * expose the slider; everything here already reads it fresh, so it needs no changes.
 */
class Haptics(context: Context) {

    private val appContext = context.applicationContext
    private val settings = KeyboardSettings(appContext)

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** Cached answer + when it was read: this is a binder call, and keys are fast. */
    private var systemOn = true
    private var systemOnReadAt = 0L

    /** A key going down. Nothing at all when the duration is zero or haptics are off. */
    fun keyPress() = buzz(settings.hapticMs)

    /**
     * The long-press panel opening. A touch longer than a key press, because it marks a
     * *change of state* rather than a keystroke — the same distinction the system makes
     * between `KEYBOARD_TAP` and `LONG_PRESS`.
     */
    fun popupOpen() = buzz(settings.hapticMs * 2)

    private fun buzz(durationMs: Int) {
        if (durationMs <= 0) return
        val device = vibrator ?: return
        if (!device.hasVibrator() || !systemFeedbackOn()) return
        device.vibrate(
            VibrationEffect.createOneShot(
                durationMs.toLong(),
                VibrationEffect.DEFAULT_AMPLITUDE
            )
        )
    }

    /**
     * Whether the user wants tactile feedback at all, from the system-wide switch.
     *
     * Re-read at most once a second: often enough that turning it off in Settings takes
     * effect while the keyboard is open, rarely enough that it is not a binder call on
     * every keystroke.
     */
    private fun systemFeedbackOn(): Boolean {
        val now = System.currentTimeMillis()
        if (now - systemOnReadAt > SETTING_TTL_MS) {
            systemOn = Settings.System.getInt(
                appContext.contentResolver,
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                1
            ) != 0
            systemOnReadAt = now
        }
        return systemOn
    }

    private companion object {
        const val SETTING_TTL_MS = 1_000L
    }
}
