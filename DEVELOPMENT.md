# DEVELOPMENT — Tastiera T9 stile TouchPal

> **File di tracciamento dello sviluppo.** Serve a riprendere il lavoro in sessioni
> successive. Aggiornato ad ogni step: task completati marcati `[x]`, stato corrente
> e prossimo passo aggiornati in cima. La fonte di verità sul *cosa* costruire resta
> `prompt-tastiera-t9-touchpal.md`; questo file traccia il *come/quando*.

---

## 📌 DIRETTIVA PERMANENTE — documentazione sempre allineata

**Nessuno step è finito finché la documentazione non è allineata.** Ogni modifica al
codice aggiorna, **nello stesso commit**:

1. **`DEVELOPMENT.md`** — la sezione *STATO CORRENTE* (ultimo step, prossimo passo), i
   task spuntati `[x]` nella fase relativa, e una voce nel *Log di sviluppo*.
2. **`docs/FUNCTIONAL.md`** — la sezione della feature toccata (comportamento e
   architettura di ciò che **esiste e funziona**), più la riga "Allineato a:" in cima.
3. **`prompt-tastiera-t9-touchpal.md`** — solo se cambia la *specifica* (cosa costruire),
   non per come è stata realizzata.

Vale anche per le modifiche piccole e per i cambi di rotta: se una decisione viene
ribaltata, la documentazione va **corretta**, non lasciata a raccontare il vecchio piano.
Un file disallineato è un bug, e va segnalato/riparato appena lo si nota.

---

## 🔖 STATO CORRENTE (aggiornare sempre qui)

