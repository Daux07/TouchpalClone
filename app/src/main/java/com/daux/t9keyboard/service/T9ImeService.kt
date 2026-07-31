package com.daux.t9keyboard.service

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import com.daux.t9keyboard.engine.Candidate
import com.daux.t9keyboard.engine.DictionaryEngine
import com.daux.t9keyboard.engine.FuzzyDictionaryEngine
import com.daux.t9keyboard.engine.ItalianDictionaryEngine
import com.daux.t9keyboard.engine.LearnedWordsEngine
import com.daux.t9keyboard.engine.MergingDictionaryEngine
import com.daux.t9keyboard.engine.SingleLetterEngine
import com.daux.t9keyboard.R
import com.daux.t9keyboard.input.AutoShift
import com.daux.t9keyboard.input.AutoSpace
import com.daux.t9keyboard.input.ComposeState
import com.daux.t9keyboard.input.FieldRules
import com.daux.t9keyboard.input.ProperNouns
import com.daux.t9keyboard.input.SentenceRules
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

    /** True while the current shift state was chosen by the keyboard, not by the user. */
    private var shiftIsAutomatic = false

    /**
     * Set when the user touches `⇧`, cleared when a word is committed: their choice wins
     * for the word they are writing, and the next word is decided afresh.
     */
    private var shiftOverridden = false

    /** True while the last space was added by the keyboard, so it may still be taken back. */
    private var provisionalSpace = false

    /**
     * Set the moment the keyboard acts, cleared by the selection change that acting
     * causes. It is how [onUpdateSelection] tells our own edits from the user moving
     * the cursor with their finger — only the latter changes where composing happens.
     */
    private var selfEdit = false

    /** Email/URL field: the `1` key's popup offers address parts instead of symbols. */
    private var emailField = false

    /** What this field allows: prose gets the writing aids, addresses and codes do not. */
    private var fieldAllows = FieldRules.Allowed.ALL

    override fun onCreate() {
        super.onCreate()
        settings = KeyboardSettings(this)
        favourites = settings.favouriteSymbols()

        val learnedEngine = LearnedWordsEngine(RoomLearnedWordsStore(this))
        learned = learnedEngine
        // Single-letter ordering sits outermost: it has the last word on what a lone
        // keypress offers, whatever the corpus and the learned words make of it.
        engine = SingleLetterEngine(FuzzyDictionaryEngine(learnedEngine))
        // ~50k-word Italian dictionary: parse off the main thread so the keyboard
        // shows instantly (predictions appear once loading completes, ~a moment).
        Thread {
            learnedEngine.load()
            val corpus = ItalianDictionaryEngine.fromAssets(this, "dict/it.txt")
            ProperNouns.setKnown(corpus.properNouns)
            engine = SingleLetterEngine(
                FuzzyDictionaryEngine(MergingDictionaryEngine(listOf(learnedEngine, corpus)))
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
        fieldAllows = FieldRules.forInputType(info?.inputType ?: 0)
        // A new field starts on letters, wherever the previous one left the keyboard.
        keyboardView?.hidePopup()
        keyboardView?.setMode(KeyboardMode.T9)
        resetComposition()

        // Nothing carried over from the previous field: not the capital, not the space.
        provisionalSpace = false
        shiftOverridden = false
        shiftIsAutomatic = false
        setShift(ShiftState.OFF)
        updateAutoShift()
    }

    /**
     * The cursor moved, by the user's finger or by the app. Whether a capital belongs
     * here depends entirely on where the cursor now is, so the question is asked again.
     */
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        if (newSelStart == oldSelStart && newSelEnd == oldSelEnd) return

        // Our own edits land here too. Only a move the *user* made says anything about
        // where composing should now happen; reacting to ours would fight our own work.
        if (selfEdit) {
            selfEdit = false
        } else {
            adoptWordAtCursor(newSelStart, newSelEnd)
        }
        // Deliberately does *not* clear the provisional space: our own edits arrive here
        // too, and dropping the flag on them would undo the feature a moment after it
        // acted. What protects a stale flag is the check in consumedByProvisionalSpace.
        updateAutoShift()
    }

    /**
     * The user has moved the cursor by hand. Whatever was being composed belongs to
     * where the cursor *was*, so it is let go — and if the cursor has landed at the end
     * of a word, that word is taken over so typing continues it.
     *
     * This is what makes editing a written word possible: park the cursor after "far",
     * press 5-2, and the keyboard proposes "farla" and learns it on space — rather than
     * starting a separate word "la" and leaving "farla" unknown.
     *
     * The adoption is **required not to change the text**: if what would be rendered
     * differs by so much as a capital from what is already there, the word is left alone
     * and the keyboard simply starts fresh. A cursor tap must never rewrite the field.
     */
    private fun adoptWordAtCursor(selStart: Int, selEnd: Int) {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        resetComposition()
        provisionalSpace = false

        if (selStart != selEnd) return // a selection, not a caret: nothing to continue

        // Only at the *end* of a word. With letters still to the right the cursor sits
        // inside it, and extending it would insert in the middle of what is written.
        if (ic.getTextAfterCursor(1, 0)?.firstOrNull()?.isLetter() == true) return

        val before = ic.getTextBeforeCursor(WORD_SCAN_CHARS, 0) ?: return
        var start = before.length
        while (start > 0 && before[start - 1].isLetter()) start--
        val word = before.substring(start)
        if (word.length < 2 || !state.adopt(word)) return

        // The capital is read back from the text, so the preview reproduces it: it is a
        // fact about the word on screen, not a rule the keyboard is applying now.
        val restored = when {
            word.length > 1 && word.all { it.isUpperCase() } -> ShiftState.LOCK
            word.first().isUpperCase() -> ShiftState.ONCE
            else -> ShiftState.OFF
        }
        shiftIsAutomatic = false
        setShift(restored)

        if (currentPreview() != word) { // would rewrite the user's text: hands off
            resetComposition()
            return
        }
        ic.setComposingRegion(selStart - word.length, selStart)
        render()
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
        selfEdit = true
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
        // From here the state is the user's, not ours, until this word is committed.
        shiftIsAutomatic = false
        shiftOverridden = true
        render() // preview, keys and column follow immediately
    }

    // --- Automatic capitals ---------------------------------------------------

    /**
     * Ask the field whether a capital belongs here and follow the answer, unless the
     * user has said otherwise (see [AutoShift]).
     *
     * `getCursorCapsMode` is the platform's own answer, so this covers the start of a
     * field, the start of a line, and the word after `.`, `!` or `?` — and, for free,
     * fields that ask for every word capitalised, like a name in a contact form.
     */
    private fun updateAutoShift() {
        if (!settings.autoCapitalise || !fieldAllows.autoCapitalise || shiftOverridden) return
        if (!state.isEmpty()) return // mid-word: the decision was made when it began

        val ic = currentInputConnection ?: return
        val caps = ic.getCursorCapsMode(currentInputEditorInfo?.inputType ?: 0)
        val before = textBeforeCursor()
        val wanted = when {
            caps and TextUtils.CAP_MODE_CHARACTERS != 0 -> ShiftState.LOCK
            // The platform sees "dot, space, capital". It cannot know that the dot
            // belonged to "ecc." — that is ours to catch.
            caps != 0 && SentenceRules.endsWithAbbreviation(before) -> ShiftState.OFF
            caps != 0 -> ShiftState.ONCE
            // …and it may miss the capital that belongs after an opening quote.
            SentenceRules.afterOpeningAtSentenceStart(before) -> ShiftState.ONCE
            else -> ShiftState.OFF
        }

        AutoShift.resolve(shift, wanted, shiftIsAutomatic)?.let { resolved ->
            shiftIsAutomatic = resolved != ShiftState.OFF
            setShift(resolved)
        }
    }

    /** A word is behind us: the next one gets a fresh capitalisation decision. */
    private fun afterWordCommitted() {
        shiftOverridden = false
        updateAutoShift()
    }

    // --- Automatic spacing ----------------------------------------------------

    /**
     * Put the space between words in by itself, so the next word can be started right
     * away. Only after **choosing a candidate**: pressing space or enter means the user
     * is already handling the separator, and adding another would double it.
     *
     * The space is *provisional* — punctuation typed next takes it back (see [AutoSpace]).
     */
    private fun insertProvisionalSpace() {
        if (!settings.autoSpace || !fieldAllows.autoSpace) return
        currentInputConnection?.commitText(" ", 1)
        provisionalSpace = true
    }

    /**
     * Handle text arriving while a provisional space is pending; returns true when the
     * insertion has been dealt with here.
     *
     * Punctuation that hugs the previous word removes the space first — otherwise
     * choosing "casa" and typing a full stop would leave `casa .`, and the feature would
     * cost more deletions than it saves. If that punctuation ends a phrase, a fresh
     * space goes after it, which is where the next sentence starts and where the
     * automatic capital then lands.
     */
    /**
     * The space bar pressed straight after an automatic space: keep just the one, rather
     * than leaving the orphan double space the user did not ask for.
     */
    private fun swallowsExplicitSpace(): Boolean {
        if (!provisionalSpace) return false
        provisionalSpace = false
        // Trust the field, not the flag: the cursor may have moved, or the app may have
        // rewritten the text underneath us.
        return charBeforeCursor(offset = 0) == ' '
    }

    /** The character [offset] positions back from the cursor, or null if there is none. */
    private fun charBeforeCursor(offset: Int): Char? {
        val before = currentInputConnection?.getTextBeforeCursor(offset + 1, 0) ?: return null
        return before.getOrNull(before.length - 1 - offset)
    }

    /** Enough of the text before the cursor to recognise an abbreviation or an ellipsis. */
    private fun textBeforeCursor(): CharSequence =
        currentInputConnection?.getTextBeforeCursor(CONTEXT_CHARS, 0) ?: ""

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
        if (state.isEmpty() && swallowsExplicitSpace()) return
        if (!state.isEmpty()) commitCurrentWord()
        ic.commitText(" ", 1)
        afterWordCommitted()
    }

    /**
     * Insert literal text, applying the Italian spacing rules around it.
     *
     * The order matters: the word in progress is committed first (so what precedes is
     * final), then a provisional space is taken back if the symbol hugs the word, then
     * an opening bracket gets its space *before*, and only at the end does a phrase
     * ending earn a space after.
     */
    private fun onInsert(rawText: String) {
        selfEdit = true // also reached straight from the symbol pages, not only via onKey
        val ic = currentInputConnection ?: return
        if (!state.isEmpty()) commitCurrentWord()

        if (!fieldAllows.autoSpace) {
            provisionalSpace = false
            ic.commitText(rawText, 1)
            afterWordCommitted()
            return
        }

        // The symbol the *rules* reason about. Only the straight double quote differs
        // from what is typed: it both opens and closes, so its role is read from the
        // text and stands in for it here — while the field still gets the plain `"`.
        val symbol = if (rawText != "\"") rawText
        else if (AutoSpace.straightQuoteCloses(charBeforeCursor(offset = 0))) "”" else "“"

        // Take back our own space when the symbol belongs against the previous word.
        if (provisionalSpace && AutoSpace.hugsPreviousWord(symbol) &&
            charBeforeCursor(offset = 0) == ' '
        ) {
            ic.deleteSurroundingText(1, 0)
        }
        provisionalSpace = false

        val before = textBeforeCursor()
        if (AutoSpace.deservesPrecedingSpace(symbol, before.lastOrNull())) {
            ic.commitText(" ", 1)
        }

        ic.commitText(rawText, 1)

        if (AutoSpace.deservesFollowingSpace(symbol, before)) insertProvisionalSpace()
        afterWordCommitted()
    }

    private fun onDigit(n: Int) {
        val ic = currentInputConnection ?: return
        // A new word is starting: the space before it stands, whoever put it there.
        provisionalSpace = false
        when (n) {
            0 -> { // space: commit the word in progress, then a space
                if (!state.isEmpty()) commitCurrentWord()
                ic.commitText(" ", 1)
                afterWordCommitted()
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
        selfEdit = true
        if (state.chooseLetter(letter)) render()
    }

    /**
     * Backspace, with one extra job: **undoing the keyboard's last automatic decision**.
     *
     * If a capital is armed and nothing has been typed against it yet, the capital is
     * what the user is objecting to — so backspace disarms it instead of deleting a
     * character. Writing a lowercase word after a full stop takes one tap, not a
     * detour through `⇧`. An automatic space needs no special case: it is the last
     * character, so an ordinary delete already removes exactly it.
     */
    private fun onBackspace() {
        val ic = currentInputConnection ?: return

        // Undo the keyboard's last decision as well as the last character. A backspace
        // straight after an automatic capital means "not that capital" — but it must
        // still delete something, or the press would look like it did nothing at all.
        // The automatic space needs no special case: it *is* the last character.
        val undoingAutoCapital = state.isEmpty() && shiftIsAutomatic && shift != ShiftState.OFF
        if (undoingAutoCapital) {
            shiftIsAutomatic = false
            shiftOverridden = true // and do not re-arm it a moment later
            setShift(ShiftState.OFF)
        }

        provisionalSpace = false // whatever is being deleted, it is not ours to undo twice
        if (state.backspace()) {
            render()
        } else {
            ic.deleteSurroundingText(1, 0)
            // Deleting back past a full stop restores the capital — unless the user has
            // just said they do not want it.
            if (!undoingAutoCapital) updateAutoShift()
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
        provisionalSpace = false
        updateAutoShift()
    }

    private fun onEnter() {
        if (!state.isEmpty()) commitCurrentWord()
        provisionalSpace = false
        sendDefaultEditorAction(true)
        // A new line is a new sentence, when the editor kept the focus.
        afterWordCommitted()
    }

    private fun onPickCandidate(candidate: Candidate) {
        selfEdit = true
        val ic = currentInputConnection ?: return
        ic.setComposingText(shift.apply(ProperNouns.display(candidate.word)), 1)
        ic.finishComposingText()
        learn(candidate.word)
        setShift(shift.afterCommit())
        resetComposition()
        // Choosing a word is a finished word: the space that follows it is ours to add,
        // so the next one can be started without a detour to the space bar.
        insertProvisionalSpace()
        afterWordCommitted()
    }

    // --- Rendering & commit ---------------------------------------------------

    /**
     * Push the current state to the field (composing preview) and the keyboard
     * (suggestion bar + column). The preview is the forced word while the user is
     * disambiguating, otherwise the best prediction (or the raw digits if unknown).
     */
    private fun render() {
        val ic = currentInputConnection ?: return

        // While forcing, only the words that agree with the letters already forced are
        // offered: the column exists precisely to overrule the ranking, so proposing
        // "dara" to someone who has just spelled out "far" would undo their work.
        candidates = if (state.isEmpty()) emptyList()
        else engine?.lookup(state.sequenceString()).orEmpty().let { found ->
            val forced = state.forcedText()
            if (forced.isEmpty()) found else found.filter { it.word.startsWith(forced) }
        }

        // Composing → letters to force; at rest → the favourite symbols.
        val columnDigit = state.activeColumnDigit()
        if (columnDigit != null) {
            keyboardView?.setColumnLetters(T9Keypad.columnLetters(columnDigit))
        } else {
            keyboardView?.setColumnFavourites(favourites)
        }
        // Shown as they will be written: a suggestion reading "roma" that lands as "Roma"
        // makes the keyboard look like it changed its mind.
        keyboardView?.setSuggestions(
            if (properNounsActive()) candidates.map { it.copy(word = ProperNouns.display(it.word)) }
            else candidates
        )
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

    /**
     * The preview before the shift state is applied — one letter per pressed digit.
     *
     * A proper noun is capitalised here rather than by shift: "Roma" is written with a
     * capital wherever it falls in a sentence, and that is a property of the word, not
     * of where the cursor happens to be.
     */
    private fun previewWord(): String {
        // A candidate is preferred even while forcing, because the filter above has
        // already discarded the ones that contradict the forced letters: spelling out
        // "far" and pressing 5-2 should read "farla", not the bare default letters.
        val word = candidates.firstOrNull { !it.fuzzy }?.word
            ?: if (state.isForcing()) state.forcedPreview()
            else state.defaultLetters() // letters, never raw digits
        return if (properNounsActive()) ProperNouns.display(word) else word
    }

    private fun properNounsActive(): Boolean =
        settings.autoCapitalise && fieldAllows.autoCapitalise

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
        // Single letters are never learned. A learned word outranks the whole corpus, so
        // writing "è" once would demote "e" for good on the most common keypress there
        // is — a permanent cost for a keystroke that carries no information anyway. What
        // a lone key offers is decided by SingleLetterEngine, not by history.
        if (word.length < 2) return
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

        /** Enough context to recognise an abbreviation or an ellipsis before the cursor. */
        const val CONTEXT_CHARS = 24
    }
}
