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

- **Fase in corso:** Fase 1 — MVP **completa**. Ultimi step: 1.7 (fuzzy) e 1.8 (simboli).
- **Ultimo step completato:** Step 1.11 — **cancella con pressione prolungata** (accelera e passa alle parole intere) e **maiuscole visibili** su tasti e colonna. Entrambi da feedback della prima prova reale.
- **Prossimo step:** **Step 1.12 — popup long-press sul tasto** (stile tastiera Google) per accentate e caratteri speciali. Deciso dall'utente come **prerequisito alla Fase 2**: si fa prima del bilingue. Piano dettagliato nella sezione "Step 1.12 — piano" qui sotto.
- **Dopo:** **Fase 2** (bilingue IT+EN), poi **Fase 3** (impostazioni/ergonomia, dove rientra la **QWERTY come layout alternativo**, richiesta dall'utente). Il test reale sullo smartphone continua in parallelo. APK: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
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

> 🛠️ **Build & test da riga di comando (senza Android Studio aperto).** Il wrapper
> `gradlew` non è nel repo, ma si può usare il Gradle+JDK già scaricati da Android Studio.
> Da Git Bash, dalla root del progetto:
> ```bash
> export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
> export ANDROID_HOME="/c/Users/Antonio/AppData/Local/Android/Sdk"
> GRADLE=$(ls /c/Users/Antonio/.gradle/wrapper/dists/gradle-8.13-bin/*/gradle-8.13/bin/gradle | head -1)
> "$GRADLE" :app:testDebugUnitTest --console=plain   # unit test
> "$GRADLE" :app:installDebug --console=plain         # build+install su emulatore/S25
> ```
> adb sta in `$ANDROID_HOME/platform-tools/adb.exe`. Screenshot emulatore:
> `adb exec-out screencap -p > out.png`.

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
- [ ] **Step 1.12 — popup long-press sul tasto** (accentate e caratteri speciali, stile
      Gboard) — piano dettagliato nella sezione "Step 1.12 — piano" più sopra
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
- [→] Long-press tasto 1 → caratteri di punteggiatura — **spostato allo Step 1.12**
- [→] Popup long-press sul tasto con lettere+accentate — **spostato allo Step 1.12**
      (deciso dall'utente dopo il test reale: si fa prima della Fase 2)
- [ ] Maiuscola automatica a inizio frase (`getCursorCapsMode`)
- [x] Modalità numerica/simboli dedicata (`12#`, due pagine QWERTY) — anticipata allo Step 1.8
- [ ] **QWERTY come layout alternativo alla T9** (idea utente): un nuovo `KeyGrid` +
      voce in `KeyboardMode`; vista e plumbing già pronti dallo Step 1.8
- [→] Vocali accentate via long-press — **spostato allo Step 1.12**
- [ ] Schermata gestione dizionario personale (lista + cancella)
- [ ] Long-press su candidato → rimuovi dal dizionario
- [ ] Opzione "salvataggio sicuro"
- [ ] Pannello emoji base
- [ ] Verifica layout responsive su S25 e S25 Ultra

---

## 🧭 Step 1.12 — piano: popup long-press sul tasto (stile Gboard)

> **Stato:** pianificato, non ancora implementato. Da fare **prima della Fase 2**
> (decisione dell'utente). Voce di Fase 1: `- [ ] Step 1.12`.

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

### I due popup di simboli (decisione dell'utente)

Due popup di simboli con ruoli **complementari e non sovrapposti** — uno curato da te, uno
che si adatta all'uso:

**`.` (riga inferiore, accanto allo spazio) → i 7 simboli preferiti.**
Sembra un doppione della colonna, e invece copre il buco che la colonna lascia: i preferiti
si vedono **solo a riposo**, perché appena componi una parola la colonna diventa lettere. Il
popup su `.` li rende raggiungibili **anche a metà parola**, che è esattamente quando servono
(apostrofo, trattino). Stessa lista, stessa `FavouriteSymbols`, nessun dato nuovo: cambiare un
preferito dalla colonna cambia anche il popup, per costruzione.

**`1` (tastierino) → i simboli più usati, per frequenza.**

- **Conteggio:** `settings/SymbolUsage.kt` (logica pura, testabile) + persistenza in
  `KeyboardSettings` su `SharedPreferences`. **Non Room:** sono qualche decina di contatori
  interi, esattamente il caso in cui un database è solo costo — la stessa scelta già fatta per
  i preferiti (§10 di `FUNCTIONAL.md`). L'incremento avviene su ogni `KeyAction.Insert`
  (pagine simboli, popup, preferiti), in un punto solo.
- **⚠️ Vincolo fondamentale: la frequenza decide *quali* simboli entrano, non *dove* stanno.**
  Un popup che si riordina da solo sposta i simboli sotto il pollice e impedisce alla memoria
  muscolare di formarsi — è il difetto classico dei menu adattivi, e renderebbe il popup più
  lento del percorso `12#`. Quindi: **ordine canonico fisso** (quello in cui compaiono nelle
  pagine simboli) e insieme **ricalcolato solo all'apertura di un campo**
  (`onStartInputView`), **mai** mentre stai digitando.
- **Seed:** con zero dati d'uso il popup deve essere già utile → parte dai caratteri storici
  del tasto `1` (`. , ? ! '`) più `@`. Chiude anche il buco esistente: quei caratteri oggi
  sono **irraggiungibili**.
- **Dimensione:** 6 celle, quanto sta comodo su una riga sopra un tasto di prima colonna.

**Decisione collegata — il tap su `1`.** Oggi il tasto mostra `@` ma toccandolo non scrive
nulla: si limita a confermare la parola. Con il popup sopra, il tap deve **inserire `@`**,
cioè ciò che il tasto mostra: è l'invariante che `SymbolLayoutTest` già protegge sulle pagine
simboli ("un tasto inserisce esattamente ciò che mostra") e violarla proprio qui sarebbe
incoerente. Il tap su `.` resta invariato.

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

- `2`→`a b c à` · `3`→`d e f è é` · `4`→`g h i ì` · `5`→`j k l` · `6`→`m n o ò` ·
  `7`→`p q r s` · `8`→`t u v ù` · `9`→`w x y z`
- `1`/`@` → **i 6 simboli più usati** (seed: `. , ? ! ' @`), ordine canonico fisso
- `.` → **i 7 simboli preferiti**, gli stessi della colonna
- `,` → `, ; : "` (fisso)
- Pagine simboli (opzionale, se non allunga troppo lo step): `-`→`– — _`, `(`→`[ {`, ecc.

### Verifica

- **Unit test** (`LongPressKeysTest`, `SymbolUsageTest`), sulla parte pura: "una cella
  inserisce esattamente ciò che mostra" (come `SymbolLayoutTest`), "ogni accento di
  `T9Keypad.accentedLetters` è raggiungibile da un popup", "nessun tasto con alternative è
  anche un tasto a gesto riservato (`⌫`)"; sulla frequenza: conteggio e top-N, **ordine
  canonico indipendente dai conteggi** (il test che protegge la memoria muscolare), seed
  usato quando non ci sono dati, preferenza corrotta che non svuota il popup.
- **Emulatore:** long-press su `3` → `d e f è é`, scorri e rilascia su `è` → composizione
  coerente (la parola prosegue, la sequenza è giusta); long-press su `.` → i preferiti,
  **anche a metà parola**; long-press su `1` → i più usati, e dopo aver usato molto un simbolo
  esso compare al **riavvio successivo** senza che l'ordine degli altri sia cambiato;
  rilascio fuori = niente; con `⇧` attivo le celle sono maiuscole. Screenshot in
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
- **Due popup di simboli sono troppi?** `.` (curato) e `1` (adattivo) hanno ruoli diversi, ma
  se all'uso reale si sovrapponessero — perché i preferiti *sono* i più usati — il ripiego è
  tenere solo `.` e restituire al popup di `1` la punteggiatura fissa. Da valutare sul campo
  dopo qualche giorno d'uso, quando i conteggi saranno significativi.

---

## 📓 Log di sviluppo (append in fondo, più recente in alto)

<!-- Formato: ### AAAA-MM-GG — titolo step -->
<!-- Cosa fatto, file toccati, note/decisioni, come verificare. -->

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