- **Fase in corso:** Fase 1 — MVP **completa**, con gli step aggiuntivi nati dalla prova reale.
- **Ultimo step completato:** Step 1.12k — **i popup da 5 celle vanno su due righe (3+2)**, il che risolve anche l'ultima cella irraggiungibile sui tasti `6` e `9`. Prima: 1.12j (anche il popup del `.` preseleziona la prima cella), 1.12i (il gesto parte dalla cella preselezionata, risolto il salto della selezione al primo movimento), 1.12h (guadagni separati per asse, orizzontale ×1.5 e verticale ×2.5, pannello staccato di 10dp dal tasto), 1.12g (la cifra è preselezionata all'apertura: tenere premuto un tasto numerico e rilasciare scrive il suo numero), 1.12f (guadagno anche in orizzontale e cifra come prima cella), 1.12e (asse verticale amplificato ×2 e misurato dal dito anziché dal centro del tasto), 1.17 (dizionario da messaggi: sottotitoli 70% + prosa giornalistica 30%, che resta la fonte delle maiuscole per i nomi propri), 1.16 (nomi propri misurati), 1.15 (regole italiane di maiuscole e spaziatura), 1.14 (tasto singolo), 1.13 (maiuscola e spazio automatici), 1.12a–d (popup long-press).
- **Da provare sul telefono:** tutto lo Step 1.15 e 1.16 insieme alla prova reale già in sospeso — in particolare abbreviazioni e puntini di sospensione, coperti dai test ma non provati a mano.
- **Prossimo step:** **Fase 2 — bilingue IT+EN**. `MergingDictionaryEngine` è già pronto dallo Step 1.5: serve `EnglishDictionaryEngine` + corpus inglese.
- **Dopo:** **Fase 3** (impostazioni/ergonomia, dove rientrano la **QWERTY come layout alternativo** e lo **scorrimento del cursore trascinando sulla barra spazio**, entrambi richiesti dall'utente). Il test reale sullo smartphone continua in parallelo: `bash tools/dev.sh apk` → `app/build/outputs/apk/debug/app-debug.apk`.
- 📝 **Appunti dell'utente dalla prova reale (30/07 sera) — da affrontare per primi nella prossima sessione.** Tre cose, tutte sull'apostrofo e sull'apprendimento; nessuna ancora riprodotta né corretta:

  1. **La parola composta a mano non risulta imparata.** Serviva `farla`, che nel dizionario non c'è.
     **Riproduzione esatta, dettata dall'utente** — da eseguire *per prima cosa*, prima di toccare il codice:
     1. digitare la sequenza che dà **`farà`** e lasciarla comparire;
     2. **cancellare la `à`** (backspace: fa pop della coppia cifra+lettera);
     3. **tornare sulla fine della parola** e **aggiungere `la` al posto di `à`**, ottenendo `farla`;
     4. premere **spazio**.

     *Richiesta:* a quel punto la parola va **memorizzata**. In alternativa, o in aggiunta: **mostrare la parola scritta per intero anche fra i candidati**, così cliccandola la si conferma — e la si impara — senza passare dallo spazio.
     **Nota per chi riprende:** il percorso di apprendimento **esiste già** — `onSpace()` → `commitCurrentWord()` → `learn(word)`, e `learn` scarta solo le parole di una lettera. Quindi non è una funzione mancante ma un motivo per cui in *quel* flusso non ha effetto. Piste: cosa restituisce `currentPreview()` dopo un backspace che ha fatto pop di una coppia (`state.isForcing()` o no), e se la parola imparata vinca poi il ranking sulla sua sequenza. La seconda richiesta tocca invece la costruzione della lista candidati: oggi la parola composta a mano si vede nel campo ma **non è una voce cliccabile** della barra.
  2. **Maiuscola sbagliata dopo l'apostrofo:** scriveva `l'Aveva` invece di `l'aveva`. Da guardare `SentenceRules.OPENING`, che include `'` fra i segni di apertura (pensato per le virgolette), e il ricalcolo delle maiuscole dopo `onInsert` di un simbolo. Va distinta l'apostrofo **elisione** (dentro la parola) dall'apostrofo usato come virgoletta.
  3. **Elisioni come parola unica:** `l'`, `un'`, `d'`… oggi non è verificato se `l'aveva` venga imparata come una parola sola o spezzata in due. *Opinione dell'utente:* dev'essere **una parola unica**. Da decidere insieme al punto 2, perché entrambi dipendono dal considerare l'apostrofo interno alla parola.

- **Come riprendere:** leggi questa sezione + i task non spuntati qui sotto. Build/test rapido da terminale: vedi blocco "Build & test da riga di comando" più sotto.

> ℹ️ **Multi-tap rimosso**: dallo Step 1.2 l'inserimento è predittivo (digiti la
> sequenza di cifre → parole proposte). Se una sequenza non ha match nel dizionario,
> per ora l'anteprima mostra le cifre grezze; sarà la **colonna** (Step 1.3) a permettere
> di forzare parole non presenti.

> 🐞 **Gotcha emulatore (tastiera non compare):** se sull'emulatore l'IME è selezionato
> ma non appare nessuna tastiera, è l'impostazione `show_ime_with_hard_keyboard=0` (la
> tastiera hardware del PC nasconde quella software). Fix rapido:
> `adb shell settings put secure show_ime_with_hard_keyboard 1`. Il codice ora forza
> comunque la visualizzazione via `onEvaluateInputViewShown()` (no-op sui telefoni reali).

> 🛠️ **Build & test da riga di comando: `tools/dev.sh`.** Il wrapper `gradlew` non è nel
> repo, quindi ogni comando dovrebbe ripetere la stessa preparazione d'ambiente (JDK di
> Android Studio, SDK, ricerca del Gradle scaricato). Sta tutta nello script, da Git Bash:
> ```bash
> bash tools/dev.sh test              # unit test JVM (nessun emulatore)
> bash tools/dev.sh install           # build + installa su emulatore/telefono
> bash tools/dev.sh apk               # genera l'APK debug e stampa il percorso
> bash tools/dev.sh boot              # avvia l'emulatore e aspetta il boot
> bash tools/dev.sh ime               # abilita e seleziona la tastiera T9
> bash tools/dev.sh shot out.png      # screenshot dell'emulatore
> bash tools/dev.sh adb <args...>     # adb con l'ambiente già impostato
> bash tools/dev.sh gradle <task...>  # qualsiasi altro task
> ```
> Comandi corti e **identici ogni volta**, il che permette anche di autorizzarli una volta
> sola in `.claude/settings.local.json` invece di approvare ogni riga di script.

> ⚠️ **Il Gradle wrapper JAR e gli script `gradlew` non sono nel repo** (non generabili in
> questo ambiente senza SDK). Alla prima apertura in Android Studio, lascia che sincronizzi
> (rigenera il wrapper), oppure esegui `gradle wrapper --gradle-version 8.9`. Serve anche
> un `local.properties` con `sdk.dir` (lo crea Android Studio automaticamente).

### Decisioni tecniche fissate
- Package applicazione: `com.daux.t9keyboard`
- Nome app / label tastiera: **T9 Keyboard**
- `compileSdk` / `targetSdk`: **35** (Android 15) — compatibile con Android 15/16 su S25.
- `minSdk`: **26** (Android 8.0).
- Linguaggio: **Kotlin**, UI iniziale via **View custom** (`InputMethodService` + custom `View`).
- Build: Gradle + Android Gradle Plugin 8.x (Kotlin DSL `.kts`).

> Nota ambiente: la build/run va fatta in **Android Studio** (SDK + emulatore/dispositivo).
> Questo repo contiene solo i sorgenti; il Gradle wrapper JAR va generato da Android Studio
> o con `gradle wrapper` alla prima apertura.

---

## Fase 0 — Scaffolding progetto (obiettivo: la tastiera compare e si seleziona)

- [x] Struttura Gradle root: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`
- [x] Modulo `app`: `app/build.gradle.kts`
- [x] `AndroidManifest.xml` con dichiarazione `InputMethodService` + permessi minimi (nessuno sensibile)
- [x] `res/xml/method.xml` (metadati IME)
- [x] `T9ImeService.kt` minimale che mostra una view placeholder (`PlaceholderKeyboardView`)
- [x] Risorse base: `strings.xml`, tema, icona placeholder (`ic_launcher.xml`)
- [x] **Milestone:** buildare, installare, abilitare e selezionare la tastiera — ✅ verificata su emulatore Android 15 (2026-07-27): la griglia compare e il predittivo funziona.

## Fase 1 — MVP: funzione centrale (la colonna)

- [~] Layout 12 tasti ITU-T E.161 (griglia custom) + tasti funzione
      → griglia responsive 4×3 fatta (`T9KeyboardView`); tasti funzione minimi (⌫, 0=spazio, ⏎).
        Shift, `*`, `#`, mode-switch arriveranno in Fase 3.
- [x] Inserimento multi-tap (trampolino Step 1.1) — rimpiazzato dal predittivo in 1.2
- [x] `DictionaryEngine` (interfaccia) + `ItalianDictionaryEngine` da asset di test
- [x] Modalità predittiva T9 + barra suggerimenti orizzontale
- [x] **Colonna di disambiguazione manuale posizionale** (stack di coppie cifra/lettera) — `ComposeState` + `DisambiguationColumnView`
- [x] Backspace = pop della coppia (cifra+lettera insieme)
- [x] Estendere/correggere parola (push nuova coppia) — stessa operazione, nessuna distinzione di codice
- [x] Apprendimento persistente (Room) + salvataggio automatico su spazio (Step 1.5)
- [x] Integrazione corpus Leipzig italiano (50k parole, `assets/dict/it.txt`, caricato in background)
      → per ora **testo**, non binario: a 50k parole il parse in background è rapido; il formato
        binario indicizzato resta un'ottimizzazione futura (non necessaria a questa dimensione).
- [x] Test unitari: `T9Keypad.sequenceFor`, `ItalianDictionaryEngine`, `ComposeState`,
      `LearnedWordsEngine`, `MergingDictionaryEngine`, `FuzzyDictionaryEngine`, `SymbolLayout`
- [x] **Step 1.12 — popup long-press sul tasto** (accentate, simboli e cifre, stile Gboard)
      — decisioni e razionale nella sezione "Step 1.12 — piano" più sopra
- [x] **Step 1.8 — pagine simboli/numeri in stile QWERTY** dietro `12#`, su griglia riusabile
- [x] **Step 1.7 — candidati "fuzzy" (tolleranti agli errori)** (idea utente)
      Oltre ai match esatti, proporre parole a **distanza di modifica 1** dalla sequenza
      digitata: togliendo una cifra (tasto premuto in più in mezzo alla parola),
      aggiungendone una (tasto mancante) o cambiandone una (tasto sbagliato).
      **Approccio:** dietro `DictionaryEngine`, dopo i match esatti, generare le varianti
      della sequenza a distanza 1 (n cancellazioni + sostituzioni + inserimenti, ~decine di
      lookup O(1)) e cercarle nell'indice; candidati **penalizzati nel peso** e mostrati in
      coda (o solo se pochi match esatti), per non disturbare la digitazione normale.
      UI/colonna invariate. Da fare dopo l'apprendimento (1.5).

## Fase 2 — Bilingue IT+EN

- [ ] `EnglishDictionaryEngine` + `BilingualDictionaryEngine` (merge candidati)
- [ ] Corpus inglese affiancato al corpus italiano
- [ ] Criteri di accettazione Fase 2 (nessuna regressione sulla v1)

## Fase 3 — Impostazioni, ergonomia, rifiniture

- [ ] Posizione colonna sinistra/destra configurabile
- [x] Simboli preferiti a stack vuoto (configurabili) — anticipati allo Step 1.9
- [ ] Riordino dei preferiti per trascinamento (oggi si riordina scambiandoli, vedi 1.9)
- [ ] Numero di preferiti configurabile (oggi fisso a `FavouriteSymbols.COUNT` = 7)
- [ ] Altezza tastiera regolabile con riproporzionamento uniforme
- [ ] Dimensione del testo dei candidati regolabile (seam già pronto:
      `SuggestionBarView.textSizeSp`)
- [x] Vocali accentate (nella colonna) — anticipate allo Step 1.10
- [x] Pannello emoji base — anticipato allo Step 1.10
- [x] Long-press tasto 1 → simboli costosi altrove — **fatto nello Step 1.12**
- [x] Popup long-press sul tasto con lettere+accentate — **fatto nello Step 1.12**
- [ ] **Scorrimento del cursore trascinando sulla barra spazio** (idea utente): con la
      scrittura predittiva riposizionare il cursore è utile. Occupa il long-press dello
      spazio, oggi libero proprio per questo (lo `0` è già sulla virgola)
- [x] Maiuscola automatica a inizio frase (`getCursorCapsMode`) — fatta nello Step 1.13
- [x] Spazio automatico dopo la scelta di un candidato — fatto nello Step 1.13
- [ ] Interruttori per maiuscola/spazio automatici nella schermata impostazioni
      (le preferenze `autoCapitalise`/`autoSpace` esistono già, manca solo la UI)
- [x] Modalità numerica/simboli dedicata (`12#`, due pagine QWERTY) — anticipata allo Step 1.8
- [ ] **QWERTY come layout alternativo alla T9** (idea utente): un nuovo `KeyGrid` +
      voce in `KeyboardMode`; vista e plumbing già pronti dallo Step 1.8
- [x] Vocali accentate via long-press — **fatto nello Step 1.12**
- [ ] Schermata gestione dizionario personale (lista + cancella)
- [ ] Long-press su candidato → rimuovi dal dizionario
- [ ] Opzione "salvataggio sicuro"
- [ ] Pannello emoji base
- [ ] Verifica layout responsive su S25 e S25 Ultra

---

## 🧭 Step 1.12 — decisioni di piano: popup long-press sul tasto (stile Gboard)

> **Stato: implementato** (vedi la voce di log in fondo). Questa sezione resta come
> registro delle **decisioni e del perché**, che il log riassume soltanto.

**Obiettivo.** Tenendo premuto un tasto compare un popup con i caratteri alternativi
(accentate e simboli), come sulla tastiera Google: si scorre il dito e si rilascia sulla
scelta, oppure si rilascia subito e si tocca la voce.

### La decisione centrale: cosa *significa* scegliere dal popup

In una tastiera normale il popup inserisce un carattere. **Qui non può essere sempre così:**
sui tasti 2–9 le lettere le decide il dizionario, e infilare una `à` grezza a metà
composizione romperebbe l'invariante di `ComposeState` (il campo mostrerebbe una cosa e la
sequenza ne conterrebbe un'altra). Quindi **due semantiche, scelte dal tipo di tasto**:

| Tasto | Popup mostra | Scegliere significa |
|-------|--------------|---------------------|
| `2`–`9` | Le lettere del tasto, accenti inclusi (`a b c à`) | **Forzare quella lettera** in questa posizione: `pressDigit(n)` + `chooseLetter(c)` — esattamente ciò che fa un tap sulla colonna |
| `1`/`@`, `,`, `.`, tasti simbolo | Segni alternativi | `KeyAction.Insert(text)`: conferma la parola in corso e scrive il segno |
| **ultima cella di ogni tasto numerico** | La **cifra** del tasto | `KeyAction.Insert("8")` — vedi sotto |

### Le cifre nei popup (decisione dell'utente)

**Il buco che chiude:** oggi i numeri sono **intypabili senza passare da `12#`**. Sul
tastierino non esiste nessun tasto `0`–`9`: i tasti numerici scrivono lettere e il numerino
d'angolo è solo un'etichetta. Scrivere "alle 8" costa tre gesti in più.

Ogni tasto numerico mette quindi **la propria cifra come ultima cella** del suo popup, con tre
regole:

- **Semantica `Insert`, non `ForceLetter`:** una cifra non è una lettera della parola, quindi
  conferma la composizione in corso e si scrive, come fa già la virgola.
- **Sempre in ultima posizione**, così la posizione non cambia da tasto a tasto.
- **Resa in teal** (`KeyboardTheme.ACCENT`), lo stesso colore del numerino d'angolo che il
  tasto già mostra: si distingue a colpo d'occhio dalle lettere e il legame visivo con
  l'etichetta è immediato.

**Lo `0` sta sul popup della barra spazio.** Non è una scelta estetica: in ITU-T E.161 **`0`
*è* il tasto spazio** — `T9Keypad.letters[0] = [' ']`, già così nel codice. Così la regola
resta una sola e senza eccezioni: *ogni cifra sta sul popup del tasto che è quella cifra*.
Metterla sul `.` sarebbe un'eccezione da ricordare.

> ⚠️ **Conflitto futuro da tenere presente:** se un domani si volesse lo **scorrimento del
> cursore trascinando sulla barra spazio** (fra le cose più comode di Gboard), quel gesto
> occuperebbe il long-press dello spazio e lo `0` andrebbe spostato sul `.`. Da decidere
> allora, non ora.

### I due popup di simboli (decisione dell'utente)

Due popup di simboli con ruoli **complementari e non sovrapposti** — uno curato da te, uno
che si adatta all'uso:

**`.` (riga inferiore, accanto allo spazio) → i 7 simboli preferiti.**
Sembra un doppione della colonna, e invece copre il buco che la colonna lascia: i preferiti
si vedono **solo a riposo**, perché appena componi una parola la colonna diventa lettere. Il
popup su `.` li rende raggiungibili **anche a metà parola**, che è esattamente quando servono
(apostrofo, trattino). Stessa lista, stessa `FavouriteSymbols`, nessun dato nuovo: cambiare un
preferito dalla colonna cambia anche il popup, per costruzione.

**`1` (tastierino) → i simboli che le altre superfici rendono costosi.**

**La regola** (decisa dall'utente, dopo aver scartato sia la punteggiatura storica sia la
frequenza): il popup di `1` non è un elenco arbitrario né un terzo accesso generico ai simboli
— contiene ciò che oggi **costa più di un gesto**:

- **parentesi** → due tasti da premere, e su `12#` sono divise fra pagina 1 (`( )`) e pagina 2
  (`[ ] { }`);
- **valute e matematica** → stanno in **pagina 2**, cioè tre gesti: `12#` → `1/2` → tasto.

Punteggiatura e simboli comuni restano **fuori**, perché sono già a un gesto: `,` e `.` sono
tasti dedicati e `? ! / - ' "` stanno nei preferiti (quindi nella colonna e nel popup di `.`).
Mettere le parentesi fra i preferiti sarebbe costato **due slot su sette** per un solo segno.

**Coppie in una cella.** `()` inserisce **entrambe le parentesi con il cursore in mezzo**: è il
gesto reale (apri, scrivi, chiudi) e riduce quattro azioni a una. Nuova
`KeyAction.InsertPair(open, close)`, implementata con `commitText(open, 1)` +
`commitText(close, 0)` — con `newCursorPosition = 0` il cursore resta **prima** del testo
inserito, quindi nessun calcolo di posizione assoluta né `setSelection`. Collassare le tre
coppie libera lo spazio per valute e matematica. Una parentesi singola resta comunque
disponibile su `12#`: non si perde nulla.

**Contenuto — campo normale** (una riga sola, dopo lo sfoltimento deciso dall'utente):

`@` · `()` · `%` · `+` · `=` · `€` · `$` · **`1`**

- **`@` resta**, benché sia anche il tap del tasto: è il simbolo predefinito del `1`, e vederlo
  nel popup dice che è lì (decisione dell'utente).
- **Fuori quadre, graffe e `£`:** poco usate in una conversazione, e restano su `12#`.
- **Fuori anche `-`:** è **già fra i preferiti di default**, quindi si raggiunge dalla colonna
  e dal popup di `.`. Tenerlo qui violerebbe la regola "i due popup non si sovrappongono".
- **Fuori `×` `÷` `<` `>`:** in chat si scrivono "x" e "/"; restano in pagina 2 di `12#`.
  Rimetterli significa tornare a due righe — scelta reversibile in una riga di dati.

**Contenuto — campo email/URL:** `@ .com .it .net .org gmail.com`. Contestuale, come fanno
Gboard e iOS: il `.com` compare **solo dove serve** e non ingombra mentre scrivi un messaggio.
Il meccanismo è già disponibile senza costo — `onStartInputView` riceve l'`EditorInfo`, basta
guardare la variazione di `inputType` (`TYPE_TEXT_VARIATION_EMAIL_ADDRESS`, `…_URI`).

**Niente conteggio di frequenza** (idea valutata e scartata dall'utente): `12#` dà già tutti i
simboli in un tap e il popup di `.` copre quelli scelti a mano, quindi un terzo accesso
*adattivo* aggiungerebbe persistenza, test e un parametro da tarare in cambio di poco — oltre
al rischio, se mal fatto, di spostare i simboli sotto il pollice. Resta un'eventuale rifinitura
di Fase 3, da decidere con dati d'uso veri anziché per ipotesi.

**Decisione collegata — il tap su `1`.** Oggi il tasto mostra `@` ma toccandolo non scrive
nulla: si limita a confermare la parola. Con il popup sopra, il tap deve **inserire `@`**,
cioè ciò che il tasto mostra: è l'invariante che `SymbolLayoutTest` già protegge sulle pagine
simboli ("un tasto inserisce esattamente ciò che mostra") e violarla proprio qui sarebbe
incoerente. Il tap su `.` resta invariato.

> Nota: `@` è il solo carattere del tasto `1` che il popup non contiene — è sul tasto stesso.
> I caratteri storici del `1` (`. , ? ! '` di `T9Keypad.letters[1]`) restano irraggiungibili
> da qui **per scelta**, perché lo sono già in un gesto altrove; `T9Keypad.letters[1]` resta
> quindi un dato usato solo dal fold delle sequenze, non dalla UI.

Il popup sui tasti lettera diventa così una **scorciatoia posizionale della colonna**, non un
secondo meccanismo: stessa operazione, stesso stato, stesso apprendimento, nessun nuovo
modello mentale da imparare. Ed è il motivo per cui vale la pena farlo anche sui tasti senza
accenti (`5 jkl`, `7 pqrs`, `9 wxyz`): forzare una parola sconosciuta diventa "tieni premuto e
scorri", senza spostare il pollice sulla colonna.

**Ricaduta gratuita:** il tasto `1` oggi si limita a confermare la parola — i suoi caratteri
(`. , ? ! '`) sono **irraggiungibili**. Il popup li rende disponibili, chiudendo un buco
esistente.

### Architettura proposta

1. **Dati (puri, testabili)** — `model/LongPressKeys.kt`: mappa `KeySpec → List<KeySpec>`
   delle alternative, o meglio un campo `alternates: List<KeySpec> = emptyList()` su `KeySpec`
   stesso, così un layout dichiara le sue alternative accanto al tasto. Per i tasti 2–9 le
   alternative si generano da `T9Keypad.columnLetters(n)`, che è già la fonte di verità: il
   popup e la colonna **non possono divergere**.
2. **Azione** — `model/KeyAction.kt`: nuova `data class ForceLetter(val c: Char)`. Oggi la
   colonna passa da una callback dedicata (`onPickLetter`); con `ForceLetter` il popup emette
   `KeyAction` normali attraverso l'`onKey` esistente, senza nuovo plumbing nella view.
3. **Vista** — `ui/KeyPopupView.kt`: una riga di celle disegnate con `KeyViewFactory` (stesso
   aspetto dei tasti, come da principio dello Step 1.8), montata su un **overlay dentro
   `KeyboardView`** e posizionata sopra il tasto d'origine.
   **Perché un overlay e non un `PopupWindow`:** niente token di finestra, niente rischio di
   leak da dimenticata `dismiss()`, e resta tutto dentro una vista che controlliamo. Lo spazio
   c'è: la barra suggerimenti (56dp) fa da tetto per la riga superiore, e sovrapporsi ad essa
   è ciò che fa anche Gboard. Se un giorno servisse uscire dai confini della tastiera, si
   passa a `PopupWindow` senza toccare né i dati né le semantiche.
4. **Gesto** — in `KeyViewFactory`, un handler touch generico accanto a quello del backspace:
   - `ACTION_DOWN` → stato premuto, timer a **400 ms** (la stessa soglia di `HOLD_DELAY_MS`,
     perché due attese diverse per lo stesso gesto si sentono);
   - allo scadere → popup aperto, la cella sotto il dito si evidenzia mentre si scorre;
   - `ACTION_UP` con popup aperto → sceglie la cella sotto il dito (fuori dal popup: annulla);
     senza popup → `performClick()`, cioè il comportamento di oggi, invariato.
   Il click listener attuale resta, così i tasti senza alternative non cambiano di una riga e
   l'accessibilità continua a funzionare.
5. **Servizio** — `T9ImeService`: gestione di `ForceLetter` (`pressDigit` + `chooseLetter` +
   `render`), e chiusura del popup su cambio modalità e `onFinishInput`.

### Dettagli che vanno decisi ora, non a metà implementazione

- **Maiuscole:** le celle del popup devono seguire `ShiftState` come già fanno tasti e colonna
  (§7 di `FUNCTIONAL.md`: ciò che si vede è ciò che si scriverà).
- **Conflitti di gesto:** `⌫` ha già il touch handler (ripetizione) → **escluso** dai popup.
  Il long-press dei preferiti vive nella colonna, che è un'altra vista → nessun conflitto.
- **Annullamento:** rilascio fuori dal popup = nessun inserimento *e* nessun click.

### Contenuto iniziale delle alternative

- `2`→`a b c à` **2** · `3`→`d e f è é` **3** · `4`→`g h i ì` **4** · `5`→`j k l` **5** ·
  `6`→`m n o ò` **6** · `7`→`p q r s` **7** · `8`→`t u v ù` **8** · `9`→`w x y z` **9**
  (in grassetto la cella cifra, sempre ultima e in teal)
- `1`/`@` → `@ () % + = € $` **1**; nei campi email/URL diventa
  `@ .com .it .net .org gmail.com`
- `space` → **`0`** (unica cella: la cifra sta sul tasto che in E.161 *è* lo zero)
- `.` → **i 7 simboli preferiti**, gli stessi della colonna
- `,` → `, ; : "` (fisso)
- Pagine simboli (opzionale, se non allunga troppo lo step): `-`→`– — _`, `(`→`[ {`, ecc.

### Verifica

- **Unit test** (`LongPressKeysTest`), sulla parte pura: "una cella inserisce esattamente ciò
  che mostra" (come `SymbolLayoutTest`, esteso alle coppie: la cella `()` inserisce `(` e `)`
  e nient'altro), "ogni accento di `T9Keypad.accentedLetters` è raggiungibile da un popup",
  "nessun tasto con alternative è anche un tasto a gesto riservato (`⌫`)", e la variante
  email/URL scelta in base all'`inputType`. Sulle cifre: **"ogni cifra 0–9 è raggiungibile da
  esattamente un popup"** (`0` dallo spazio, `1`–`9` dal proprio tasto) — il test che
  garantisce che i numeri non tornino intypabili, e che nessuna cifra finisca in due posti; e
  "la cella cifra è l'ultima del suo popup".
- **Emulatore:** long-press su `3` → `d e f è é`, scorri e rilascia su `è` → composizione
  coerente (la parola prosegue, la sequenza è giusta); long-press su `.` → i preferiti,
  **anche a metà parola**; long-press su `1` → `@ () % + = € $ 1`, e la cella `()` lascia il
  **cursore in mezzo**; in un campo email lo stesso tasto mostra `.com`; **"alle 8" scritto
  senza mai passare da `12#`**, e `0` dal long-press dello spazio; rilascio fuori = niente;
  con `⇧` attivo le celle lettera sono maiuscole (quella cifra no, resta teal). Screenshot in
  `docs/screenshots/step-1.12-*.png`.
- **Documentazione:** nuova sezione in `FUNCTIONAL.md` (popup: le due semantiche e il perché),
  aggiornamento di §4 (la colonna guadagna una scorciatoia) e §7 (accenti raggiungibili in due
  modi), voce di log qui, direttiva permanente rispettata.

### Rischi / punti aperti

- **Il popup non deve rallentare la digitazione normale:** 400 ms sono tanti per un tap, ma se
  durante la prova reale risultasse fastidioso sui tasti lettera, l'alternativa è limitare i
  popup ai tasti non-lettera e lasciare gli accenti alla colonna. Da valutare **sul campo**,
  non a priori.
- **Dita grandi / celle piccole:** con 5 alternative su un tasto stretto le celle vanno più
  larghe del tasto d'origine e il popup va **rientrato** ai bordi dello schermo (i tasti di
  colonna 1 e 3). Riguarda in pieno i popup a 6–7 celle di `1` e `.`.
- **I due popup di simboli non si sovrappongono, per costruzione:** `.` contiene i preferiti
  (punteggiatura di tutti i giorni, scelta da te), `1` contiene ciò che costa più di un gesto
  (parentesi, matematica, valute). Se durante l'uso qualcosa dovesse migrare fra i due, si
  sposta un preferito — nessuna modifica di codice.
- **Popup larghi su tasti di bordo:** `1` (8 celle) e `.` (7 preferiti) sono molto più larghi
  del tasto d'origine, e `1` sta in prima colonna: il **rientro ai bordi dello schermo** va
  verificato proprio lì. Se 8 celle risultassero troppo strette per il pollice, si passa a due
  righe — il popup deve quindi saper andare a capo fin dall'inizio.
- **Semantiche miste in uno stesso popup** (lettere `ForceLetter` + cifra `Insert`): è
  intenzionale e resa visibile dal colore, ma è il punto in cui un bug si nasconderebbe bene.
  Il test "una cella inserisce esattamente ciò che mostra" va scritto in modo da coprire
  entrambe le semantiche, non solo l'inserimento.

---

## 📓 Log di sviluppo (append in fondo, più recente in alto)

<!-- Formato: ### AAAA-MM-GG — titolo step -->
<!-- Cosa fatto, file toccati, note/decisioni, come verificare. -->

### 2026-07-30 — Step 1.12k: i popup da 5 vanno su due righe (3+2) — e i tasti di bordo
**Due segnalazioni dell'utente, una causa sola.** *"3+2 secondo me meglio sull'orizzontale a
5"* e *"sul 6 e sul 9 ho difficoltà a far arrivare il cursore sull'ultima posizione, troppo
al limite dello schermo"*.

**Non sono due problemi.** Con il gesto che parte dalla prima cella (Step 1.12i), l'ultima
cella di una riga da cinque sta **quattro passi di cella** più a destra: ~188dp di pannello,
cioè **~125dp di corsa del dito** al guadagno orizzontale 1.5. I tasti `6` e `9` stanno sulla
colonna destra del tastierino, a ~117dp dal bordo schermo: **la corsa non ci sta**, e il dito
finiva contro il bordo prima di arrivare in fondo. Non era una sensazione, era aritmetica.

**Fatto:** `MAX_PER_ROW` 5 → **4**, quindi i popup da cinque (`2 a b c à`, `6 m n o ò`,
`7 p q r s`, `9 w x y z`, `4`, `8`, e la virgola `0 , ; : "`) diventano **3+2**. La corsa
orizzontale massima si dimezza (~63dp) e la differenza si paga con un passo in giù, dove lo
spazio c'è. La richiesta estetica e il difetto si chiudono con la stessa costante.

**Perché 4 e non 3**, che pure darebbe 3+2: `rows()` bilancia, quindi a 4 un pannello da sei
celle resta 3+3 e uno da otto 4+4, mentre a 3 il popup di `1` (otto celle) passerebbe a **tre
righe**. L'altezza è la dimensione che scarseggia — sulla prima fila di tasti il pannello è
già appoggiato al bordo superiore della tastiera (Step 1.12h).

**File:** `model/LongPressKeys.kt`; test `LongPressKeysTest` (+1 caso: 5 → 3+2).
**Verificato su emulatore (Pixel 10 Pro, Android 17):** il popup di `6` è `6 M N` / `O Ò` e
l'ultima cella `Ò` si raggiunge con un movimento breve in diagonale, senza avvicinarsi al
bordo destro → `docs/screenshots/step-1.12k-tasto-6-tre-piu-due.png`.

### 2026-07-30 — Step 1.12j: anche il `.` preseleziona, niente eccezioni (scelta utente)
**L'utente:** *"io adeguerei il `.` al resto dei popup per coerenza"*.

**Fatto:** la preselezione non è più "la prima cella **se è una cifra**" ma **la prima cella**,
punto. Tenere premuto `.` e rilasciare scrive il **primo preferito**.

**Perché è meglio, e non solo più uniforme.** L'eccezione era difendibile — fra i preferiti
nessuna cella è il default ovvio — ma costava una regola in più da ricordare proprio sul
pannello che l'utente configura da sé. E il primo preferito *è* un default che l'utente ha già
scelto: i preferiti sono ordinati e riordinabili, quindi chi vuole un altro simbolo in testa lo
sposta lì.

**Codice più corto, che è il segno che l'eccezione era di troppo:** `restingIndex` diventa
`if (cells.isEmpty()) -1 else 0` e `anchorPoint()` perde il ramo di riserva "centro della riga
in basso", che ora non ha più chiamanti.

**File:** `ui/KeyPopupView.kt`.
**Verificato su emulatore (Pixel 10 Pro, Android 17):** tenendo premuto `.` la cella `/` è
evidenziata all'apertura e al rilascio senza muovere il dito il campo contiene `/` →
`docs/screenshots/step-1.12j-punto-preselezione.png`,
`step-1.12j-punto-preferito-inserito.png`.

### 2026-07-30 — Step 1.12i: il gesto parte dalla cella preselezionata (scelta utente)
**Chiude il difetto** aperto dallo Step 1.12g e documentato nell'1.12h. L'utente: *"mi ero
accorto del salto ma era più impellente il cambio di guadagno, io andrei con la a"* — cioè
l'origine del gesto si sposta **sulla cella preselezionata**.

**Fatto:** `anchorPoint()` sostituisce il punto fisso "centro della riga in basso". A dito
fermo il puntatore sta **al centro della cella evidenziata**, quindi il primo movimento
**scorre via da lì** invece di teletrasportarsi: su `jkl` un colpetto a destra ora dà `j`,
non più `k`.

**La conseguenza, che è il prezzo della scelta:** la cifra è in alto a sinistra, quindi il
resto del pannello si raggiunge andando a destra e **in giù** — non più in su. Il senso del
gesto verticale si inverte rispetto agli Step 1.12d–h. È coerente (si scende *dentro* il
pannello, verso le celle che stanno visivamente sotto), ma è un'abitudine diversa da quella
di due step fa: se all'uso non convince, l'alternativa scartata era tenere l'origine sulla
riga in basso e rinunciare alla preselezione della cifra.

**L'eccezione resta:** il popup del `.` non ha una cella preselezionata, e lì l'origine
continua a essere il centro della riga in basso — senza niente di selezionato, il dito deve
puntare alla riga che gli è più vicina.

**File:** `ui/KeyPopupView.kt`.
**Verificato su emulatore (Pixel 10 Pro, Android 17):** su `jkl` (`5 j k l`) 33dp a destra
danno **`j`**, la cella adiacente (prima: `k`, scavalcando `j`); su `def` **20dp in giù**
danno **`f`**, la cella sotto al `3` →
`docs/screenshots/step-1.12i-scorrimento-continuo.png`, `step-1.12i-riga-sotto.png`.

### 2026-07-30 — Step 1.12h: taratura del gesto sul popup (feedback utente)
**Segnalazione:** "il movimento lungo l'asse orizzontale ora è troppo accentuato, mentre
sull'asse verticale il dito è ancora troppo sopra il popup". Chiesto chiarimento sul secondo
punto, perché ammetteva letture opposte; risposta: *"sposta il pannello leggermente più sopra
e aumenta un po' il guadagno senza esagerare"*.

**Fatto — 1. guadagni separati per asse.** `POINTER_GAIN` si spacca in `HORIZONTAL_GAIN`
**1.5** e `VERTICAL_GAIN` **2.5**. Un valore solo era una semplificazione sbagliata: i due
assi hanno problemi diversi. Di lato le distanze sono brevi (una cella) e troppo guadagno
rende l'evidenziazione nervosa; in su il dito deve coprire un passo di riga **e** restare
fuori da un pannello che gli sta appena sopra. Riga sopra ~20dp, estremità di una riga da 5
~59dp.

**2. Il pannello si stacca dal tasto di 10dp** (`POPUP_GAP_DP`, era 2): ogni dp è spazio
sottratto alla mano.

**Il limite da sapere, perché non è aggirabile con una costante:** sulla **prima fila di
tasti** il pannello **non può salire**. È già appoggiato al bordo superiore della tastiera —
la finestra dell'IME è tutto lo spazio disponibile, e un pannello a due righe (~108dp) non
entra sopra un tasto che dista 61dp dal bordo. Su quella fila (`abc`, `def`) il dito resta
sotto al pannello comunque, e a compensare è solo il guadagno verticale. Alzarlo davvero
richiederebbe un `PopupWindow` che esce dalla finestra dell'IME, cioè disfare la scelta
architetturale dello Step 1.12 (nessun token di finestra, niente che sopravviva alla tastiera).

**3. Difetto trovato verificando, non segnalato dall'utente.** Vedi il punto 🐞 in STATO
CORRENTE: preselezione e origine geometrica sono in due posti diversi, quindi il primo
movimento fa **saltare** la selezione. Non l'ho risolto qui perché le due soluzioni possibili
si escludono a vicenda e una delle due rinuncia alla scorciatoia appena chiesta: è una
decisione, non un dettaglio implementativo.

**File:** `ui/KeyPopupView.kt`, `ui/KeyboardView.kt`.
**Verificato su emulatore (Pixel 10 Pro, Android 17):** **16dp** in su bastano ora per la riga
sopra (`d` evidenziata); il pannello di `jkl` mostra lo stacco dal tasto →
`docs/screenshots/step-1.12h-verticale-16dp.png`, `step-1.12h-stacco-dal-tasto.png`.
**Da tarare sul telefono:** i due guadagni sono stimati sulla geometria dell'emulatore; sono
due costanti in `KeyPopupView`, indipendenti fra loro.

### 2026-07-30 — Step 1.12g: la cifra è preselezionata all'apertura (richiesta utente)
**Richiesta:** "avendo messo all'inizio il numero vorrei che appena aperto il popup il cursore
sia già sul numero, così se non sposto viene inserito direttamente il numero".

**Fatto:** a dito fermo la selezione non è più "niente" ma la **prima cella**, quando è la
cifra. Tenere premuto un tasto numerico e rilasciare **scrive il suo numero**, senza mirare —
il percorso più corto possibile su un tastierino che non ha tasti 0–9. Il pannello si apre già
con la cifra evidenziata, quindi il comportamento si vede *prima* di produrlo.

**Ribalta una decisione dello Step 1.12d**, e va detto: lì "a dito immobile nessuna cella è
selezionata" era la via d'uscita — apri il pannello, ci ripensi, rilasci. Ora per annullare si
scorre via dal pannello e si rilascia dove non è selezionato nulla. È il prezzo della
scorciatoia, pagato consapevolmente; la documentazione lo dice invece di far finta di niente.

**L'eccezione:** il popup del `.` è di soli **preferiti**, senza cifra. Lì a riposo resta
selezionato **niente**: nessuna cella è il default ovvio e sceglierne una a caso la
scriverebbe di sorpresa. Il pannello riconosce la cella cifra da `isFunction`, che già la
marcava per colorarla di teal — nessun concetto nuovo, e `KeyPopupView` continua a non sapere
cosa sia una cifra.

**Un dettaglio:** l'evidenziazione iniziale si applica **solo quando il pannello compare**
(`opening` in `positionPopup`), non a ogni layout: un re-layout a metà gesto butterebbe via la
cella su cui il dito è nel frattempo scivolato.

**File:** `ui/KeyPopupView.kt` (`restingIndex`, `highlightResting`), `ui/KeyboardView.kt`.
**Verificato su emulatore (Pixel 10 Pro, Android 17):** pressione prolungata su `def` senza
muovere il dito → `3` evidenziato all'apertura e **`3` scritto nel campo** al rilascio; sul
popup del `.` a dito fermo nessuna cella evidenziata →
`docs/screenshots/step-1.12g-cifra-preselezionata.png`,
`step-1.12g-cifra-inserita-senza-mirare.png`, `step-1.12g-preferiti-nessuna-preselezione.png`.

### 2026-07-30 — Step 1.12f: guadagno anche in orizzontale, cifra in testa (feedback utente)
**Tre punti dell'utente**, uno dei quali volutamente **non** implementato.

**1. Anche l'orizzontale va amplificato.** Restava 1:1, e su una riga da 5 celle l'estremità
sta a due larghezze di cella dal centro: ~88dp, quasi due tasti di corsa. Il guadagno ×2 dello
Step 1.12e vale ora su **entrambi gli assi** (`VERTICAL_GAIN` → `POINTER_GAIN`), quindi
l'estremità è a ~44dp: tutto il pannello sta dentro una larghezza di tasto.

Perché in orizzontale serviva anche un perno, che prima non c'era: la x era **assoluta**
(`pointer.x = rawX`), il che funzionava solo perché il pannello è centrato sul tasto — ma
**non** per i tasti di bordo, il cui pannello viene rientrato nello schermo e quindi non è più
centrato su di loro. Ora la x parte dal centro del pannello e si misura da `originX`, come la
y: stesso gesto ovunque, bordo compreso.

`REACH_X_DP` 32 → 60 di conseguenza: è tolleranza in coordinate del pannello, quindi il
guadagno ne dimezza il costo per il dito, e "scorrere oltre il fondo della riga prende
comunque l'ultima cella" doveva restare vero. Essere generosi lì è sicuro: la tolleranza
decide solo *se* qualcosa è selezionato, mai quale cella vince — quella è sempre la più vicina.

**2. La cifra come prima cella, non ultima** (richiesta esplicita). Ha anche una ragione
strutturale: un pannello sta su una riga o su due, e solo la cella d'apertura significa la
stessa cosa in entrambi i casi — l'ultima passa da "fine della riga" a "in basso a destra"
appena la lista va a capo. Vale per tutti: `3 d e f è é`, `1 @ () / % + = €`, `0 , ; : "`.

**3. Le 5 celle su una riga: non toccate, per ora.** L'utente le preferirebbe 3+2 ma sospetta
che il fastidio venga dal punto 1 — quindi prima si prova il guadagno. Se non basta è una
costante: `MAX_PER_ROW` = 3, e `rows()` bilancia già (5 → 3+2).

**File:** `ui/KeyPopupView.kt`, `model/LongPressKeys.kt`; test `LongPressKeysTest` (la cifra
è la prima cella, virgola inclusa, e il campo email).
**Verificato su emulatore (Pixel 10 Pro, Android 17):** test verdi; popup di `def` = `3 d e` /
`f è é` con il `3` teal in testa; **20dp** di scorrimento laterale arrivano a `é`, l'ultima
cella (col codice precedente si fermavano su `è`, quella centrale); sul popup da 5 celle di
`abc`, **44dp** a sinistra raggiungono il `2` all'estremità →
`docs/screenshots/step-1.12f-cifra-in-testa-e-orizzontale.png`,
`step-1.12f-riga-da-5-estremita.png`.

### 2026-07-30 — Step 1.12e: la riga sopra del popup si raggiunge con un guizzo (feedback utente)
**Segnalazione dell'utente:** "lo spostamento in orizzontale sulla stessa riga funziona bene,
per arrivare alla riga sopra devo praticamente arrivare con il dito sulla riga".

**La causa.** L'inseguimento verticale era **1:1**: cambiare riga costava mezzo passo di riga
di corsa e centrarla uno intero, cioè ~50dp, all'incirca **l'altezza di un tasto**. Il dito
finiva quindi sul pannello — esattamente ciò che lo Step 1.12d aveva costruito il tracking
sfalsato per evitare. In orizzontale il problema non si pone perché una cella è larga ~44dp ma
il gesto è una spazzata laterale, dove quella distanza è naturale.

**Fatto: l'asse verticale è amplificato ×2** (`VERTICAL_GAIN`), l'orizzontale no. Nessuna
soglia a scatti: la mappatura resta continua, solo con pendenza doppia, così l'evidenziazione
continua a seguire il dito invece di saltare. Ora la riga sopra è a ~25dp.

**Il secondo difetto, che emerge solo amplificando.** Il perno era il **centro del tasto**: con
guadagno 1 premere 15dp sopra il centro era innocuo (restava dentro la tolleranza della riga in
basso), con guadagno 2 diventano 30dp e il pannello si sarebbe aperto con la riga *sopra* già
selezionata. Lo zero ora è **`originY`**, dov'era il dito all'apertura: dove dentro il tasto è
caduta la pressione non decide più quale riga parte selezionata. Come effetto collaterale
`anchorCenterY` non serve più e `setTracking` perde un parametro — il pannello si misura da sé.

**File:** `ui/KeyPopupView.kt`, `ui/KeyboardView.kt` (call site più corto).
**Verificato su emulatore (Pixel 10 Pro, Android 17):** test verdi; long-press su `def` e
**20dp** di corsa verso l'alto (meno di metà tasto, il dito non lascia il tastierino) →
si illumina `e` sulla **riga sopra**; con il codice precedente a 20dp sarebbe rimasta
selezionata la riga in basso. Scorrimento solo orizzontale → `3`, riga in basso, invariato →
`docs/screenshots/step-1.12e-riga-sopra-con-poco-movimento.png`,
`step-1.12e-orizzontale-invariato.png`.

### 2026-07-29 — Step 1.17: un dizionario da messaggi, non da giornale
**Domanda dell'utente:** "non esiste un dizionario da messaggio?" Sì, e il corpus
giornalistico era il limite vero — non il taglio a 50k, la cui coda è già fatta di parole
viste **una volta sola**.

**Quanto era sbagliato il registro, in numeri:** `ciao` compare **27** volte nel corpus
giornalistico e **225.358** nei sottotitoli di film e serie; `beh` 47 contro 415.077; `ok`
27 contro 513.655. Parole che in chat si scrivono ogni giorno stavano in fondo alla classifica.

**Fatto:** `tools/BuildDictionary.java` (sostituisce `ConvertLeipzig.java`) fonde **due
corpora**, perché nessuno dei due da solo basta:
- **OpenSubtitles** (hermitdave/FrequencyWords, CC BY-SA 4.0), peso **70%** — il parlato;
- **Leipzig news** (CC BY-4.0), peso **30%** — il vocabolario che ai dialoghi manca
  (istituzioni, geografia, registro formale) e, unico dei due, **le maiuscole**: resta la
  fonte delle prove per i 442 nomi propri, che i sottotitoli non potrebbero dare essendo
  tutti minuscoli.

Le due scale non sono confrontabili (milioni di token contro migliaia), quindi ciascuna è
convertita in **occorrenze per milione** prima della fusione.

**Effetto:** `ciao` 27→660, `beh` 47→1215, `ok` 27→1498, `cavolo` 5→134. Le parole da
giornale scendono ma **restano** (`presidente` 1381→339, `comunale` 284→50): non sparisce
nulla, cambia chi vince.

**Verificata la taratura delle soglie**, che erano calibrate sulle frequenze grezze: con i
nuovi pesi le lettere che sono parole restano sopra i 1000 di `SingleLetterEngine`
(`e` 29308, `o` 1906) e il rumore sotto (`d` 25, `q` 9); il peso massimo (29.311) resta
lontanissimo dal milione con cui `LearnedWordsEngine` marca le parole imparate.

**File:** `tools/BuildDictionary.java` (nuovo), `tools/ConvertLeipzig.java` (rimosso),
`assets/dict/it.txt` (ricostruito, 545 KB).
**Verificato su emulatore:** `2426` ora predice **"ciao"** come prima scelta →
`docs/screenshots/step-1.17-dizionario-da-chat.png`.
**Margine ulteriore:** Leipzig ha edizioni più grandi (`ita-it_web-public_2019_1M`,
`ita_wikipedia_2021_1M`, ~240 MB l'una) che migliorerebbero copertura **e** statistica dei
nomi propri. Da valutare dopo la prova sul campo, sapendo quali parole mancano davvero.

### 2026-07-29 — Step 1.16: i nomi propri escono dai dati, non da una lista
**Fatto:** riscaricato il corpus Leipzig (26 MB) e **rigenerato il dizionario** conservando
l'informazione che la conversione buttava via.

**Come.** `ConvertLeipzig` ora conta, per ogni parola, la **quota di occorrenze maiuscole**
*prima* di unire le varianti. Sopra il **90%** (e almeno 30 occorrenze, sotto le quali la
percentuale è rumore) la parola prende il flag `P` in `it.txt`. Sono **442 nomi propri**, e
`ProperNouns` non contiene più nulla di scritto a mano: riceve l'insieme dal corpus quando il
caricamento finisce.

**La misura fa meglio della lista, e si vede dove:**

| Parola | Maiuscole | Esito |
|---|---|---|
| `roma` | 99% (831 occ.) | **Roma** |
| `milano`, `italia`, `pasqua` | 100% | maiuscole |
| `rosa` 17% · `viola` 27% · `bianca` 44% | sotto soglia | fiore, colore, aggettivo — **salvi** |
| `prato` 72% · `camera` 65% · `nord` 68% | sotto soglia | nomi comuni |
| `marzo` 5% · `domenica`, `lunedì` | bassissime | mesi e giorni minuscoli **senza eccezioni** |

Le maiuscole di inizio frase gonfiano ogni parola ma arrivano al massimo al ~20% (`il` 18%,
`quando` 19%): la soglia al 90% le ignora senza fatica. E `marco` e `luca`, al 100%, entrano —
nell'italiano di oggi la moneta e l'evangelista non competono più con i nomi.

**File:** `tools/ConvertLeipzig.java` (conteggio maiuscole + flag `P`),
`assets/dict/it.txt` (rigenerato, 564 KB), `engine/ItalianDictionaryEngine.kt`
(espone `properNouns`), `input/ProperNouns.kt` (lista scritta a mano **rimossa**),
`service/T9ImeService.kt`; test `ProperNounsTest` riscritto sul formato del dizionario.
**Verificato su emulatore:** nelle note `6-4-5-2-6-6` dà **"Milano"** maiuscolo nel campo e
nella barra, mentre nel campo Email lo stesso testo resta `casa.milano` — nomi propri e
blacklist convivono → `docs/screenshots/step-1.16-nome-proprio-dal-corpus.png`.
**Nota:** l'insieme arriva con il corpus, quindi nel primo istante dopo l'avvio nessuna parola
è capitalizzata da sola. È il modo innocuo di sbagliare.

### 2026-07-29 — Step 1.15: le regole italiane di maiuscole e spaziatura
**Fatto:** implementata la specifica dell'utente (4 sezioni: maiuscole, spazi, blacklist dei
campi, annullamento col backspace) sopra la base dello Step 1.13.

**Prima di tutto la blacklist** (`FieldRules`), perché è quella che evita danni: fuori dal
testo semplice — email, URL, password, numeri, telefono, date, campi che chiedono nessun
suggerimento — **entrambi gli automatismi tacciono**. Senza, le altre regole peggiorerebbero
gli indirizzi invece di migliorare la prosa.

**Maiuscole** (`SentenceRules`): due cose che `getCursorCapsMode` non può sapere.
Le **abbreviazioni** (`ecc. dott. pag. p.v.`), confrontate sul token prima del punto così che
il punto interno di `p.v.` non rompa il riconoscimento; e le **virgolette/parentesi di
apertura** dopo una fine frase, dove la maiuscola appartiene alla parola dentro.
`vedi` non ha punto: in lista ci sono `v.` e `vd.`, che sono le forme che il punto ce l'hanno.

**Spazi** (`AutoSpace` esteso): aperture con lo spazio prima e non dopo, chiusure il contrario,
apostrofo che non prende spazio da nessuna parte, `...` trattati come un segno solo (il secondo
punto niente, il terzo sì). La guardia "solo dopo una lettera" **resta**, ed è ciò che tiene
interi `3,14` e `10:30`: la blacklist non li coprirebbe, perché un orario si scrive dentro un
messaggio, in un campo di testo normale.

**Le virgolette dritte** sono l'unico simbolo che apre *e* chiude, quindi il ruolo si legge dal
testo: dopo una lettera o una cifra possono solo chiudere. Il carattere inserito resta però
quello digitato — il ruolo serve alle regole di spaziatura, non a sostituirlo con le
tipografiche.

**Nomi propri** (`ProperNouns`): lista **volutamente stretta**. Il corpus non può aiutare
(`ConvertLeipzig` minuscola e somma le varianti, quindi "Roma" è andata perduta): la strada
giusta è rigenerarlo tenendo la quota di occorrenze maiuscole, ed è quella pianificata.
Intanto solo nomi che non sono anche parole comuni, e **niente nomi di persona**: Rosa, Viola,
Bianca, Vera e Marco metterebbero la maiuscola a un fiore, un colore e una moneta. Una
maiuscola sbagliata dà più fastidio di una mancante.

**Backspace (§4), con una modifica alla specifica.** Annullare la maiuscola *senza cancellare*
rende la pressione invisibile e sembra un tocco perso. Quindi il backspace **cancella comunque**
e in più disinnesca la maiuscola automatica. Lo spazio automatico non ha bisogno di nulla: è
l'ultimo carattere, quindi la cancellazione normale toglie esattamente lui.

**File:** `input/FieldRules.kt`, `input/SentenceRules.kt`, `input/ProperNouns.kt` (nuovi),
`input/AutoSpace.kt` (riscritto), `service/T9ImeService.kt`; test `FieldRulesTest`,
`SentenceRulesTest`, `ProperNounsTest`, `AutoSpaceTest` (riscritto) — 30 casi in tutto.
**Verificato su emulatore:** nel campo **Email** `casa.` resta minuscolo e senza spazio dopo
il punto (blacklist attiva); nelle note `7-6-6-2` dà **"Roma"** maiuscola con lo shift spento →
`docs/screenshots/step-1.15-campo-email-senza-aiuti.png`, `step-1.15-nome-proprio.png`.
**Da rivedere sul telefono:** le abbreviazioni e i puntini di sospensione sono coperti dai test
ma non provati a mano — svuotare il campo via adb si è rivelato inaffidabile.

### 2026-07-29 — Step 1.14: un tasto solo si comporta da tasto, non da parola
**Segnalazione dell'utente:** scrivendo una sola lettera esce l'accentata, e l'ordine dei
candidati di un tasto singolo sembra strano.

**Due cause, entrambe reali.**

1. **I pesi del corpus.** Per il tasto `3` il dizionario ha `e 48146`, `è 27734`, `é 82`,
   `d 37`, `f 9`: ordinando per frequenza le accentate finiscono **davanti alle altre
   lettere del tasto**. Peggio, `q` non compare mai da sola nel corpus e quindi **spariva
   del tutto** dal tasto 7.
2. **L'apprendimento.** Una parola imparata pesa ≥ 1.000.000 e batte tutto il corpus: basta
   scrivere `è` **una volta** perché diventi per sempre la prima scelta del tasto `3`. È
   questa la ragione per cui la tastiera "prendeva in automatico l'accentata".

**Fatto.**
- `SingleLetterEngine` (decoratore, il più esterno): per una cifra sola la lista è
  **ricostruita dal tastierino** — ci sono tutte le lettere del tasto, quelle che sono parole
  vere (oltre le mille occorrenze) vengono prima, e un accento non precede mai la sua lettera
  semplice. Le sequenze più lunghe non vengono toccate: lì la frequenza è la risposta giusta.
- **Le lettere singole non si imparano più** (`learn()` ignora le parole di un carattere):
  un danno permanente sulla pressione di tasto più comune che esista, in cambio di
  un'informazione che non serve — l'ordine di un tasto solo lo decide la regola, non la
  cronologia.

**File:** `engine/SingleLetterEngine.kt` (nuovo), `service/T9ImeService.kt`;
test `engine/SingleLetterEngineTest.kt` (7 casi, fra cui "ogni lettera del tasto è offerta"
e "un accento imparato non scavalca la lettera semplice").
**Verificato su emulatore:** `3` → **`e è d f é`** (accentata seconda, come richiesto),
`7` → **`p q r s`** nell'ordine del tasto, con la `q` finalmente presente →
`docs/screenshots/step-1.14-tasto-singolo-3.png`, `step-1.14-tasto-singolo-7.png`.
**Nota:** nel primo istante dopo l'avvio, con il corpus ancora in caricamento, un tasto solo
mostra il semplice ordine del tastierino (nessun peso è ancora noto). Si sistema da sé.

### 2026-07-29 — Step 1.13: maiuscola automatica e spazio automatico
**Fatto:** (richiesta dell'utente) i due aiuti alla scrittura che mancavano, che poi si
sostengono a vicenda — la maiuscola serve a inizio frase, e lo spazio automatico è ciò che
crea quel confine di frase. Entrambi hanno già la preferenza (`autoCapitalise`, `autoSpace`,
accese di default) pronta per la schermata di Fase 3.

**Maiuscola.** La domanda "qui ci va?" la risponde **Android**, non noi:
`getCursorCapsMode(inputType)`. Copre senza casi speciali inizio campo, inizio riga e la
parola dopo `.`/`!`/`?`, e **gratis** i campi che chiedono ogni parola maiuscola (il nome in
rubrica) o tutto maiuscolo. Ricalcolata all'apertura del campo, alla conferma di una parola,
in cancellazione (tornare oltre un punto rimette la maiuscola) e allo spostamento del cursore.

**La parte delicata è quando *non* toccarla.** Spegnere `⇧` a inizio frase è deliberato, e
riaccenderla subito dopo significa litigare con chi scrive. Regola di **proprietà**
(`AutoShift.resolve`, puro e testato): la tastiera cambia solo ciò che ha impostato lei; uno
stato scelto dall'utente vale fino alla conferma di quella parola, poi si ridecide.

**Spazio.** Scegliere un candidato inserisce anche lo spazio dopo (solo lì: spazio e invio
sono già un separatore, raddoppiarlo sarebbe un peggioramento). Lo spazio è **provvisorio**,
ed è questo a decidere se la funzione piace: senza regola, scegliere "casa" e digitare un
punto lascerebbe `casa .`. Quindi la punteggiatura che sta attaccata alla parola precedente si
**riprende** lo spazio, e se chiude una frase ne mette uno **dopo** — dove poi cade la
maiuscola. Un punto porta lo spazio **solo dopo una lettera**, il che tiene interi `3.14` e
`www.sito.it`.

**Bug trovato provando (e la sua morale).** Il primo tentativo dava `Cara .`: azzeravo il
flag dello spazio in `onUpdateSelection`, dove però arrivano **anche le nostre modifiche** —
`commitText(" ")` disfaceva la funzione un istante dopo che aveva agito. La correzione non è
inseguire il cursore ma **fidarsi del campo invece che del flag**: prima di cancellare si
verifica che davanti al cursore ci sia davvero uno spazio. Così un flag stantio (cursore
spostato, testo riscritto dall'app) non può mangiare un carattere scritto dall'utente.

**File:** `input/AutoShift.kt`, `input/AutoSpace.kt` (nuovi, puri),
`settings/KeyboardSettings.kt` (due preferenze), `service/T9ImeService.kt`;
test `input/AutoShiftTest.kt` (6 casi) e `input/AutoSpaceTest.kt` (6 casi).
**Verificato su emulatore:** a campo vuoto `2272` predice **"Casa"** con la colonna `A B C À`;
scegliendo un candidato compare lo spazio; premendo `.` subito dopo si ottiene **`Basa. `** —
spazio ripreso, punto attaccato, spazio nuovo dopo — e i tasti passano a `ABC/DEF` per la
frase successiva → `docs/screenshots/step-1.13-maiuscola-automatica.png`,
`step-1.13-spazio-e-punto.png`.
**Non incluso:** spazio automatico dopo le emoji (Gboard non lo fa) e dopo i simboli.

### 2026-07-29 — Step 1.12d: il dito non copre più il popup (feedback utente)
**Il problema:** per scegliere bisognava portare il dito **sul** pannello, che è
esattamente ciò che si sta cercando di leggere. Gboard non lo fa: il dito resta sulla
tastiera e la selezione lo segue più in alto.

**Fatto:** il punto usato per la scelta non è più il dito ma la sua **traduzione dentro
il pannello** (`KeyPopupView.pointerInPanel`): stessa distanza a destra o a sinistra, ma
sollevata quel tanto che basta perché il dito **fermo sul tasto punti alla riga in
basso**, e salire di una cella raggiunga la riga sopra. Lo scarto si calcola dalla
geometria reale (centro del tasto meno centro dell'ultima riga), quindi resta giusto
anche quando il pannello viene rientrato in alto perché non ci sta.

**Serviva anche l'origine del gesto.** Il pannello si apre 400 ms **dopo** la pressione,
quindi il gesto ricorda l'ultima posizione nota del dito e la passa a `showPopup`: è lo
zero da cui si misura lo spostamento. Finché il dito non si è mosso davvero (10dp) non è
selezionato nulla, così aprire il pannello e rilasciare senza spostarsi resta un modo per
cambiare idea invece di scrivere il carattere che capita sopra al dito.

**File:** `ui/KeyPopupView.kt`, `ui/KeyViewFactory.kt` (`PopupHost.showPopup` ora riceve
l'origine), `ui/KeyboardView.kt` (passa il centro del tasto sullo schermo).
**Verificato su emulatore:** dito fermo sul tasto `def`, spostato solo di lato → si
illuminano `3` e poi `è` **sopra** al dito, che non entra mai nel pannello; rilasciando
scrive `è`. A dito immobile nessuna cella è selezionata →
`docs/screenshots/step-1.12d-tracking-sfalsato.png`,
`step-1.12d-nessuna-scelta-a-riposo.png`.

### 2026-07-29 — Step 1.12c: la cella selezionata si vede (feedback utente)
**Bug, non scelta di design.** La cella sotto il dito cambiava colore in modo
impercettibile: costruendola avevo passato solo il colore normale, lasciando quello
"premuto" di default — tarato sui tasti scuri (`KEY_PRESSED #4A515C`) e quindi quasi
identico allo sfondo più chiaro del pannello (`POPUP_BG #454B57`).

**Fatto:** la cella selezionata ora si **riempie di teal** (`ACCENT`) con il glifo
invertito allo scuro del tema, come fa Gboard. Non un pallino, che coprirebbe o farebbe
concorrenza al carattere proprio nel momento in cui lo devi leggere. L'inversione del
glifo non è un vezzo: la cella cifra è **già** teal, e senza inversione sparirebbe dentro
il proprio evidenziatore.

**File:** `ui/KeyPopupView.kt`.
**Verificato su emulatore:** `è` e la cifra `3` evidenziate e leggibili →
`docs/screenshots/step-1.12c-selezione-evidente.png`, `step-1.12c-selezione-cifra.png`.

### 2026-07-29 — Step 1.12b: popup su due righe (feedback dagli screenshot)
**Fatto:** (segnalazione dell'utente sugli screenshot dello Step 1.12) i popup lunghi
erano troppo larghi — quello del `1` con 8 celle copriva l'**84% della larghezza schermo**.
Ora oltre **5 celle** il pannello va a capo: `LongPressKeys.MAX_PER_ROW` 8 → 5, con
`rows()` che bilancia lo spezzone (6 celle → 3+3, non 5+1). Cinque e non quattro perché
tiene su una riga sola i popup più frequenti (`a b c à 2`, `p q r s 7`), dove il gesto è
una singola spazzata orizzontale, e spezza solo quelli lunghi. Le righe sono **centrate**,
così un pannello con celle di larghezza diversa (`.com` accanto a `@`) resta una griglia.

**Il bug che la richiesta ha scoperto.** `KeyPopupView.indexAt` cercava la prima cella che
*contiene* il dito, allargata di 28dp in verticale perché il dito sta sotto al pannello
mentre scorre. Con due righe quella tolleranza copre la riga successiva: due celle
rispondono allo stesso punto e vinceva sempre quella visitata per prima, cioè la riga in
alto — **la seconda riga sarebbe stata inselezionabile**. Ora si prende la cella **più
vicina**: un punto fra due righe appartiene alla più prossima, senza ambiguità. La
tolleranza è diventata asimmetrica (32dp di lato, 18dp in verticale): larga per lo
scorrimento oltre il fondo della riga, stretta in basso perché il dito che ha aperto il
pannello poggia sul tasto sottostante, e un rilascio senza spostarsi deve voler dire
"lascia perdere", non un carattere a caso.

**File:** `model/LongPressKeys.kt`, `ui/KeyPopupView.kt`; test `LongPressKeysTest`
(+2 casi: lo spezzone è bilanciato, e nessun popup supera il massimo per riga).
**Verificato su emulatore:** `1` → due righe da 4 (dall'84% al ~43% della larghezza),
`3` → `d e f` / `è é 3`; **selezionata `è` sulla seconda riga**, che è esattamente il caso
che il vecchio hit-testing sbagliava → `docs/screenshots/step-1.12b-seconda-riga.png`,
`step-1.12b-seconda-riga-scelta.png`.

### 2026-07-29 — Step 1.12: popup long-press (accentate, simboli e **cifre**)
**Fatto:** tenendo premuto un tasto si apre un pannello di alternative; si scorre il dito e
si rilascia sulla scelta. Le decisioni e il loro perché stanno nella sezione "Step 1.12 —
decisioni di piano" più sopra; qui cosa è stato costruito.

**Due semantiche in un solo meccanismo.** Sui tasti 2–9 una cella lettera **forza** quella
lettera (`KeyAction.ForceLetter`) — la stessa operazione del tap sulla colonna, quindi il
popup è una sua scorciatoia e non un secondo modello da imparare; tutto il resto
**inserisce**. Le lettere vengono da `T9Keypad.columnLetters`, la fonte di verità che la
colonna già usa: popup e colonna non possono divergere.

**Le cifre.** Ogni tasto numerico offre la propria cifra come **ultima** cella, in teal come
il numerino d'angolo che rappresenta. Chiude un buco reale: il tastierino non ha tasti 0–9,
quindi prima un numero si scriveva solo passando da `12#`. Lo `0` sta sul popup della
virgola — la barra spazio sarebbe la sua casa in E.161, ma quel long-press è **riservato allo
scorrimento del cursore** (Fase 3) — e il tasto `,` ora mostra `0` nell'angolo come gli altri.

**Correttezza, il punto meno ovvio:** forzare una lettera dal popup quando restano posizioni
non risolte risolverebbe la *prima* di esse, non l'ultima, trasformando la parola in
tutt'altro. `resolvePendingFromPreview()` chiude prima le posizioni aperte **con ciò che il
campo sta già mostrando**, così la parola non cambia sotto le mani dell'utente.

**Perché una vista figlia e non un `PopupWindow`:** il pannello non riceve mai eventi propri
— il dito appartiene al tasto, che gli inoltra le coordinate — quindi non serve una finestra:
niente token, niente `dismiss()` dimenticata che sopravvive alla tastiera. `KeyboardView` è
diventata un `FrameLayout` con il corpo tastiera come figlio e il popup come fratello sopra.

**Altro:** `KeyAction.InsertPair` inserisce `()` col **cursore in mezzo**
(`commitText(close, 0)`: posizione non positiva = misurata dall'inizio del testo inserito,
quindi nessun `setSelection`); il popup di `1` diventa `@ .com .it .net .org /` nei campi
email/URL (`inputType` letto in `onStartInputView`); il tap su `1` ora **scrive `@`**, ciò che
il tasto mostra; le celle lettera seguono le maiuscole, la cella cifra no.

**File:** `model/LongPressKeys.kt`, `ui/KeyPopupView.kt` (nuovi), `model/KeyAction.kt`
(`ForceLetter`, `InsertPair`), `model/T9Keypad.kt` (`0` sulla virgola), `ui/KeyViewFactory.kt`
(`PopupHost` + gesto), `ui/KeyboardView.kt` (FrameLayout + posizionamento), `ui/KeyboardTheme.kt`
(`POPUP_BG`), `service/T9ImeService.kt`; test `model/LongPressKeysTest.kt` (11 casi, fra cui
**"ogni cifra 0–9 è raggiungibile da esattamente un popup"**, che impedisce sia il ritorno del
buco sia una cifra offerta da due posti).

**Verificato su emulatore (Pixel, Android 17):** test verdi; `3` → `d e f è é 3` con la cella
sotto il dito evidenziata e `è` inserita coerentemente; `1` → `@ () / % + = € 1`, e in un
campo email → `@ .com .it .net .org / 1`; `,` → `, ; : " 0`; `.` → i preferiti, gli stessi
della colonna; **"alle 8" scritto senza mai passare da `12#`**; `()` seguito da una lettera dà
`(a)`; con `⇧` le celle sono `A B C À` e la cifra resta teal; rilascio sul tasto = nessun
inserimento. Screenshot `docs/screenshots/step-1.12-*.png`.

**Non incluso:** conteggio di frequenza dei simboli (valutato e scartato: `12#` dà già tutti i
simboli in un tap e il popup di `.` copre quelli scelti a mano); popup sulle pagine simboli
oltre a `,` e `.`.

### 2026-07-29 — Step 1.11: cancella tenendo premuto + maiuscole visibili (feedback dalla prima prova reale)
Due riscontri dell'utente dopo la prima prova sul campo.

**1. Il cancella era poco pratico.** Ora `⌫` **ripete tenendolo premuto e accelera**:
dopo 400 ms (abbastanza perché un tocco normale non lo inneschi) cancella un carattere
ogni 55 ms, e dopo 10 caratteri passa a **parole intere** ogni 140 ms — svuotare una
frase non richiede più venti tocchi. Nuova azione `KeyAction.DeleteWord`, che non sta su
nessun tasto: è ciò in cui il tenere premuto si trasforma. Cancella la parola in corso di
composizione se c'è, altrimenti la parola prima del cursore **spazi finali inclusi**, così
la seconda ripetizione non si limita a mangiare lo spazio lasciato dalla prima.
Il tasto è gestito **interamente a eventi touch** (niente click listener), così un tocco
singolo cancella esattamente una volta; lo stato "premuto" è pilotato a mano perché gli
eventi vengono consumati.

**2. Maiuscole invisibili.** Colonna e tasti restavano sempre nello stesso caso
indipendentemente da `⇧`. Ora **mostrano ciò che scriveranno davvero**:
`ShiftState.appliesToNext(atWordStart)` decide, e il servizio lo chiama **due volte**
con posizioni diverse — i tasti scrivono il carattere dopo l'ultima cifra premuta
(`state.isEmpty()`), la colonna risolve la prima posizione non ancora risolta
(`!state.isForcing()`). Con la maiuscola singola le due cose non sono sempre a inizio
parola insieme, ed è giusto che si comportino diversamente: premuto `⇧` i tasti passano a
`ABC/DEF/…`, alla prima cifra tornano minuscoli perché il resto della parola lo sarà.

**File:** `model/KeyAction.kt` (`DeleteWord`), `ui/KeyViewFactory.kt` (ripetizione a
pressione prolungata), `service/T9ImeService.kt` (`onDeleteWord`, `renderShift`),
`input/ShiftState.kt` (`appliesToNext`), `ui/DisambiguationColumnView.kt` (`setUppercase`),
`ui/T9BodyView.kt` (etichette dei tasti lettera), `ui/KeyboardView.kt`; test in
`ShiftStateTest`.
**Verificato su emulatore:** tasti `ABC/DEF/GHI…` con shift attivo e colonna `A B C À`,
minuscoli a shift spento; tocco singolo su `⌫` = un carattere; 1,6 s di pressione =
"tre quattro" spazzati via lasciando intatto il testo dopo il cursore →
`docs/screenshots/step-1.11-tasti-maiuscoli.png`, `step-1.11-colonna-maiuscola.png`.

### 2026-07-28 — Step 1.10: maiuscole, vocali accentate, emoji (pre-test reale)
Le tre cose che mancavano per scrivere davvero un messaggio. Nessuno dei tre tasti
del layout è più uno stub (resta solo `🎙`, che non è nel layout attuale).

**1. Maiuscole (`⇧`).** `input/ShiftState.kt`: `OFF → ONCE → LOCK`, il tasto cicla.
In T9 la maiuscola vale per la **parola**, non per il singolo tasto (le lettere le
decide il dizionario), quindi `ONCE` capitalizza la parola in corso e si consuma alla
conferma, `LOCK` scrive tutto maiuscolo finché non lo spegni. Il glifo diventa `⇪` e
passa dal teal al bianco quando è attivo. **Punto chiave:** la maiuscola si applica
**all'ultimo momento**, in `currentPreview()`; composizione e dizionario restano
minuscoli, così "Casa" e "casa" sono la stessa parola per lookup e apprendimento.

**2. Vocali accentate — nella colonna** (scelta fra le due proposte dall'utente).
`T9Keypad.accentedLetters`: 2→à, 3→è/é, 4→ì, 6→ò, 8→ù, offerte **dopo** le lettere
normali del tasto (la colonna scorre già). Non entrano in `letters`, per non toccare
etichette dei tasti né `sequenceFor` (che gli accenti li ripiega comunque): la sequenza
resta pulita, quindi "perché" si cerca e si impara come qualsiasi altra parola.
**Perché la colonna e non un popup long-press:** la colonna *è* già il meccanismo
"quale lettera esattamente", è sempre visibile e non aggiunge UI né conflitti col
long-press dei preferiti. Il popup sul tasto resta valutabile dopo il test, ma sarebbe
in gran parte ridondante.

**3. Emoji (`☺`).** `model/EmojiLayout.kt`: un pannello di 32 emoji comuni, otto per
riga (più spazio dei simboli, per restare riconoscibili). È **un altro `KeyGrid`**:
nessuna vista nuova, solo una voce in `KeyboardMode` — la riusabilità dello Step 1.8
che ripaga. `KeyAction.Emoji` sparisce, diventa `Mode(EMOJI)`.

**File:** `input/ShiftState.kt`, `model/EmojiLayout.kt` (nuovi), `model/T9Keypad.kt`
(accenti + `columnLetters`), `input/ComposeState.kt` (accetta gli accenti del tasto),
`model/KeyAction.kt`, `model/KeyboardMode.kt`, `model/SymbolLayout.kt`,
`ui/KeyViewFactory.kt` (`updateLabel`, glifi grandi contati in code point),
`ui/T9BodyView.kt`, `ui/KeyboardView.kt`, `service/T9ImeService.kt`; test
`input/ShiftStateTest.kt` (6 casi) e 2 nuovi in `ComposeStateTest`.
**Verificato su emulatore:** "Casa"/"CASA" col glifo che cambia, colonna con À su 2 e
È/É su 3 (scorrendo), emoji inserite e ritorno al T9 →
`docs/screenshots/step-1.10-shift-maiuscola.png`, `step-1.10-accentate-colonna.png`,
`step-1.10-emoji.png`.
**Non incluso:** maiuscola automatica a inizio frase (`getCursorCapsMode`) — da valutare
dopo il test, potrebbe dare fastidio più che aiutare.

### 2026-07-28 — Step 1.9: simboli preferiti nella colonna (a riposo)
**Fatto:** la colonna non è più spazio morto quando non stai scrivendo: mostra **7 simboli
preferiti** (`@ ? ! / - ' "` di default), quindi scorre (le celle mantengono la misura da 4).
Tap = inserisce. **Long-press** = apre le pagine simboli per sostituire *quella* posizione,
con la barra dei suggerimenti che spiega cosa sta aspettando ("Scegli il simbolo per la
posizione 2"); mentre la scelta è in sospeso i tasti simbolo **assegnano invece di scrivere**,
`1/2` resta navigabile e qualsiasi altro tasto (incluso `abc`) annulla.

**Riordino — la domanda posta:** non serve un drag&drop. Se il simbolo scelto è **già** fra i
preferiti, i due slot si **scambiano** (`FavouriteSymbols.replace`): un long-press + un tap
spostano un preferito dove vuoi, senza duplicati e senza perdere nulla. Il drag resta
un'eventuale rifinitura di Fase 3, non un prerequisito.

**Persistenza:** `SharedPreferences` (`settings/KeyboardSettings.kt`), non Room —
sette valori letti una volta e scritti a un tocco, dove un database sarebbe solo costo.
La logica di lista è pura e testata (`model/FavouriteSymbols.kt`): normalizzazione
(preferenza corrotta o vecchia → default per slot), sostituzione, scambio.

**Bug risolto strada facendo:** la colonna restava vuota alla prima comparsa. Le celle si
dimensionano sull'altezza della colonna, che è ignota mentre la input view viene costruita,
e i simboli possono arrivare **prima o dopo** quel momento; `onSizeChanged` non basta perché
non scatta quando la dimensione non cambia (vista riusata). Ora la ricostruzione avviene in
`onLayout`, che copre entrambi gli ordini.

**File:** `model/FavouriteSymbols.kt`, `settings/KeyboardSettings.kt` (nuovi),
`ui/DisambiguationColumnView.kt` (due tipi di contenuto: lettere o preferiti; long-press),
`ui/SuggestionBarView.kt` (+`showHint`), `ui/KeyboardView.kt`, `ui/T9BodyView.kt`,
`service/T9ImeService.kt`, `res/values/strings.xml`; test `model/FavouriteSymbolsTest.kt`
(6 casi, fra cui "ogni simbolo di default è raggiungibile dalle pagine simboli", che
impedisce uno slot cambiabile ma non più ripristinabile).
**Verificato su emulatore:** preferiti a riposo, sostituzione (`?`→`€`), **scambio**
(`/` in posizione 1, `@` finito in 4), inserimento al tap, e tutto **persistito** dopo
`force-stop` → `docs/screenshots/step-1.9-preferiti-colonna.png`,
`step-1.9-scelta-simbolo.png`, `step-1.9-preferiti-persistiti.png`.

### 2026-07-28 — Step 1.8: pagine simboli in stile QWERTY (layout riusabile)
**Fatto:** il tasto `12#` ora funziona davvero. Apre **due pagine di numeri e simboli**
disposte in stile **QWERTY** (dieci tasti stretti per riga, larghezza piena) invece della
griglia 3×3: `1/2` ↔ `2/2` scambia le pagine, `abc` torna al T9.

**Decisione (utente):** il layout doveva essere **riusabile**, perché la QWERTY vera
arriverà come alternativa alla T9. Quindi la struttura non è "una schermata simboli" ma
una griglia generica:
- `model/KeyGrid.kt` — `KeyGrid` = righe di `KeySpec`, ciascuna con un peso (riga inferiore
  più sottile). Nessuna assunzione T9.
- `ui/GridKeyboardView.kt` — disegna un `KeyGrid` qualsiasi. **Aggiungere la QWERTY
  significherà aggiungere un `KeyGrid`, non una vista nuova.**
- `ui/KeyViewFactory.kt` — il disegno del singolo tasto (faccia arrotondata, stato premuto,
  numerino d'angolo) estratto e **condiviso** fra T9, simboli e futura QWERTY, così non
  possono divergere visivamente.
- `model/KeyboardMode.kt` + `KeyAction.Mode(target)` — cambio di superficie tipizzato
  (prima `ModeSwitch` era un no-op). Aggiungere `QWERTY` all'enum basterà.
- `ui/KeyboardView.kt` (era `T9KeyboardView`) — radice che ospita barra suggerimenti +
  corpo della modalità corrente; `ui/T9BodyView.kt` è il solo corpo T9. Rinominata perché
  ora ospiterà anche superfici non-T9.

**Scelte di comportamento:** cambiando modalità la parola in corso viene **confermata**
(uscire dal tastierino a metà parola lascerebbe un composing text che nessun tasto può più
chiudere); ogni nuovo campo riparte da `abc`; nelle pagine simboli la barra suggerimenti
resta invisibile ma **occupa spazio**, così l'altezza della tastiera non salta.

**Simboli:** pagina 1 = cifre + punteggiatura di tutti i giorni (`@ # € _ & - + ( ) /`,
`* " ' : ; ! ?`); pagina 2 = segni rari (valute, matematica, parentesi, marchi). Due pagine
perché su una sola i tasti diventano troppo piccoli per il pollice.

**File:** `model/KeyGrid.kt`, `model/KeyboardMode.kt`, `model/SymbolLayout.kt`,
`ui/GridKeyboardView.kt`, `ui/KeyViewFactory.kt`, `ui/T9BodyView.kt`, `ui/KeyboardView.kt`
(nuovi; `ui/T9KeyboardView.kt` rimosso), `model/KeyAction.kt`, `model/T9Keypad.kt`,
`service/T9ImeService.kt`; test `model/SymbolLayoutTest.kt` (6 casi, fra cui "un tasto
inserisce esattamente ciò che mostra", che protegge dalle sviste nelle tabelle).
**Verificato:** test verdi; su emulatore inserito `€` dalla pagina 2 e tornato al T9 con
predizione funzionante → `docs/screenshots/step-1.8-simboli-pagina1.png`,
`step-1.8-simboli-pagina2.png`.

### 2026-07-28 — Step 1.7: candidati "fuzzy" (tolleranti agli errori)
**Fatto:** oltre ai match esatti la tastiera propone ora parole a **distanza di modifica 1**
dalla sequenza digitata: cifra di troppo (tasto premuto in più), cifra mancante (tasto
saltato), cifra sbagliata (tasto vicino).

**Come:** `FuzzyDictionaryEngine` **decora** un qualsiasi `DictionaryEngine`. Invece di
scandire il dizionario genera le **varianti della sequenza** (cancellazioni, sostituzioni,
inserimenti: qualche decina di stringhe) e le cerca nell'indice esistente → una manciata
di lookup O(1) per pressione, nessun matching fuzzy su 50k parole. Sta **fuori** dal
`MergingDictionaryEngine`, quindi tollera i refusi anche sulle parole imparate e (Fase 2)
su entrambe le lingue.

**Perché non disturba la digitazione normale:**
- i candidati fuzzy sono **marcati** (`Candidate.fuzzy`), pesati / 1000 e messi **dopo**
  tutti gli esatti, con un tetto di 6;
- sequenze sotto le 3 cifre non vengono corrette (a 2 cifre tutto è a distanza 1 da tutto);
- **soprattutto**: `currentPreview()` considera solo i candidati **esatti**. Un fuzzy è
  un'offerta da toccare, mai qualcosa da confermare di nascosto — altrimenti scrivere una
  parola che il dizionario non conosce si trasformerebbe in una parola simile, cioè
  esattamente ciò che la colonna serve a impedire;
- nella barra sono resi in grigio (`TEXT_DIM`) invece che bianco/teal.

**File:** `engine/FuzzyDictionaryEngine.kt` (nuovo), `engine/Candidate.kt` (campo `fuzzy`),
`service/T9ImeService.kt` (wrapping + preview solo esatta), `ui/SuggestionBarView.kt`
(colore smorzato); test `engine/FuzzyDictionaryEngineTest.kt` (9 casi).
**Verificato:** test verdi; su emulatore, digitando `726333` (nessun match esatto)
l'anteprima resta `pamddd` e in coda compaiono in grigio "sandé" (cifra di troppo) e
"schede" (cifra sbagliata) → `docs/screenshots/step-1.7-candidati-fuzzy.png`.

### 2026-07-28 — Step 1.5b: candidati più grandi (feedback utente)
**Fatto:** i candidati nella barra suggerimenti erano troppo piccoli: testo da 17sp →
**22sp** (`SuggestionBarView.DEFAULT_TEXT_SP`) e barra da 46dp → **56dp** per lasciare
respiro. La dimensione è esposta come proprietà `textSizeSp` (applicata anche ai chip già
creati), pronta per essere pilotata dalle **impostazioni della tastiera in Fase 3**
insieme all'altezza regolabile.
**File:** `ui/SuggestionBarView.kt`, `ui/T9KeyboardView.kt` (`BAR_DP`).
**Verificato:** `docs/screenshots/step-1.5b-candidati-piu-grandi.png`.

### 2026-07-28 — Step 1.5: apprendimento persistente (dizionario personale, Room)
**Fatto:** la tastiera ora **impara**. Ogni parola effettivamente confermata — con spazio,
invio, punteggiatura, o scegliendo un suggerimento — finisce nel dizionario personale,
che viene consultato **prima** del corpus. Una parola forzata lettera per lettera con la
colonna va quindi digitata "a mano" **una sola volta**: dalla seconda in poi è la prima
predizione della sua sequenza.

**Come funziona (architettura):**
- `LearnedWordsEngine` — il dizionario personale come `DictionaryEngine`. Indice in RAM
  `sequenza → (parola → n. usi)`: durante la digitazione **non si tocca mai il database**.
  Peso = `BASE_WEIGHT (1.000.000) + usi × 1.000`, sopra la frequenza massima del corpus
  (~75k di "di"), così una parola imparata batte sempre le parole di dizionario con la
  stessa sequenza, e le più usate salgono fra loro. Kotlin puro (nessuna dipendenza
  Android) grazie al seam `LearnedWordsEngine.Store`.
- `MergingDictionaryEngine` — unisce più dizionari dietro l'unica interfaccia `lookup`,
  deduplicando per parola (tiene il peso più alto). Serve ora per personale+corpus e
  **servirà identico in Fase 2** per IT+EN.
- Room (`learning/`): entità `LearnedWord` (parola PK, sequenza, usi, ultimo uso), `LearnedWordDao`,
  `LearnedWordsDatabase`, e `RoomLearnedWordsStore` che **scrive in coda su un thread
  singolo** (la pressione di un tasto non deve mai attendere il disco); l'ordine è garantito
  dall'executor a thread singolo. Il DB è locale, non esce mai dal dispositivo.
- `T9ImeService`: il dizionario personale esiste **da subito** (il corpus no, si carica in
  background), quindi si può imparare anche mentre il corpus è ancora in caricamento;
  a caricamento finito `engine` diventa il merge dei due.

**File:** `engine/LearnedWordsEngine.kt`, `engine/MergingDictionaryEngine.kt`,
`learning/LearnedWord.kt`, `learning/LearnedWordDao.kt`, `learning/LearnedWordsDatabase.kt`,
`learning/RoomLearnedWordsStore.kt` (nuovi); `service/T9ImeService.kt`;
`build.gradle.kts` + `app/build.gradle.kts` (plugin KSP + Room 2.6.1);
test `engine/LearnedWordsEngineTest.kt`, `engine/MergingDictionaryEngineTest.kt`.

**Verificato (emulatore Pixel, Android 17):** test verdi (`:app:testDebugUnitTest`).
1. "daux" (3‑2‑8‑9, assente dal corpus) forzata dalla colonna + spazio; ridigitando
   3‑2‑8‑9 è la prima predizione → `docs/screenshots/step-1.5-parola-imparata.png`.
2. `adb shell am force-stop com.daux.t9keyboard`, riapertura: 3‑2‑8‑9 predice ancora
   "daux" (letta dal DB) → `step-1.5-persistenza-dopo-riavvio.png`. Il file
   `databases/learned_words.db` esiste nella sandbox dell'app.
3. Scegliendo "cara" da 2272 (dove il corpus mette "casa" prima), la digitazione
   successiva propone `cara, casa, basa, bara` — una sola "cara", senza duplicati →
   `step-1.5-imparata-batte-corpus.png`.

**Nota:** cancellare una parola imparata (long‑press sul candidato) e la schermata di
gestione del dizionario personale restano in Fase 3; il DAO ha già `delete(word)`.

### 2026-07-27 — Step 1.6: corpus reale Leipzig (dizionario italiano da 50k parole)
**Fatto:** sostituito il dizionario di test (~40 parole) con un dizionario **reale**.
Scaricato il corpus **Leipzig `ita_news_2022_100K`** (CC BY-4.0), convertito con
`tools/ConvertLeipzig.java` (filtra parole italiane a-z+accenti, unisce varianti
maiuscole/minuscole sommando le frequenze, ordina, tiene le top 50k) → `assets/dict/it.txt`
(~576KB, header con attribuzione CC BY). Caricato **in background** (`Thread` in `onCreate`)
per non bloccare la comparsa della tastiera; `engine` reso nullable e i lookup null-safe.

**Come rigenerare il dizionario** (se serve un taglio diverso):
```
JAVA="/c/Program Files/Android/Android Studio/jbr/bin/java.exe"
# scarica ed estrai un corpus Leipzig italiano (es. da downloads.wortschatz-leipzig.de),
# poi:
"$JAVA" tools/ConvertLeipzig.java <path/...-words.txt> app/src/main/assets/dict/it.txt 50000
```

**File:** `tools/ConvertLeipzig.java` (nuovo), `assets/dict/it.txt` (nuovo), rimosso
`assets/dict/it_test.txt`; `service/T9ImeService.kt` (load in background, engine nullable).
**Verificato:** test verdi; su emulatore "grande" (4-7-2-6-3-3) predetto correttamente.
Screenshot `docs/screenshots/step-1.6-dizionario-reale.png`.

### 2026-07-27 — Fix: scrittura di numeri invece di lettere
**Bug (segnalato):** digitando dall'emulatore, per sequenze **non nel dizionario**
l'anteprima ripiegava sulle cifre grezze (`sequenceString()`), quindi premendo spazio
venivano scritti i **numeri** (es. "36987").
**Fix:** `currentPreview()` ora usa `ComposeState.defaultLetters()` (prima lettera di ogni
tasto) come fallback → sempre lettere, mai cifre; la colonna corregge le singole lettere.
**File:** `input/ComposeState.kt` (+`defaultLetters()`), `service/T9ImeService.kt`.
Test di regressione in `ComposeStateTest`. **Verificato** su emulatore: "casa"+spazio →
"casa ", 9-9-9 → "www" (non "999"). Screenshot `docs/screenshots/type-*.png` non salvati.

### 2026-07-27 — Step 1.4e: proporzioni (meno verticale, colonna a riempimento)
**Fatto:** (feedback utente) tastiera **meno alta** per avvicinarsi alle proporzioni del
riferimento (`BODY_HEIGHT_FRACTION` 0.44→0.34): tasti più larghi che alti. Celle colonna
non più minuscole a altezza fissa ma **dinamiche**: `cellHeight = viewport / count`
(count limitato a 3–4), così 3–4 elementi **riempiono** la colonna; oltre 4 mantengono la
dimensione da 4 e la colonna **scorre**. `onSizeChanged` ricalcola al variare dell'altezza.
**File:** `ui/DisambiguationColumnView.kt`, `ui/T9KeyboardView.kt`.
**Verificato:** `docs/screenshots/step-1.4e-proporzioni.png` (colonna P/Q/R/S che riempie).
**Nota:** l'altezza sarà comunque resa **regolabile dall'utente** in Fase 3.

### 2026-07-27 — Step 1.4d: colonna piccola+scorrevole, riga inferiore più sottile
**Fatto:** (feedback utente) `DisambiguationColumnView` ora è una `ScrollView` con celle
ad **altezza fissa piccola** (`CELL_HEIGHT_DP=40`); scorre quando gli elementi eccedono
lo spazio (pronta per liste candidati lunghe e per i simboli preferiti a riposo in Fase 3).
Riga inferiore resa **più sottile** (`BOTTOM_ROW_WEIGHT=0.72` vs 1 delle righe lettere).
Colonna leggermente più stretta (`COLUMN_WEIGHT=0.9`, `12#` allineato a 0.9).
**File:** `ui/DisambiguationColumnView.kt`, `ui/T9KeyboardView.kt`, `model/T9Keypad.kt`.
**Verificato:** `docs/screenshots/step-1.4d-colonna-piccola-scroll.png`.

### 2026-07-27 — Step 1.4c: rifiniture layout (feedback utente)
**Fatto:** (su richiesta) colonna disambiguazione **più chiara** (`COLUMN_CELL` slate) e
**limitata all'area superiore** (non arriva più fino in fondo: si ferma sopra la riga
inferiore); `12#` ora sta **sotto la colonna** (angolo in basso a sx); riga inferiore
**full-width** con `12# · , · space · . · ⏎` (microfono sostituito dal **punto**);
**icone outline** monocromatiche (faccina `☺︎` teal via U+FE0E, niente emoji colorata).

**File modificati:** `ui/KeyboardTheme.kt` (colori cella colonna), `ui/DisambiguationColumnView.kt`
(cella chiara, testo bianco), `model/T9Keypad.kt` (rightColumn/bottomRow, pesi allineati a 7.6),
`ui/T9KeyboardView.kt` (body verticale: upperArea con colonna+griglia+funzioni, poi bottomRow full-width).
**Verificato:** screenshot `docs/screenshots/step-1.4c-layout-rifinito.png`.

### 2026-07-27 — Step 1.4b: struttura layout fedele all'originale
**Fatto:** riprodotta la struttura esatta di `docs/screenshots/layout.jpg`: colonna
disambiguazione a sinistra (piena altezza), griglia 3×3 lettere al centro, **colonna
funzioni a destra** (⌫ backspace, ⇧ shift, ☺ emoji), **riga inferiore full-width**
(12# · virgola · space bar larga · 🎙 mic · ⏎ invio).

**File modificati:**
- `model/KeyAction.kt` — nuove azioni: `Space`, `Insert(text)`, `Shift`, `ModeSwitch`,
  `Emoji`, `Mic` (le ultime 4 sono stub per Fase 3, presenti per fedeltà).
- `model/T9Keypad.kt` — `KeySpec` ora ha `weight`; layout diviso in `letterRows` (3×3),
  `rightColumn`, `bottomRow`.
- `ui/T9KeyboardView.kt` — nuova composizione a pesi (colonna | [griglia+colonna funzioni]
  sopra, riga inferiore sotto).
- `service/T9ImeService.kt` — gestione `Space` (commit+spazio), `Insert` (virgola),
  stub no-op per shift/mode/emoji/mic.

**Funzionanti:** ⌫, virgola, space, ⏎. **Stub (Fase 3):** ⇧, 12#, ☺, 🎙.
**Verificato:** build+install OK, screenshot `docs/screenshots/step-1.4b-struttura-completa.png`.

### 2026-07-27 — Step 1.4: restyle grafico stile TouchPal
**Fatto:** overhaul visivo per avvicinarsi all'originale (riferimento in
`docs/screenshots/layout.jpg`). Tema scuro definito. Prima era "grezzo" (rettangoli
piatti); ora tasti arrotondati con feedback al tocco, lettere grandi minuscole con
numero piccolo teal, accenti ciano su colonna e tasti funzione.

**File creati/modificati:**
- `ui/KeyboardTheme.kt` — palette condivisa (BG, KEY, ACCENT teal, ecc.) + builder di
  sfondi arrotondati con stato "pressed" (`keyBackground`, `ghostBackground`).
- `model/T9Keypad.kt` — `KeySpec` ora ha `mainLabel`/`number`/`isFunction`; `T9Layout`
  mostra le lettere in grande e il numero piccolo (tasto 1 = "@").
- `ui/T9KeyboardView.kt` — tasti come `FrameLayout` (label centrata + numero d'angolo),
  palette nuova, **gestione WindowInsets** per non finire sotto la nav bar (edge-to-edge
  di targetSdk 35), altezza che tiene conto dell'inset.
- `ui/SuggestionBarView.kt` / `ui/DisambiguationColumnView.kt` — restilizzati (primo
  suggerimento teal, celle arrotondate con feedback).

**Verificato (2026-07-27):** build+install OK, screenshot
`docs/screenshots/step-1.4-restyle-touchpal.png` — riga inferiore ora sopra la nav bar.

**Da fare per fedeltà piena (opzionale, prossimo step possibile):** colonna funzioni a
destra (shift/redo), riga inferiore con `12#` + virgola + space bar con lingua + mic +
emoji, come nell'originale. Attuale set funzioni è semplificato (⌫, space, ⏎).

### 2026-07-27 — Step 1.3: colonna di disambiguazione manuale (funzione centrale)
**Fatto:** implementata la colonna posizionale che permette di **forzare parole non nel
dizionario** lettera per lettera (sez. 3 del piano).

**File creati/modificati:**
- `input/ComposeState.kt` — stato della composizione: `digits` (cifre premute) + `chosen`
  (lettere forzate), invariante `chosen.length ≤ digits.size`. Op: `pressDigit`,
  `chooseLetter` (valida che la lettera appartenga alla cifra), `backspace` (pop della
  coppia intera, o della cifra non risolta in coda), `activeColumnDigit` (posizione
  corrente della colonna), `forcedText`, `sequenceString`, `isForcing`, `reset`.
  Estendere e correggere sono la stessa operazione (backspace + ripressione).
- `ui/DisambiguationColumnView.kt` — striscia verticale a lato della griglia con le
  lettere del tasto alla posizione corrente; tap = forza la lettera.
- `ui/T9KeyboardView.kt` — body ora orizzontale: colonna (peso 1) + griglia (peso 5),
  responsivo; nuovo `setColumnLetters()`; callback `onPickLetter`.
- `service/T9ImeService.kt` — integra `ComposeState`: `render()` unico che aggiorna
  anteprima (parola forzata se in forcing, altrimenti predizione), barra e colonna.
- Test: `input/ComposeStateTest.kt` (8 casi: forcing, walk, pop-coppia, correzione, ecc.).

**Verificato (2026-07-27):** `:app:testDebugUnitTest` verde; su emulatore forzata la
parola **"bau"** (2‑2‑8, non nel dizionario) scegliendo B‑A‑U dalla colonna.
Screenshot: `docs/screenshots/step-1.3-colonna-lettere.png`, `step-1.3-forcing-bau.png`.

**Nota:** la colonna a riposo (stack vuoto) è per ora vuota; i simboli preferiti
configurabili arrivano in Fase 3. L'apprendimento della parola forzata è Step 1.5.

### 2026-07-27 — Step 1.2: motore predittivo + barra suggerimenti
**Fatto:** l'inserimento ora è **predittivo T9**. I tasti 2–9 costruiscono una sequenza
di cifre; il motore la mappa alle parole del dizionario ordinate per frequenza. La prima
scelta appare come anteprima (composing text), l'intera lista nella barra suggerimenti
(tap per scegliere). `0`/spazio conferma la parola, `⌫` accorcia la sequenza, `⏎` conferma
ed esegue l'azione editor. Multi-tap rimosso.

**File creati/modificati:**
- `engine/Candidate.kt` — modello candidato (word, sequence, weight su scala confrontabile).
- `engine/DictionaryEngine.kt` — interfaccia unica di lookup (seam per il bilingue Fase 2).
- `engine/ItalianDictionaryEngine.kt` — indice in RAM `sequenza → [candidati]`; `fromAssets()`
  + `build(lines)` puro e testabile.
- `model/T9Keypad.kt` — aggiunto `sequenceFor(word)` (parola→cifre, con fold accenti IT).
- `assets/dict/it_test.txt` — dizionario di test (~40 parole, con collisioni per il ranking).
- `ui/SuggestionBarView.kt` — barra suggerimenti orizzontale scrollabile (chip tap-abili).
- `ui/T9KeyboardView.kt` — ora contiene barra suggerimenti + griglia; `setSuggestions()`.
- `service/T9ImeService.kt` — logica predittiva (buffer sequenza, anteprima, commit, pick).
- Test JVM: `model/T9KeypadTest.kt`, `engine/ItalianDictionaryEngineTest.kt`.

**Come verificare (Android Studio):**
1. Test logici: esegui gli unit test (`app/src/test/...`) — devono passare senza emulatore.
2. Sul device/emulatore: digita `2272` → la barra mostra "casa, cara, bara" (casa prima);
   `2663` → "come". Tocca un suggerimento per sceglierlo; `0` conferma + spazio.
3. Sequenza senza match (es. `99999`) → anteprima mostra le cifre grezze (atteso finché
   non c'è la colonna, Step 1.3).

**Screenshot (verifica su emulatore Android 15):**
- `docs/screenshots/step-1.1-griglia-tastiera.png` — la griglia 12 tasti.
- `docs/screenshots/step-1.2-predittivo-casa.png` — `2272` → "casa" + suggerimenti cara/bara/basa.

### 2026-07-27 — Step 1.1: griglia 12 tasti + multi-tap
**Fatto:** sostituito il placeholder con la vera griglia della tastiera e l'inserimento testo.

**File creati/modificati:**
- `model/KeyAction.kt` — azioni tasto (Digit/Backspace/Enter).
- `model/T9Keypad.kt` — mapping ITU-T E.161 (`letters`), `KeySpec`, layout `T9Layout` 4×3.
- `ui/T9KeyboardView.kt` — griglia responsive (righe/tasti con `weight`, altezza = 42% schermo),
  tasti con label grande + sottotitolo lettere; ogni tap notificato via callback.
- `service/T9ImeService.kt` — logica **multi-tap**: stessa cifra ripetuta cicla le lettere
  (via composing text, timeout 800ms), cifra diversa conferma la precedente; `0`=spazio,
  `⌫`=cancella (annulla il composing se in corso), `⏎`=azione editor.
- Rimosso `ui/PlaceholderKeyboardView.kt`.

**Come verificare (in Android Studio, emulatore o S25):**
1. Run/reinstalla, seleziona **T9 Keyboard** in un campo di testo.
2. Deve comparire la griglia 4×3 scura (1–9, ⌫, 0, ⏎).
3. Digita: es. `8`→"t", subito ancora `8`→"u", ancora→"v"; pausa >0.8s conferma la lettera.
   Prova a scrivere "ciao" (2·2·2→c pausa, 4·4·4→i, 2→a, 6·6·6→o).
4. `0` inserisce spazio, `⌫` cancella, `⏎` invia/va a capo secondo il campo.

**Nota:** il multi-tap è temporaneo (trampolino); Step 1.2 introduce il predittivo.

### 2026-07-27 — Fase 0: scaffolding progetto Android
**Fatto:** creato l'intero scheletro Gradle + modulo `app` di un IME Android installabile.
Il servizio `T9ImeService` mostra una `PlaceholderKeyboardView` (pannello scuro con label)
per provare che la tastiera si renderizza. Nessun permesso sensibile dichiarato.

**File creati:**
- `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `.gitignore`
- `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9)
- `app/build.gradle.kts` (namespace `com.daux.t9keyboard`, compile/target SDK 35, minSdk 26, JVM 17)
- `app/src/main/AndroidManifest.xml` (service con `BIND_INPUT_METHOD` + intent-filter `android.view.InputMethod`)
- `app/src/main/res/xml/method.xml` (subtype italiano)
- `app/src/main/java/.../service/T9ImeService.kt`
- `app/src/main/java/.../ui/PlaceholderKeyboardView.kt`
- `app/src/main/res/values/strings.xml`, `themes.xml`, `res/drawable/ic_launcher.xml`

**Come verificare (Milestone Fase 0, da fare in Android Studio):**
1. Apri il progetto in Android Studio → sync Gradle (rigenera il wrapper).
2. `Run` / `installDebug` su emulatore Android 15 o su S25.
3. Impostazioni Android → *Gestione generale → Elenco tastiere e predefinita* → abilita **T9 Keyboard**.
4. In un campo di testo, cambia tastiera e seleziona **T9 Keyboard**: deve comparire il
   pannello placeholder scuro con la scritta "T9 Keyboard — placeholder (Fase 0)".

**Nota:** wrapper JAR/`gradlew` non versionati (vedi avviso in STATO CORRENTE).
