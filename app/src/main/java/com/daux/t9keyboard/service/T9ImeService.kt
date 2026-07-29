package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.text.InputType
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
import com.daux.t9keyboard.input.ShiftState
import com.daux.t9keyboard.learning.RoomLearnedWordsStore
import com.daux.t9keyboard.model.FavouriteSymbols
import com.daux.t9keyboard.model.KeyAction
import com.daux.t9keyboard.model.KeySpec
import com.daux.t9keyboard.model.KeyboardMode
import com.daux.t9keyboard.model.LongPressKeys
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

    private var shift = ShiftState.OFF

    /** Email/URL field: the `1` key's popup offers address parts instead of symbols. */
    private var emailField = false

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
            onEditSymbol = ::onEditFavourite,
            keyAlternates = ::alternatesFor
        )
        keyboardView = view
        render()
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        emailField = isAddressField(info)
        // A new field starts on letters, wherever the previous one left the keyboard.
        keyboardView?.hidePopup()
        keyboardView?.setMode(KeyboardMode.T9)
        resetComposition()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        currentInputConnection?.finishComposingText()
        keyboardView?.hidePopup()
        resetComposition()
    }

    /** Email address or URL field — where `.com` is wanted and nowhere else. */
    private fun isAddressField(info: EditorInfo?): Boolean {
        val type = info?.inputType ?: return false
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        return when (type and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI -> true
            else -> false
        }
    }

    // --- Long-press popups ----------------------------------------------------

    /**
     * What a key offers when held. Asked at press time because the answer depends on
     * the current state: the favourites, the field type, and the capitalisation.
     *
     * Only the *labels* are capitalised: the actions keep the lowercase letter, so the
     * composition and the dictionary stay unaffected by shift, exactly as elsewhere.
     */
    private fun alternatesFor(spec: KeySpec): List<KeySpec> {
        val cells = LongPressKeys.forKey(spec.action, favourites, emailField)
        if (cells.isEmpty()) return cells
        if (!shift.appliesToNext(atWordStart = !state.isForcing())) return cells
        return cells.map {
            if (it.action is KeyAction.ForceLetter) it.copy(mainLabel = it.mainLabel.uppercase())
            else it
        }
    }

    /**
     * A letter picked from a key's popup: the same thing the column does, at the end
     * of the word rather than at the column's active position.
     *
     * Positions still unresolved are first filled in **from what the field is already
     * showing**, so the word does not change under the user: without this, forcing a
     * letter after typing "cas" predictively would resolve position 0 with it and turn
     * the word into something else entirely.
     */
    private fun onForceLetter(digit: Int, letter: Char) {
        resolvePendingFromPreview()
        state.pressDigit(digit)
        state.chooseLetter(letter)
        render()
    }

    private fun resolvePendingFromPreview() {
        val shown = previewWord()
        while (true) {
            val pending = state.activeColumnDigit() ?: return
            val position = state.forcedText().length
            val fallback = T9Keypad.letters[pending]?.firstOrNull() ?: return
            val letter = shown.getOrNull(position) ?: fallback
            // The fallback is always a letter of its own digit, so this terminates.
            if (!state.chooseLetter(letter) && !state.chooseLetter(fallback)) return
        }
    }

    /** Insert a pair and leave the cursor between the halves — `()` typed in one go. */
    private fun onInsertPair(open: String, close: String) {
        val ic = currentInputConnection ?: return
        if (!state.isEmpty()) commitCurrentWord()
        ic.commitText(open, 1)
        // A non-positive position is measured from the start of the inserted text, so
        // the cursor lands between the two halves without any absolute arithmetic.
        ic.commitText(close, 0)
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
            KeyAction.DeleteWord -> onDeleteWord()
            KeyAction.Enter -> onEnter()
            is KeyAction.Insert -> onInsert(action.text)
            is KeyAction.InsertPair -> onInsertPair(action.open, action.close)
            is KeyAction.ForceLetter -> onForceLetter(action.digit, action.letter)
            is KeyAction.Mode -> onModeSwitch(action.target)
            KeyAction.Shift -> onShift()
            // Wired for real in Phase 3; no-op for now (present for layout fidelity).
            KeyAction.Mic -> Unit
        }
    }

    /** `⇧` cycles off → next word capitalised → caps lock. */
    private fun onShift() {
        shift = shift.next()
        render() // preview, keys and column follow immediately
    }

    /**
     * Switch surface (T9 ↔ symbol pages). The word in progress is committed first:
     * leaving the keypad mid-word would strand a composing text no key can finish.
     */
    private fun onModeSwitch(target: KeyboardMode) {
        if (!state.isEmpty()) commitCurrentWord()
        keyboardView?.hidePopup()
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
            // The key shows "@", so tapping it types "@" — the same invariant the symbol
            // pages are held to. Everything else it offers is under its long-press.
            1 -> onInsert("@")
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

    private fun setShift(state: ShiftState) {
        if (shift == state) return
        shift = state
        renderShift()
    }

    /**
     * Show the capitalisation where it is visible. The keys type the character after
     * the last digit pressed; the column resolves the first unresolved position — so
     * with a one-shot shift the two are not always at the start of the word together.
     */
    private fun renderShift() {
        keyboardView?.setShiftState(
            shift,
            keysUppercase = shift.appliesToNext(atWordStart = state.isEmpty()),
            columnUppercase = shift.appliesToNext(atWordStart = !state.isForcing())
        )
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

    /**
     * What holding backspace becomes: drop the word in progress, or the word before
     * the cursor — trailing spaces included, so a second hold does not just eat the
     * gap it left behind.
     */
    private fun onDeleteWord() {
        val ic = currentInputConnection ?: return
        if (!state.isEmpty()) {
            ic.finishComposingText()
            resetComposition()
            render()
            return
        }
        val before = ic.getTextBeforeCursor(WORD_SCAN_CHARS, 0) ?: return
        if (before.isEmpty()) return

        var end = before.length
        while (end > 0 && before[end - 1].isWhitespace()) end--
        while (end > 0 && !before[end - 1].isWhitespace()) end--
        ic.deleteSurroundingText(before.length - end, 0)
    }

    private fun onEnter() {
        if (!state.isEmpty()) commitCurrentWord()
        sendDefaultEditorAction(true)
    }

    private fun onPickCandidate(candidate: Candidate) {
        val ic = currentInputConnection ?: return
        ic.setComposingText(shift.apply(candidate.word), 1)
        ic.finishComposingText()
        learn(candidate.word)
        setShift(shift.afterCommit())
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
            keyboardView?.setColumnLetters(T9Keypad.columnLetters(columnDigit))
        } else {
            keyboardView?.setColumnFavourites(favourites)
        }
        keyboardView?.setSuggestions(candidates)
        renderShift()

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
    private fun currentPreview(): String =
        // Capitalisation is applied here, at the last moment: the composition and the
        // dictionary stay lowercase, so learning and lookups are unaffected by shift.
        shift.apply(previewWord())

    /** The preview before capitalisation — one letter per pressed digit. */
    private fun previewWord(): String = when {
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
            learn(word) // stored lowercase: "Casa" and "casa" are the same word
            setShift(shift.afterCommit())
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

    private companion object {
        /** How far back to look for the start of a word. Longer than any word. */
        const val WORD_SCAN_CHARS = 64
    }
}
