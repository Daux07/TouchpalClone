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

- **Fase in corso:** **Fase 3 — impostazioni ed ergonomia**, pianificata con l'utente il 04/08 (vedi *Fase 3 — decisioni di piano*): fatte 3.1–3.7, si prosegue con la 3.8. Fasi 1 e 2 complete.
- **Ultimo step completato:** **3.7 — backspace ed emoji scambiati**: `⌫` scende in basso a destra e `☺` sale in cima, per stare dove la Gboard lo ha già insegnato al pollice (richiesta dell'utente). Prima: **3.6 — il maiuscolo in mezzo alla parola** (segnalazione dell'utente: *"volevo memorizzare xD con la D maiuscola"*): mentre si scelgono le lettere a mano dalla colonna, `⇧` vale per **quella lettera lì**, e il dizionario personale ricorda la forma scritta quando porta una maiuscola che nessuna regola avrebbe potuto metterci. `xD`, `iPhone`, `McDonald` si scrivono e si ripropongono come sono. Prima: **3.5 — l'app si chiama DauxPal, e ogni build lascia un APK versionato**: nome e etichetta tastiera passano da "T9" a "DauxPal" (scelta dell'utente), l'icona da "T" a "D" — **segnaposto, ne arriverà una vera**. L'`applicationId` non cambia, o si perderebbe il dizionario personale. E il file versionato lo produce ora anche `install`, non solo `apk`: usando `install` ci si ritrovava sempre col solo `app-debug.apk`. Prima: **3.4 — larghezza dei tasti e lato (idea dell'utente)**: un cursore stringe i tasti dal 100% al 60% dello schermo e un interruttore li porta a sinistra per i mancini. Il pannello resta largo quanto lo schermo: non rimpicciolisce la tastiera, la **avvicina al pollice**. Default a tutto schermo — stringere costa precisione, quindi si accende solo chi ne ha bisogno. Prima: **3.3 — altezza della tastiera e dimensione del testo dei candidati**: due cursori sopra l'anteprima, che si muove sotto le dita mentre li sposti. L'altezza va dal 22% al 40% dello schermo e riproporziona tutto in modo uniforme — è gratis, perché la disposizione è fatta di pesi; il testo dei candidati va da 12 a 24 sp sul seam che aspettava dallo Step 1.25. Prima: **3.2 — anteprima viva nella schermata impostazioni**: in fondo alla schermata c'è una tastiera **vera** (`KeyboardView` con le callback a vuoto), non un disegno. Serve alla 3.3 — una misura non si sceglie alla cieca — ma si ripaga già ora: i tasti sono vivi, quindi la durata della vibrazione si giudica premendo un tasto invece che dal solo colpo al rilascio dello slider. Prima: **3.1 — le impostazioni si raggiungono e contengono qualcosa**: una rotellina sopra la colonna, all'altezza dei candidati, apre la schermata senza uscire dall'app in cui si scrive; dentro ci sono maiuscola e spazio automatici e la durata della vibrazione, che vibra quando lasci lo slider. Prima: **2.5 — due ritocchi grafici chiesti dall'utente**: la riga dello spazio è un decimo più alta di una riga di lettere (si mancava lo spazio ogni tanto) e la freccia dello shift passa da 20sp a 26sp, perché un glifo di contorno alla misura dei pieni si legge più piccolo di loro. La tastiera si alza da 0,28 a 0,287 dello schermo — l'utente ha dato il permesso, così le lettere non si appiattiscono per pagare la riga. Prima: **2.4 — l'elisione non impara più spazzatura, e `c'è` sì**: `Elision.join` non unisce una coda di una sola lettera non accentata, quindi `l'a` non entra più nel dizionario mentre `c'è`, `n'è`, `s'è` restano imparabili — e sono l'unico modo in cui possano comparire, visto che nessun corpus contiene apostrofi. Prima: **2.3 — il peso delle parole imparate**: da 1.000.000 fisso a abitudine + spinta recente sulla scala del corpus, e la pressione prolungata su un candidato lo dimentica. Prima: **2.2 — la `b` prima della `a`**: una lettera singola imparata da una build vecchia dominava il tasto 2; la regola scende nel dizionario e l'archivio viene bonificato al caricamento. Prima: **Fase 2.1 — il bilinguismo si accende e si spegne**: schermata impostazioni raggiungibile dal launcher, e la lingua è un elenco (`Language`) invece di un interruttore per l'inglese, così una terza costa un dizionario e una riga. Prima: **Fase 2 — bilingue IT+EN** (versione 2.0): italiano e inglese attivi insieme, senza cambio lingua; l'inglese non scavalca mai l'italiano, quindi nessuna sequenza che funzionava prima si ordina diversamente. Prima: Step 1.26 — **riga dello spazio uniformata** alle file di lettere (senza far ricrescere la tastiera) e **una lettera sola non è mai un nome proprio**: premendo `2` la barra offriva "a B C à". Prima: 1.25 — **tastiera più bassa**: barra candidati quasi dimezzata e tasti più larghi che alti; la tastiera passa dal 40% al 31% dello schermo. Prima: 1.24 — **resilienza ai refusi doppi**: due tasti invertiti (che prima sfuggivano del tutto) e, come ultima risorsa sulle parole lunghe, due tasti sbagliati. Prima: 1.23 (anteprima leggibile: senza corrispondenze esatte il campo mostra la migliore offerta invece delle lettere di default), 1.22 — **completamento di parola**: dopo le parole che i tasti scrivono esattamente, la barra offre quelle di cui i tasti sono l'inizio (dieci tasti per `contemporaneamente`). Prima: 1.21 (la tastiera vibra sotto il dito, con durata regolabile — `KeyboardSettings.hapticMs`, default 18 ms), 1.20 (la versione è visibile: `versionName` è il numero dello step e da lì derivano nome app ed etichetta tastiera — nel selettore si legge "T9 1.21"), 1.19 (l'apostrofo dell'elisione sta dentro la parola: `l'aveva` si impara come parola unica e si riscrive digitando le sole lettere), 1.18 (la tastiera riprende la parola già scritta sotto il cursore: sposti il cursore a fine parola, continui a digitare, e la parola intera viene imparata sullo spazio o sul candidato), 1.12k (i popup da 5 celle vanno su due righe (3+2), che risolve anche l'ultima cella irraggiungibile sui tasti `6` e `9`), 1.12j (anche il popup del `.` preseleziona la prima cella), 1.12i (il gesto parte dalla cella preselezionata, risolto il salto della selezione al primo movimento), 1.12h (guadagni separati per asse, orizzontale ×1.5 e verticale ×2.5, pannello staccato di 10dp dal tasto), 1.12g (la cifra è preselezionata all'apertura: tenere premuto un tasto numerico e rilasciare scrive il suo numero), 1.12f (guadagno anche in orizzontale e cifra come prima cella), 1.12e (asse verticale amplificato ×2 e misurato dal dito anziché dal centro del tasto), 1.17 (dizionario da messaggi: sottotitoli 70% + prosa giornalistica 30%, che resta la fonte delle maiuscole per i nomi propri), 1.16 (nomi propri misurati), 1.15 (regole italiane di maiuscole e spaziatura), 1.14 (tasto singolo), 1.13 (maiuscola e spazio automatici), 1.12a–d (popup long-press).
- **Da provare sul telefono:** tutto lo Step 1.15 e 1.16 insieme alla prova reale già in sospeso — in particolare abbreviazioni e puntini di sospensione, coperti dai test ma non provati a mano.
- **Prossimo step:** **3.8 — scorrimento del cursore trascinando sulla barra spazio** (idea dell'utente). Indipendente dalla schermata impostazioni: occupa il long-press dello spazio, tenuto libero apposta fin dallo Step 1.12 perché lo `0` è già sulla virgola.

  ℹ️ *Era numerato 3.5, ma quel numero se lo sono preso gli step chiesti dall'utente strada facendo (DauxPal, il maiuscolo interno, lo scambio del backspace). Il piano del 04/08 aveva l'ordine giusto, non i numeri definitivi.*
- **In attesa di riscontro dall'utente:** il **punto 2** non si riproduce sull'emulatore — riprovato sulla 2.4 il 04/08 sia a inizio campo sia a inizio frase, con la maiuscola automatica armata (vedi sopra). Resta da provare sul telefono con `dauxpal-3.7-debug.apk`, che è l'unico posto dove il difetto sia mai stato visto.
- **Dopo:** altre lingue oltre IT/EN, e il dizionario binario indicizzato se il formato testo diventasse stretto.

- 📌 **Code della 2.3 — entrambe chiuse il 2026-08-04.**

  1. ✅ **CHIUSA — 2026-08-04. Decisione dell'utente: le lettere singole restano escluse.**
     `LearnedWordsEngine.isLearnable` non cambia (≥ 2 caratteri): `e`, `è`, `é` hanno peso solo
     dal corpus — fisso — e l'ordine del tasto lo decide `SingleLetterEngine` dal tastierino,
     non la frequenza.

     **La motivazione originale era scaduta, e la regola è stata rimotivata invece che tolta.**
     Era: *"una parola imparata batte l'intero corpus, quindi scrivere `è` una volta demoterebbe
     `e` per sempre"* — vero con `BASE_WEIGHT` 1.000.000 contro i 48.146 di `e`, falso adesso che
     una `è` imparata una volta peserebbe **200**. Il motivo per cui la regola resta è
     **diverso**: la **prevedibilità** di un tasto premuto da solo.

     **Misurato prima di decidere**, sul tasto `2` dove `a` pesa 15.038: una `b` imparata una
     volta peserebbe `200 + 50.000` di spinta recente = **50.200**, e siccome
     `SingleLetterEngine` confronta per peso le lettere semplici dello stesso tasto (sopra
     `REAL_WORD_WEIGHT` = 1.000), il tasto leggerebbe **`b a c à` per un'ora**, poi 5.200 entro
     il giorno, 700 entro la settimana, 200 dopo. Cioè il sintomo della 2.2 — quello segnalato
     due volte dall'utente — non più permanente ma **ricorrente a ogni uso**. In cambio, il
     guadagno era quasi nullo: le lettere singole che sono parole vere il corpus le ordina già
     bene, e servirebbero ~50 usi perché l'abitudine da sola (senza spinta) superi `a`.

     **Quel che la regola non porta:** la difesa «l'accento sta dopo la sua lettera semplice»
     **non** dipende da qui — `SingleLetterEngine` ordina su quel flag prima ancora di guardare
     un peso, quindi reggerebbe anche togliendo la regola. Il commento di `isLearnable`, che
     raccontava ancora la motivazione vecchia, è stato riscritto con questa.

  2. ✅ **CHIUSA — Step 2.4 (2026-08-04).** *Le elisioni potevano infilare spazzatura nel
     dizionario.* **Riprodotta sull'emulatore prima di correggere**, e la riproduzione ha
     smentito la stima scritta qui: non era affatto innocua. Scritto `l` + apostrofo + `a` +
     spazio, la sequenza `52` proponeva **`l'a` prima di `la`** — non in coda, come diceva il
     conto «200 contro 17.675», perché quel conto dimenticava la spinta recente della 2.3, che
     per un'ora mette la parola appena imparata sopra tutto il corpus.

     ⚠️ **La correzione che questo promemoria suggeriva era sbagliata, ed è la cosa più utile
     da ricordare.** «Chiedere che la coda sia essa stessa una parola (≥ 2 caratteri)» avrebbe
     ucciso **`c'è`** — che ha esattamente la forma di `l'a`: una lettera di testa, una di coda.
     Lo ha fatto notare l'utente. E `c'è` è proprio la parola da proteggere: **nessun corpus
     contiene apostrofi** (verificato su `it.txt` e `en.txt`), quindi essere imparata è l'unico
     modo in cui possa mai comparire fra i candidati. Provata sull'emulatore, si riscrive con
     **due tasti** dalla sequenza `23`.

     **La discriminante vera non è la lunghezza ma l'accento:** le elisioni corte che l'italiano
     ha davvero — `c'è`, `n'è`, `s'è`, `v'è` — finiscono tutte per vocale accentata, mentre la
     spazzatura è sempre una vocale semplice (`l'a`, `l'e`, `l'o`). `Elision.join` rifiuta ora
     di unire una coda di un solo carattere non accentato.
- 📝 **Appunti dell'utente dalla prova reale (30/07 sera).** **Ordine deciso dall'utente: prima le tre segnalazioni (1, 2, 3), poi la vibrazione (4).** **Tutti chiusi il 31/07**: 1 → Step 1.18, 3 → Step 1.19, 4 → Step 1.21. Il punto 2 **non si riproduce** e attende un riscontro sul telefono.

  1. ✅ **RISOLTO — Step 1.18.** *La parola composta a mano non risulta imparata.*

     **La premessa dell'appunto era sbagliata, e questo ha cambiato la diagnosi.** `farla`
     **è** nel dizionario (`it.txt` riga 1534, peso 61) ed è il **primo candidato** della sua
     sequenza `32752`, davanti a `darla` (4): non andava composta a mano affatto. `farà`
     invece **non c'è** — c'è solo `fara` (76), quindi al primo passo lo schermo mostrava
     `fara`. Seguendo i passi dettati alla lettera l'apprendimento funzionava già.

     Il difetto vero stava nel passo **«tornare sulla fine della parola»**: l'utente ha
     confermato di aver **toccato il campo** per spostare il cursore. `onUpdateSelection`
     non azzerava la composizione, così il buffer di cifre della tastiera e la regione in
     composizione dell'editor restavano disallineati, e quel che si imparava era il
     frammento sbagliato.

     La correzione va oltre il reset, perché la dinamica che l'utente vuole è **riprendere
     la parola già scritta**, nei due versi (da `farla` a `farà` e viceversa): vedi
     `docs/FUNCTIONAL.md` §4, *Riprendere una parola già scritta*.

  1. ~~La parola composta a mano non risulta imparata.~~ *(testo originale dell'appunto, tenuto per riferimento.)* Serviva `farla`, che nel dizionario non c'è.
     **Riproduzione esatta, dettata dall'utente** — da eseguire *per prima cosa*, prima di toccare il codice:
     1. digitare la sequenza che dà **`farà`** e lasciarla comparire;
     2. **cancellare la `à`** (backspace: fa pop della coppia cifra+lettera);
     3. **tornare sulla fine della parola** e **aggiungere `la` al posto di `à`**, ottenendo `farla`;
     4. premere **spazio**.

     *Richiesta:* a quel punto la parola va **memorizzata**. In alternativa, o in aggiunta: **mostrare la parola scritta per intero anche fra i candidati**, così cliccandola la si conferma — e la si impara — senza passare dallo spazio.
     **Nota per chi riprende:** il percorso di apprendimento **esiste già** — `onSpace()` → `commitCurrentWord()` → `learn(word)`, e `learn` scarta solo le parole di una lettera. Quindi non è una funzione mancante ma un motivo per cui in *quel* flusso non ha effetto. Piste: cosa restituisce `currentPreview()` dopo un backspace che ha fatto pop di una coppia (`state.isForcing()` o no), e se la parola imparata vinca poi il ranking sulla sua sequenza. La seconda richiesta tocca invece la costruzione della lista candidati: oggi la parola composta a mano si vede nel campo ma **non è una voce cliccabile** della barra.

     **Domanda dell'utente, verificata: cliccare un candidato non passa da `onSpace()`.** Sono due percorsi indipendenti — `onPickCandidate()` chiama `learn()` da sé e poi aggiunge lo spazio con `insertProvisionalSpace()` (spazio *provvisorio*, che viene ritirato se segue punteggiatura), mentre `onSpace()` impara via `commitCurrentWord()`. `learn()` ha quindi due chiamanti distinti. **Conseguenza pratica:** la seconda opzione è **autosufficiente** — se la voce aggiunta è un `Candidate` vero, cliccarla impara già di suo, quindi è una via d'uscita indipendente dalla correzione del difetto, non un'aggiunta che la aspetta.
  2. ⚠️ **NON RIPRODOTTO — Step 1.19.** *Maiuscola sbagliata dopo l'apostrofo: scriveva `l'Aveva` invece di `l'aveva`.*

     Provato sull'emulatore in un campo di prosa vero (messaggi, con gli aiuti attivi):
     esce **`J'aveva`, minuscolo**, e subito dopo l'apostrofo i tasti tornano minuscoli,
     cioè `updateAutoShift` non arma nulla. Il motivo è strutturale: `afterOpeningAtSentenceStart`
     guarda **cosa precede l'apostrofo** e con `l'` trova una lettera, non un fine frase.
     Scatta solo se l'apostrofo segue un punto o apre il campo — il caso virgoletta, per cui
     è stato scritto. Anche `getCursorCapsMode` di Android scavalca l'apostrofo e trova la
     lettera. Nessuna delle due strade produce `l'Aveva`.

     **Ipotesi: sul telefono c'era un APK vecchio** — questo file segnalava da sé che gli
     Step 1.15 e 1.16, quelli che hanno riscritto le regole di maiuscole, non erano mai
     stati provati sul telefono. Nessuna correzione scritta: sarebbe stata codice contro un
     difetto non osservato, col rischio di rompere il caso virgoletta che funziona. Lo
     Step 1.19 rende comunque esplicita la regola.

     🔁 **Riprovato sulla 2.4 (2026-08-04), in due posizioni invece di una — non riprodotto.**
     - *A inizio campo:* `l` + apostrofo + `aveva` → esce **`l'aveva`**, minuscolo, e subito
       dopo l'apostrofo i tasti sono già minuscoli.
     - *A inizio frase* — il caso che mancava alla prova del 31/07, ed è quello dove la
       maiuscola automatica è **armata**: scritto `ciao. ` i tasti diventano maiuscoli, poi
       `l` + apostrofo + `aveva` dà **`ciao. L'aveva`**. La maiuscola va sulla `l`, dove
       deve, e si consuma lì: dopo l'apostrofo i tasti tornano minuscoli invece di
       riarmarsi. Era l'unico meccanismo plausibile per produrre `l'Aveva`, ed è escluso.

     **Cosa questo non prova.** La segnalazione originale viene dal **telefono**, non
     dall'emulatore, e su un APK di cui non si conosce la versione. L'emulatore può solo
     continuare a dire che il difetto non c'è: a chiudere il punto è un riscontro sul
     telefono con `dauxpal-3.7-debug.apk`.

  3. ✅ **RISOLTO — Step 1.19.** *Elisioni come parola unica.*

     Confermato nel database prima di correggere: scritto `j'aveva` + spazio, risultava
     imparata **`aveva`** da sola e `j'aveva` no. Ora `Elision` distingue per posizione
     l'apostrofo che unisce due parole da quello usato come virgoletta, e la parola viene
     imparata **intera**. `sequenceFor` salta l'apostrofo dell'elisione, così `l'aveva` sta
     nel dizionario personale sotto `528382` e alla seconda scrittura si ottiene digitando
     **solo le lettere**, con l'apostrofo scritto dalla tastiera.
  4. ✅ **RISOLTO — Step 1.21.** Scelta dell'utente: **durata regolabile**, quindi il
     `Vibrator` con durata esplicita e non `performHapticFeedback`. Il prezzo della scelta
     è che rispettare l'impostazione di sistema diventa un nostro compito, ed è fatto
     (`HAPTIC_FEEDBACK_ENABLED`, verificato spegnendolo e riaccendendolo). Testo originale
     dell'appunto qui sotto.

  4. ~~**Manca il ritorno tattile: la tastiera non vibra sotto il dito.**~~ L'utente: *"mi sta mancando tanto non sentire il tasto quando schiaccio"* — quindi **non** è una rifinitura da rimandare, va fatta presto; la durata **regolabile** arriverà con la schermata impostazioni.
     **Nota per chi riprende:** il posto giusto è `KeyViewFactory`, dove ogni tasto ha già il suo `ACTION_DOWN`: si scrive una volta sola e vale per tutte le superfici (T9, simboli, emoji, popup). Una decisione da prendere subito, perché cambia l'implementazione — `performHapticFeedback(KEYBOARD_TAP)` rispetta l'impostazione di sistema e il profilo del telefono ma **non** ha durata regolabile; il `Vibrator` con durata esplicita è calibrabile ma deve tacere da sé quando l'utente ha spento il feedback tattile di sistema. Da considerare anche la ripetizione del backspace tenuto premuto (non deve diventare un ronzio continuo) e l'apertura del popup, che di solito ha un colpo suo.

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
> bash tools/dev.sh install           # build + installa (lascia anche l'APK versionato)
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
- [x] `DictionaryEngine` (interfaccia) + `CorpusDictionaryEngine` da asset di test
- [x] Modalità predittiva T9 + barra suggerimenti orizzontale
- [x] **Colonna di disambiguazione manuale posizionale** (stack di coppie cifra/lettera) — `ComposeState` + `DisambiguationColumnView`
- [x] Backspace = pop della coppia (cifra+lettera insieme)
- [x] Estendere/correggere parola (push nuova coppia) — stessa operazione, nessuna distinzione di codice
- [x] Apprendimento persistente (Room) + salvataggio automatico su spazio (Step 1.5)
- [x] Integrazione corpus Leipzig italiano (50k parole, `assets/dict/it.txt`, caricato in background)
      → per ora **testo**, non binario: a 50k parole il parse in background è rapido; il formato
        binario indicizzato resta un'ottimizzazione futura (non necessaria a questa dimensione).
- [x] Test unitari: `T9Keypad.sequenceFor`, `CorpusDictionaryEngine`, `ComposeState`,
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

- [x] `BilingualDictionaryEngine` (concatenazione IT→EN; `CorpusDictionaryEngine` serve entrambe le lingue, quindi nessun `EnglishDictionaryEngine` separato)
- [x] Corpus inglese affiancato al corpus italiano (`assets/dict/en.txt`, 36.560 parole)
- [x] Criteri di accettazione Fase 2: **nessuna regressione sulla v1** — verificato, `2272` dà la lista identica a prima dell'inglese

## 🧭 Fase 3 — decisioni di piano (2026-08-04)

> Pianificata con l'utente **prima** di scrivere codice, come lo Step 1.12. Questa sezione
> registra le decisioni e il perché; la lista qui sotto le mette in ordine.

**Il problema della lista.** Le voci erano cresciute per accumulo — ogni cosa rimandata finiva
lì — quindi mescolavano preferenze che aspettavano due righe di UI, funzioni nuove intere
(QWERTY è una seconda tastiera, non un'impostazione) e rifiniture. In ordine di lista non aveva
una forma; raggruppata sì.

**Decisione 1 — come arriva un'impostazione di *misura* alla tastiera: con un'anteprima viva.**
Per un interruttore basta ciò che si fa già (si scrive nelle preferenze, il servizio rilegge
all'apertura). Per **altezza** e **dimensione del testo dei candidati** no: non si sceglie una
misura che non si vede, e uscire-guardare-rientrare è il modo in cui si sbaglia tre volte.

Qui il codice ha un vantaggio che non è ovvio: **`KeyboardView` è un normale `FrameLayout`** che
prende un `Context` e delle lambda, e non dipende da `InputMethodService`. La schermata può
quindi mostrare una tastiera **vera**, con le callback a vuoto, che si ridimensiona mentre si
muove lo slider. Altrove sarebbe un progetto; qui è uno step (3.2).

**Decisione 2 — il «salvataggio sicuro» resta in lista, ma con la motivazione annotata.** Vedi
la voce apposita più sotto: la 2.3 ha risolto lo stesso problema in un altro modo, e chi la
riprende deve saperlo invece di implementarla per inerzia.

**Decisione 3 — QWERTY e ampliamento delle emoji restano fuori dalla Fase 3.** Non perché non
contino: sono due funzioni nuove con decisioni proprie da prendere, e in mezzo alle impostazioni
farebbero durare la fase il triplo.

**Ordine concordato:** 3.1 interruttori e via d'accesso → 3.2 anteprima viva → 3.3 altezza e
testo → 3.4 cursore sulla barra spazio → 3.5 colonna e preferiti → 3.6 dizionario personale →
3.7 rifiniture.

---

## Fase 3 — Impostazioni, ergonomia, rifiniture

- [ ] **Il backspace non sta nello stesso posto su tutte le superfici** (notato dalla review della
      3.7, non toccato). Sulla T9 è ora in basso a destra; nelle pagine simboli sta in fondo a
      destra della **terza** riga (`SymbolLayout`), e nel pannello **emoji** è in basso a
      **sinistra**, accanto ad `abc` (`EmojiLayout`) — il caso peggiore. L'argomento della 3.7
      (il pollice cerca dove ha imparato) vale identico lì: cambiando superficie il tasto si
      sposta sotto la mano. Da decidere insieme, perché quelle righe hanno forme diverse dalla
      colonna della T9 e non basta riordinare una lista

- [ ] 🎨 **Icona vera al posto del segnaposto.** Dallo Step 3.5 è una "D" bianca su indigo,
      disegnata a mano come prima era una "T" — «per l'icona poi sistemiamo, troveremo un'icona»
      (utente, 04/08)
- [x] **Nome dell'app: DauxPal** (richiesta dell'utente, 04/08) — **Step 3.5**. L'`applicationId`
      **non** va cambiato: si porterebbe via il dizionario personale
- [x] **APK versionato a ogni build**, non solo con `dev.sh apk` — **Step 3.5**
- [x] **Larghezza dei tasti e lato dello schermo** (idea dell'utente, 04/08) — **Step 3.4**. Da
      non confondere con la riga qui sotto: questa sposta **la tastiera dentro lo schermo**,
      quella sposta **la colonna dentro la tastiera**. Sono complementari, e un mancino
      probabilmente vorrà entrambe
- [ ] Posizione colonna sinistra/destra configurabile (dentro la tastiera — vedi la riga sopra)
- [x] Simboli preferiti a stack vuoto (configurabili) — anticipati allo Step 1.9
- [ ] Riordino dei preferiti per trascinamento (oggi si riordina scambiandoli, vedi 1.9)
- [ ] Numero di preferiti configurabile (oggi fisso a `FavouriteSymbols.COUNT` = 7)
- [x] **Vibrazione alla pressione dei tasti** + **durata regolabile** — anticipata allo Step 1.21
      (richiesta dell'utente dopo la prova reale). La durata è già un'impostazione
      (`KeyboardSettings.hapticMs`, default 18 ms). Lo slider è arrivato nello **Step 3.1**, e
      vibra quando lo lasci andare: è l'unica impostazione che si sceglie sentendola
- [x] Altezza tastiera regolabile con riproporzionamento uniforme — **Step 3.3**
- [x] Dimensione del testo dei candidati regolabile — **Step 3.3**, sul seam che aspettava dallo
      Step 1.25 (`SuggestionBarView.textSizeSp`)
- [x] **Anteprima viva della tastiera nella schermata impostazioni** — **Step 3.2**. È
      `KeyboardView`, la stessa classe che mostra l'IME, con le callback a vuoto
- [x] Vocali accentate (nella colonna) — anticipate allo Step 1.10
- [x] Pannello emoji base — anticipato allo Step 1.10
- [x] Long-press tasto 1 → simboli costosi altrove — **fatto nello Step 1.12**
- [x] Popup long-press sul tasto con lettere+accentate — **fatto nello Step 1.12**
- [ ] **Scorrimento del cursore trascinando sulla barra spazio** (idea utente): con la
      scrittura predittiva riposizionare il cursore è utile. Occupa il long-press dello
      spazio, oggi libero proprio per questo (lo `0` è già sulla virgola)
- [x] Maiuscola automatica a inizio frase (`getCursorCapsMode`) — fatta nello Step 1.13
- [x] Spazio automatico dopo la scelta di un candidato — fatto nello Step 1.13
- [x] Interruttori per maiuscola/spazio automatici nella schermata impostazioni — **Step 3.1**
- [x] **Via d'accesso alle impostazioni dalla tastiera** (idea dell'utente, 04/08): una rotellina
      sopra la colonna di disambiguazione, all'altezza dei candidati — **Step 3.1**. Prima
      l'unica strada era l'icona nel launcher, cioè uscire dall'app in cui si sta scrivendo
- [x] Modalità numerica/simboli dedicata (`12#`, due pagine QWERTY) — anticipata allo Step 1.8
- [ ] **QWERTY come layout alternativo alla T9** (idea utente): un nuovo `KeyGrid` +
      voce in `KeyboardMode`; vista e plumbing già pronti dallo Step 1.8
- [x] Vocali accentate via long-press — **fatto nello Step 1.12**
- [ ] Schermata gestione dizionario personale (lista + cancella)
- [x] Long-press su candidato → rimuovi dal dizionario — anticipato alla 2.3 (solo le parole
      personali; sul corpus la barra dice che non c'è niente da dimenticare)
- [ ] Opzione **"salvataggio sicuro"** (specifica §4: una conferma o un ritardo prima di salvare
      le parole *mai viste prima*, «per ridurre il rischio di errori»).

      ⚠️ **La motivazione è stata erosa dalla 2.3 — deciderlo, non implementarlo per inerzia.**
      Quel rischio oggi è già reversibile: il peso proporzionale più la spinta recente fanno
      **riemergere** la parola sbagliata dove la vedi, e la pressione prolungata la cancella. Una
      conferma a ogni parola nuova pagherebbe con un attrito continuo qualcosa che costa un
      gesto. È lo stesso schema di `isLearnable` (coda 1 della 2.3): una voce sopravvissuta alla
      propria motivazione. L'utente ha scelto di **tenerla aperta e decidere più avanti**
      (04/08). Se si toglie, va aggiornata la specifica, non solo questa riga.
- [ ] **Pannello emoji da ampliare** (osservazione dell'utente, 04/08: *"le emoji sono
      piuttosto ridotte"* — verificato, sono **32**, una pagina sola). Oggi è il pannello base
      anticipato allo Step 1.10, giusto per non lasciare morto il tasto `☺`. Mancano categorie,
      riga dei recenti, ricerca e toni della pelle. Da decidere **come**, prima che quali: a
      pagine come i simboli, o a categorie con una barra propria? I recenti sono la cosa che si
      usa davvero, e sono anche l'unica che richiede di memorizzare qualcosa.

- [ ] 🐞 **Tre emoji sono disegnate più piccole delle altre** (trovato il 04/08, non corretto
      su scelta dell'utente: nota di fine giornata, non urgente). `❤️`, `✌️` e `☀️` escono a
      **18sp** invece di 22, e a occhio si vedono più piccole delle vicine — rapporto misurato
      sullo screenshot ~0,81, cioè esattamente 18/22.

      **Diagnosi già fatta:** in `KeyViewFactory.labelSize` i glifi singoli prendono 22sp con la
      regola `codePointCount(0, length) == 1`, il resto cade su 18sp. Quelle tre portano il
      **selettore di variazione `U+FE0F`** — quello che chiede ad Android di disegnarle a colori
      invece che come simbolo di testo — quindi contano **due** code point e finiscono nel ramo
      sbagliato. Il commento lì sopra parla delle coppie surrogate (`🔥`, un code point solo) ma
      il selettore di variazione non era stato considerato.

      **Correzione:** una riga — contare i code point ignorando `U+FE0F` (e `U+FE0E`, che è la
      variante testuale usata dal tasto `☺︎` stesso).
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

### 2026-08-04 — Step 3.7: il backspace va dove il pollice lo cerca
**Richiesta dell'utente:** *"puoi invertire il tasto di cancellazione con quello delle emoji?
Avendo usato la Gboard ultimamente si trova in una posizione più simile e riesco ad usare la
memoria muscolare meglio"*.

Fatto: `☺` in cima, `⇧` in mezzo (non era in discussione), `⌫` in basso a destra. Tre righe
riordinate in `T9Layout.rightColumn`, più i due schemi ASCII che le disegnavano al contrario.

**Ed è un buon argomento, non una preferenza.** Il backspace è il tasto più premuto dopo lo
spazio, e dove sta non è una questione di gusto: è una questione di cosa il pollice ha già
imparato altrove. Una tastiera che lo mette dove nessun'altra lo mette costa una correzione ogni
volta che la mano ha ragione e la disposizione è diversa.

**File modificati:** `model/T9Keypad.kt`, `ui/T9BodyView.kt` (lo schema nel commento),
`docs/FUNCTIONAL.md` (lo stesso schema), `app/build.gradle.kts` (3.6 → 3.7).

**Verificato (2026-08-04):** 166 test verdi e guardato sull'emulatore — screenshot
`docs/screenshots/step-3.7-backspace-in-basso.png`. Nessun test nuovo: è l'ordine di una lista di
tasti, e ciò che va verificato è dove si trovano sullo schermo.

### 2026-08-04 — Step 3.6: il maiuscolo in mezzo alla parola
**Segnalazione dell'utente:** aveva scritto `xD`, premuto spazio, e la parola non risultava
memorizzata. Poi: *"volevo memorizzare xD con la D maiuscola"*.

**Indagando, il problema era più a monte di come sembrava: la tastiera non sapeva proprio
scrivere una maiuscola dentro una parola.** `ShiftState.apply` fa due cose sole — `ONCE`
capitalizza la **prima** lettera, `LOCK` le capitalizza tutte. Non esisteva uno stato «questa
lettera qui maiuscola», quindi premendo `⇧` prima della D si otteneva `Xd`. Il che spiega anche
perché la parola non fosse stata imparata: per avere `xD` nel campo l'utente aveva
necessariamente scritto **due pezzi separati**, e la tastiera stava componendo solo `D`.

Quindi due cose, e la prima è una scelta di progetto: **poterlo scrivere** e **poterlo
memorizzare**.

**Scriverlo — `⇧` vale per la lettera, ma solo mentre si forza** (scelta dell'utente fra tre
opzioni). In T9 le lettere di solito le decide il dizionario, e lì «la prossima lettera» non
vuol dire niente: `⇧` deve continuare a valere per la parola. Ma quando si scelgono le lettere
**a mano dalla colonna** si sta dettando la parola una lettera per volta, e lì la domanda ha una
risposta. `ComposeState` tiene ora l'insieme delle posizioni capitalizzate a mano, e la
posizione 0 **non** è mai fra queste: la prima lettera è affare di `ShiftState` (maiuscole
automatiche, nomi propri, shift-lock), e due meccanismi che rispondono dello stesso carattere è
il modo in cui finiscono per contraddirsi.

Un `⇧` singolo si consuma sulla lettera scelta, come su una macchina da scrivere. E mentre si
forza non capitalizza più all'indietro la prima lettera: si vedeva `X` appena premuto `⇧`, che
poi tornava minuscola all'arrivo della lettera dopo — la parola lampeggiava in una forma diversa
da quella che si stava scegliendo.

**Memorizzarlo — la chiave resta minuscola, la forma scritta le viaggia accanto.** `Casa` e
`casa` devono restare **una** parola per lookup, deduplica e conteggio, quindi la chiave non
cambia; il dizionario ricorda in più *come si scrive*, e solo quando c'è qualcosa da ricordare.

**La regola è: una maiuscola che nessuna regola avrebbe potuto metterci**, cioè dopo il primo
carattere. Tutto il resto è già spiegato da qualcosa che fa la tastiera, e memorizzarlo
significherebbe memorizzare il comportamento della tastiera come se fosse un fatto sulla parola:
- `casa` — niente da dire.
- `Casa` — la maiuscola iniziale la producono un punto, un campo nuovo o un nome proprio.
  Ricordarla farebbe tornare maiuscola per sempre ogni parola che una volta ha aperto una frase.
- `CIAO` — tutto maiuscolo è shift-lock, cioè un tono di voce, non un'ortografia. Escluso.
- `xD`, `iPhone`, `McDonald`, `LaTeX` — tenuti esattamente come scritti.

E una forma che non dice niente **non cancella** una che diceva qualcosa: scrivere `xd` a inizio
frase arriva come `Xd`, la cui maiuscola è ordinaria, e senza questa cautela spazzerebbe via lo
`xD` imparato apposta.

**La migrazione del database è scritta a mano, e non è pedanteria.** La colonna nuova avrebbe
potuto arrivare con `fallbackToDestructiveMigration`, che è una riga — e che cancellerebbe il
dizionario personale di chiunque ne avesse già uno. È l'unico dato di quest'app che l'utente non
può recuperare, perché lo ha costruito scrivendo. Le righe esistenti prendono `NULL`, che è
esattamente giusto: sono state imparate quando le maiuscole non si ricordavano.

**File modificati:** `input/ComposeState.kt` (le posizioni capitalizzate),
`service/T9ImeService.kt` (`onPickLetter`, `renderShift`, `alternatesFor`, `currentPreview`),
`engine/LearnedWordsEngine.kt` (`displayFormOf` e la forma scritta nell'indice),
`learning/` (colonna `display`, versione 2 del database, migrazione),
`app/build.gradle.kts` (3.5 → 3.6). Test: `LearnedWordsEngineTest` +6.

**Verificato (2026-08-04):** 166 test verdi, e sull'emulatore il ciclo intero — premuto `9`,
forzata la `x` dalla colonna, premuto `⇧`, forzata la `D`: nel campo compare **`xD`**; spazio; e
la sequenza `93` la ripropone **`xD`**, con la maiuscola. Screenshot:
`step-3.6-xd-scritta.png`, `step-3.6-xd-riproposta.png`.

**Un test è stato reso più severo per via del cambio:** il fake store di
`LearnedWordsEngineTest` buttava via `lastUsed` in scrittura, quindi nessun test poteva
accorgersi se andava perso. Ora lo conserva, e l'asserzione lo verifica.

### 2026-08-04 — Step 3.5: l'app si chiama DauxPal, e ogni build lascia un APK versionato
**Due richieste dell'utente, arrivate insieme mentre se ne stava indagando un'altra.**

**1. Il nome.** *"vorrei che l'app al posto di t9 si chiamasse DauxPal"*. `app_name` e
`ime_label` derivano da un solo posto (`resValue` in `build.gradle.kts`, dallo Step 1.20),
quindi è stata una riga — più tre cose che sarebbero rimaste indietro:
- Il **titolo della schermata impostazioni**, che scriveva "T9 3.4 — Impostazioni" a mano.
- L'**icona**, una "T" disegnata a mano *per T9*: la lettera è il nome, quindi rinominare
  l'app senza ridisegnarla avrebbe lasciato il vecchio nome sulla schermata principale. Ora è
  una "D" — **segnaposto dichiarato**, l'utente ne troverà una vera.
- Il **prefisso degli APK**, da `t9-` a `dauxpal-`.

**L'`applicationId` resta `com.daux.t9keyboard`, e non è una dimenticanza.** Cambiarlo fa
disinstallare e reinstallare l'app per Android, e con essa se ne andrebbe il **dizionario
personale** — cioè tutto ciò che l'utente ha insegnato scrivendo. Il nome del pacchetto non si
vede da nessuna parte; il dizionario si sente a ogni parola. "T9" del resto continua a indicare
la *tecnica*, che non è cambiata: ad avere un nome proprio è il prodotto.

**2. Gli APK versionati.** *"gli apk andavano versionati in modo da non avere sempre solo
app-debug"*. La copia col nome versionato esisteva dallo Step 1.20 ma la faceva **solo**
`dev.sh apk` — e in pratica si usa `install`, quindi ci si ritrovava sempre e solo
`app-debug.apk`: un nome che non dice quale build sia e che ogni ricostruzione sovrascrive. La
copia è ora una funzione chiamata da entrambi i comandi.

**File modificati:** `app/build.gradle.kts` (nome e versione 3.4 → 3.5),
`res/drawable/ic_launcher.xml`, `settings/SettingsActivity.kt` (il titolo), `tools/dev.sh`.

**Verificato (2026-08-04):** 160 test verdi; `install` lascia `dauxpal-3.7-debug.apk` accanto ad
`app-debug.apk`; nel selettore tastiere di Android si legge **"DauxPal 3.5"** con l'icona "D".

**Nota di metodo:** l'icona è rimasta una "T" per due build. Non era il codice — è la **cache
delle icone di sistema**: si aggiorna con `am force-stop com.android.settings`, non
reinstallando. E soprattutto **non** con `pm clear` sulla propria app, che cancellerebbe il
dizionario personale (la lezione della 2.2).

### 2026-08-04 — Step 3.4: i tasti si stringono verso un lato (idea dell'utente)
**Domanda dell'utente:** *"è fattibile inserire un cursore per ottimizzare la larghezza? Il
pannello può rimanere a schermo intero ma per schermi larghi potrebbe stringere un po' i tasti
verso destra (ed un pulsante che permette di fare l'opposto stringendo verso sinistra in caso di
uso mancino)"*. Fattibile, e più semplice di quanto sembrasse.

- **Larghezza tasti**, dal **60% al 100%** dello schermo, default **100%**.
- **Interruttore «Tasti a sinistra (per mancini)»**, che non significa niente a tutto schermo.

**Perché è costato poco:** tutto ciò che si vede sta dentro un unico figlio, `content`, in un
`FrameLayout`. Stringere è dargli una larghezza frazionaria e una `gravity`. Il pannello resta a
tutto schermo perché lo sfondo è della `KeyboardView`, non di `content` — e questo è il punto
della richiesta: **non rimpicciolisce la tastiera, la avvicina al pollice**. Colonna, tasti, riga
dello spazio, barra dei candidati e rotellina si stringono insieme, perché sono tutti figli di
`content` che se la dividono a pesi.

**Il difetto che sarebbe arrivato dopo, corretto insieme.** `positionPopup` teneva i popup
long-press dentro i bordi della **`KeyboardView`**. Finché i tasti erano larghi quanto lei le due
cose coincidevano; con i tasti stretti, un popup aperto sul tasto di bordo poteva piazzarsi nella
fascia vuota accanto, staccato dal tasto che lo aveva aperto. Il limite è ora `content.left` /
`content.right`. Una riga, ma senza pensarci si sarebbe scoperta solo per caso.

**Il default è 100% di proposito.** Stringere costa precisione — gli stessi tasti con meno spazio
— quindi vale solo per chi non arriva dall'altra parte dello schermo, e può dirlo. Una tastiera
che arriva già stretta sarebbe peggio per tutti quelli che non l'hanno chiesto.

**Da non confondere con «posizione colonna sinistra/destra»**, che resta aperta in Fase 3: quella
sposta la colonna di disambiguazione **dentro** la tastiera, questa sposta la tastiera **dentro
lo schermo**. Sono complementari.

**File modificati:** `settings/KeyboardSettings.kt` (`keyboardWidthPercent`, `keyboardOnLeft`),
`ui/KeyboardView.kt` (`applyContentWidth`, il limite dei popup), `settings/SettingsActivity.kt`,
`app/build.gradle.kts` (3.3 → 3.4).

**Verificato (2026-08-04):** 160 test verdi, e sull'emulatore — cursore e interruttore muovono
l'anteprima (a destra e a sinistra, col pannello sempre a tutto schermo), e la tastiera **vera**
si stringe a sinistra riaperto un campo in Messaggi. Il popup long-press sul tasto di bordo resta
**dentro i tasti** invece di scivolare nella fascia vuota — catturato tenendo premuto con
`input motionevent DOWN`/`UP` separati, perché `input swipe` rilascia prima che lo screenshot
faccia in tempo. Screenshot: `step-3.4-tasti-stretti-a-destra.png`,
`step-3.4-tasti-stretti-a-sinistra.png`, `step-3.4-popup-dentro-i-tasti.png`.

**Nota:** su questo emulatore (1280px, 427dp) il guadagno è modesto — la tastiera è già
raggiungibile. Si vedrà sull'S25 Ultra, che è il caso per cui la funzione è stata chiesta.

### 2026-08-04 — Step 3.3: altezza e testo dei candidati, scelti guardandoli cambiare
**Due cursori sopra l'anteprima della 3.2**, che si muove sotto le dita mentre li sposti.

- **Altezza tastiera**, dal **22% al 40%** dello schermo, default **29%**.
- **Testo dei candidati**, da **12 a 24 sp**, default **17 sp**, sul seam che aspettava dallo
  Step 1.25 (`SuggestionBarView.textSizeSp`).

**Il riproporzionamento uniforme non è costato nulla, ed è il dividendo di una scelta vecchia.**
La disposizione è fatta di **pesi**, non di misure: ogni tasto è una quota di qualunque altezza
venga fuori, quindi muovere il totale riproporziona tutto insieme invece di stirare una riga. Il
lavoro è stato togliere `BODY_HEIGHT_FRACTION` da costante a preferenza letta a ogni `onMeasure`.

**Il default è cambiato di tre decimi di punto, e va detto invece che nascosto.** Gli Step 1.25 e
2.5 avevano misurato **28,7%**; un cursore si muove in punti percentuali interi e una scritta
deve poterlo dire, quindi il default è **29%** — circa 3px più alta su uno schermo da 2856, meno
della riga fra due tasti. Il valore misurato resta scritto nel commento di `KeyboardView`,
insieme a come ci si era arrivati.

**I due cursori si comportano diversamente da quello della vibrazione, di proposito.** Quello
vibra al *rilascio*, perché un colpo per pixel sarebbe un sonaglio; questi scrivono e ridisegnano
a **ogni movimento**, perché una dimensione va *guardata cambiare* — metà del valore
dell'anteprima è vedere i tasti passare per l'altezza che stavi per scegliere. Il prezzo è una
scrittura nelle `SharedPreferences` per pixel di corsa: onesto e piccolo (`apply()` è asincrona),
e l'alternativa sarebbe un disaccordo fra ciò che si vede e ciò che è memorizzato per tutta la
durata del trascinamento.

**File modificati:** `settings/KeyboardSettings.kt` (`bodyHeightPercent`, `candidateTextSp` e i
loro limiti), `ui/KeyboardView.kt` (`applySizeSettings`, `onMeasure`), `service/T9ImeService.kt`
(rilettura al ritorno in un campo), `settings/SettingsActivity.kt` (il costruttore di cursore
condiviso), `app/build.gradle.kts` (3.2 → 3.3).

**I limiti hanno una ragione, non sono numeri tondi.** Il minimo del 22% sta sopra il pavimento
che lo Step 1.25 aveva trovato scendendo troppo e dovendo risalire nella 2.5; il massimo del 40%
è dove i tasti tornano **più alti che larghi**, la forma che la 1.25 aveva misurato e scartato.
Il tetto di 24 sp è quanto sta ancora nella striscia dei candidati, che **non** cresce col testo:
`BAR_DP` è fisso, e la 1.25 aveva dimensionato le due cose insieme apposta.

**Verificato (2026-08-04):** 160 test verdi, e sull'emulatore in tre passaggi — i cursori muovono
l'anteprima (al 40% e 24 sp la barra tiene quattro candidati invece di sei, che è esattamente il
compromesso per cui il tetto è lì); e le impostazioni arrivano alla **tastiera vera**, non solo
all'anteprima, riaperto un campo in Messaggi. Screenshot:
`docs/screenshots/step-3.3-cursori-dimensioni.png` e
`docs/screenshots/step-3.3-anteprima-al-massimo.png`.

**Nota di metodo:** dopo un `install` l'emulatore torna a Gboard, e per un momento è sembrato un
crash della tastiera. Va rieseguito `bash tools/dev.sh ime` — è già scritto nel blocco dei
comandi, ma vale la pena ricordarlo qui perché il sintomo (un pannello mobile bianco al posto
della tastiera) non somiglia affatto alla sua causa.

### 2026-08-04 — Step 3.2: nella schermata impostazioni c'è una tastiera vera
**Non un disegno e non un mock-up:** in fondo alla schermata sta `KeyboardView`, la stessa
classe che mostra l'IME, con le callback che non vanno da nessuna parte.

**Che si potesse fare è l'incidente fortunato di un'architettura presa bene tempo fa.**
`KeyboardView` è un normale `FrameLayout` che prende un `Context` e delle lambda, e non ha mai
saputo nulla di `InputMethodService`. Una tastiera che potesse esistere solo dentro il proprio
servizio avrebbe reso questo step una riscrittura.

**A cosa serve:** altezza della tastiera e dimensione del testo dei candidati (3.3) sono
**misure**, e una misura scelta alla cieca è scelta male — muovi un cursore, esci, apri un campo,
strizzi gli occhi e torni indietro. Qui la cosa misurata sta sulla stessa schermata del comando
che la misura.

**Si ripaga già prima di quei cursori:** i tasti sono vivi, quindi la durata della vibrazione si
giudica **premendo un tasto** invece che dal solo colpo che lo slider dà al rilascio. Stesso
`KeyViewFactory`, quindi stessa `Haptics`.

La rotellina dell'anteprima non porta da nessuna parte, di proposito: è già la schermata
impostazioni, e un comando che riapre la schermata su cui sei è una trappola, non una scorciatoia.

**Due difetti trovati montandola, e la diagnosi giusta è arrivata solo al terzo tentativo.** La
schermata si apriva col titolo tagliato. Ho incolpato prima i tasti dell'anteprima che rubavano
il fuoco, poi l'ordine di `requestFocus()`; entrambe le correzioni non hanno cambiato nulla.
Il dump di `uiautomator` ha detto la verità in una riga: `ScrollView` a `[0,0]`, titolo a `y=72`,
**`scrollY` = 0**. Non era scorsa affatto — il titolo stava **sotto la barra di stato**, perché
l'inset non arrivava più.

Causa: l'inset lo reclama la vista **più esterna**, e fino alla 3.1 la `ScrollView` *era* la più
esterna. Diventata figlia del nuovo contenitore, il padding ha smesso di arrivare. Spostato il
listener sul contenitore; il basso resta della tastiera, che legge da sé l'inset della barra di
navigazione.

L'altra correzione — `requestFocus()` chiamato alla fine invece che su una vista ancora vuota —
**resta in piedi anche se non era la causa di questo difetto**: chiedere il fuoco a un contenitore
senza figli vince solo finché i figli non esistono, e il commento diceva già di volerlo per i
checkbox. `FOCUS_BLOCK_DESCENDANTS` sull'anteprima resta per lo stesso motivo: un'anteprima non
deve essere una tappa nel giro del modulo.

**File modificati:** `settings/SettingsActivity.kt`, `app/build.gradle.kts` (3.1 → 3.2).

**Verificato (2026-08-04):** 160 test verdi, e sull'emulatore — titolo visibile, anteprima
ancorata in fondo con i candidati d'esempio e i simboli preferiti veri nella colonna. Che sia
**viva** non è stato dedotto: premuti tre tasti dell'anteprima, `dumpsys vibrator_manager`
registra tre vibrazioni di `com.daux.t9keyboard` con `Step=18ms` — esattamente la durata
impostata dal cursore qui sopra. Screenshot: `docs/screenshots/step-3.2-anteprima-viva.png`.

### 2026-08-04 — Step 3.1: le impostazioni si raggiungono, e contengono qualcosa
**Primo step della Fase 3**, pianificata con l'utente prima di scriverlo (vedi *Fase 3 —
decisioni di piano*). Due cose che si tengono: una via d'accesso e qualcosa da raggiungerci.

**La via d'accesso è un'idea dell'utente arrivata a lavoro iniziato:** *"per accedere alle
impostazioni metterei una rotellina sopra la barra di disambiguità all'altezza dei candidati"*.
Chiudeva un buco vero: l'unica strada era l'icona nel launcher, cioè **uscire dall'app in cui si
sta scrivendo** per cambiare come si scrive.

La rotellina sta sopra la colonna, nella corsia che è già "non lettere", quindi niente si è
dovuto spostare per farle posto. Usa **gli stessi pesi e lo stesso rientro di 3dp** di
`T9BodyView` (0.9 su 7.4): è ciò che la fa stare *sopra* la colonna invece che genericamente
vicino. Resta visibile anche quando la barra dei candidati non lo è (pagine simboli ed emoji):
la strada per le impostazioni non deve dipendere da quale superficie si sta guardando.

Due dettagli che non sono opzionali:
- **`FLAG_ACTIVITY_NEW_TASK`.** Una tastiera è un servizio, non un'activity: non ha un task
  proprio in cui mettere la schermata, e senza il flag `startActivity` lancia un'eccezione.
- **La tastiera si nasconde** (`requestHideSelf`). La schermata impostazioni non è un campo di
  testo, e lasciarle sopra una tastiera coprirebbe i comandi per cui ci si è andati.

`KeyboardView` non conosce le Activity: prende una lambda `onSettings` come già fa per ogni
altro tasto, ed è il servizio ad aprire.

**Dentro, tre preferenze che esistevano già nel codice e non avevano dove mostrarsi:**
`autoCapitalise`, `autoSpace` e `hapticMs`. Le sezioni sono ordinate per **quanto spesso si
cambiano** — scrittura, vibrazione, lingue: gli aiuti alla scrittura sono quelli che si provano,
non piacciono e si riprovano, una lingua si mette una volta e si dimentica. Le lingue erano
prime solo perché erano arrivate prime.

**Lo slider della vibrazione vibra.** È l'unica impostazione che non si può leggere: 12 ms e 24
ms sono lo stesso numero da guardare e due tastiere diverse da usare. Lo slider chiama la
`Haptics` vera, lo stesso percorso di codice di una pressione, quindi ciò che si sente lì è
esattamente ciò che si sentirà scrivendo. Vibra **al rilascio** e non durante il trascinamento:
un colpo per pixel non è un riscontro, è un sonaglio, e coprirebbe la cosa da giudicare.

**File modificati:** `ui/KeyboardView.kt` (la riga in alto diventa rotellina + barra),
`service/T9ImeService.kt` (`openSettings`), `settings/SettingsActivity.kt` (interruttori e
slider), `app/build.gradle.kts` (2.5 → 3.1).

**Verificato (2026-08-04):** 160 test verdi, e sull'emulatore — la rotellina è allineata sopra
la colonna, apre la schermata e la tastiera si toglie di mezzo; lo slider si muove da 0 a 60 ms
con la scritta che lo segue («Spenta» a zero). Screenshot:
`docs/screenshots/step-3.1-rotellina-sopra-la-colonna.png` e
`docs/screenshots/step-3.1-schermata-impostazioni.png`.

**Nessun test nuovo**, ed è una lacuna dichiarata: `SettingsActivity` e `Haptics` dipendono
dalla piattaforma (`SharedPreferences`, `Vibrator`) e non hanno unit test JVM, come già scritto
in fondo a `docs/FUNCTIONAL.md`. Ciò che si poteva verificare qui è che i comandi si vedano e
scrivano la preferenza giusta, ed è stato fatto a mano.

### 2026-08-04 — Step 2.5: la riga dello spazio si alza, la freccia dello shift si allarga
**Due ritocchi grafici chiesti dall'utente dopo l'uso**, non trovati leggendo il codice.

**1. «Ogni tanto manco lo spazio».** La riga in fondo era alta esattamente come una riga di
lettere dallo Step 1.26; ora è **un decimo più alta**. Cresce tutta la riga e non il solo
spazio: una riga di tasti disuguali si legge come uno sbaglio, non come un bersaglio più grande.

**Il permesso dell'utente ha cambiato la soluzione a metà lavoro.** La prima versione teneva la
tastiera all'altezza di prima e prendeva i dieci centesimi alle righe di lettere — che
diventavano il 3,4% più piatte — e costringeva il peso a valere `1.14`, perché un peso è una
quota del tutto e cresceva anche il tutto. Poi è arrivato *«puoi alzare un po' il livello totale
della tastiera se serve»*: il corpo passa da **0,28 a 0,287** dello schermo, cioè 0,28 × 4,1/4,
esattamente la quota della riga in più. Le lettere restano quelle che erano, e il peso torna a
dire una cosa sola: `1.1`, un decimo più di una riga di lettere.

**2. «La freccia dello shift la vedo un po' piccola».** Da **20sp a 26sp**, mentre `⌫` e `☺`
restano a 20. Non è un'eccezione arbitraria: `⇧` è una freccia di **contorno** con quasi tutto
il riquadro vuoto, gli altri sono glifi pieni, e alla stessa misura nominale la freccia si legge
come il tasto più piccolo della colonna — proprio quello che deve dire in che maiuscole si sta
per scrivere. La richiesta era «almeno allargare la freccia» e la misura nominale è l'unica leva
che c'è: il glifo si allarga crescendo.

**File modificati:** `ui/T9BodyView.kt` (`BOTTOM_ROW_WEIGHT`), `ui/KeyboardView.kt`
(`BODY_HEIGHT_FRACTION`), `ui/KeyViewFactory.kt` (`labelSize`), `app/build.gradle.kts` (2.4 → 2.5).
Nessun test nuovo: sono tre costanti di disposizione, e ciò che va verificato è come si vedono.

**Verificato (2026-08-04):** 160 test verdi, e installato sull'emulatore — la riga in fondo è
visibilmente più alta delle righe di lettere, che non sono cambiate, e la freccia dello shift
sta ora alla pari di `⌫` e `☺` invece che sotto. Screenshot:
`docs/screenshots/step-2.5-riga-spazio-e-shift.png` (aggiunto a posteriori, il 04/08: era
rimasto fuori da `docs/`).

### 2026-08-04 — Step 2.4: l'elisione non impara più spazzatura, e `c'è` continua a impararsi
**La seconda coda della 2.3, e il caso in cui la correzione già scritta nel promemoria era
sbagliata.** Il promemoria diceva: se `Elision.join` compone `l'a`, chiedere che la coda sia
essa stessa una parola (≥ 2 caratteri).

**L'utente ha fatto notare che l'unica elisione che valga la pena imparare è `c'è`.** Che ha
esattamente la forma di `l'a`: una lettera di testa, una di coda, tre caratteri. La correzione
suggerita le avrebbe uccise **entrambe** — nessuna regola sulla lunghezza può distinguerle.

**Verificato che `c'è` dipenda davvero dall'apprendimento:** nessuna parola con apostrofo esiste
in `it.txt` né in `en.txt`. Il dizionario personale è l'unica strada per cui possa comparire fra
i candidati. Buttarla via per prevenire `l'a` sarebbe stato un pessimo scambio.

**Riprodotto sull'emulatore prima di scrivere codice**, e la riproduzione ha corretto anche la
stima del danno:
- `l` + `'` + `a` + spazio → la sequenza `52` proponeva **`l'a` prima di `la`**. Il promemoria
  diceva «con 200 contro i 17.675 di `la` è innocuo»: sbagliato, perché quel conto ignorava la
  spinta recente della 2.3 (+50.000), che per un'ora mette la parola appena imparata sopra tutto.
- `c` + `'` + `è` → la sequenza `23` la propone per prima. Due tasti per scrivere `c'è`.
- Pressione prolungata su `l'a` → sparisce, `la` torna prima, la lista si ricompone intatta. Il
  rimedio della 2.3 funziona, il che è il motivo per cui questo difetto non era urgente.

**La discriminante è l'accento, non la lunghezza.** Le elisioni corte che l'italiano ha davvero
— `c'è`, `n'è`, `s'è`, `v'è` — finiscono tutte per vocale accentata; la spazzatura è sempre una
vocale semplice (`l'a`, `l'e`, `l'o`). `Elision.join` rifiuta di unire una coda di un solo
carattere non accentato.

**File modificati:** `input/Elision.kt` (la regola), `model/T9Keypad.kt` (`isAccented`, che
espone come domanda la mappa `accentFold` già esistente invece di far nascere una seconda lista
di accenti), `app/build.gradle.kts` (2.3 → 2.4). Test: `ElisionTest` +2, i due casi riprodotti.

**Verificato (2026-08-04):** 160 test verdi. Su emulatore, con la build nuova: `52` legge
`la là ja ka jb jc kc lb` — `l'a` non entra più; e `c'è`, dimenticata apposta e riscritta da
zero, torna prima su `23`.

### 2026-08-04 — Coda della 2.3: le lettere singole restano fuori dal dizionario
**Non uno step: una decisione rimasta in sospeso dalla 2.3**, più la documentazione che la
2.3 aveva lasciato indietro. Nessun cambio di comportamento, nessun test nuovo — il
comportamento è esattamente quello di prima, ed è questo il punto.

**La domanda era se `isLearnable` (≥ 2 caratteri) avesse ancora un motivo di esistere**, visto
che la sua motivazione scritta — *"una parola imparata batte l'intero corpus"* — era stata resa
falsa dalla 2.3 stessa, che ha portato `BASE_WEIGHT` da 1.000.000 a 200. Una regola che sopravvive
alla propria motivazione è un candidato naturale alla rimozione, e valeva la pena guardarci dentro
invece di lasciarla lì per inerzia.

**Misurato invece che stimato.** Sul tasto `2` (`a` = 15.038): una `b` imparata una volta
peserebbe 200 + 50.000 di `RECENT_WEIGHT` = 50.200, e `SingleLetterEngine` confronta per peso le
lettere semplici sopra `REAL_WORD_WEIGHT` = 1.000 — quindi `b a c à` per un'ora, 5.200 entro il
giorno, 700 entro la settimana. Il sintomo della 2.2, non più permanente ma **ricorrente**.

**Decisione dell'utente: la regola resta, con una motivazione nuova** — la *prevedibilità* di un
tasto premuto da solo, non più il peso. Il guadagno del toglierla era quasi nullo (le lettere che
sono parole vere il corpus le ordina già bene; servirebbero ~50 usi perché l'abitudine da sola
superi `a`), il costo era un difetto già segnalato due volte.

**Notato mentre si decideva, e messo per iscritto:** la difesa «l'accento sta dopo la sua lettera
semplice» **non passa da questa regola** — `SingleLetterEngine` ordina su quel flag prima di
guardare i pesi. Reggerebbe anche togliendola. Prima non era scritto da nessuna parte, e chi
avesse riaperto la questione avrebbe potuto crederla in pericolo.

**File modificati:** `engine/LearnedWordsEngine.kt` (solo il commento di `isLearnable`),
`DEVELOPMENT.md`, `docs/FUNCTIONAL.md`.

**Disallineamenti di documentazione riparati nello stesso commit** (direttiva permanente: un file
disallineato è un bug):
- `docs/FUNCTIONAL.md` — la riga *"Allineato a:"* diceva ancora **2.1** mentre il file
  documentava già la 2.3.
- Fase 3 — *"Long-press su candidato → rimuovi dal dizionario"* era ancora da fare: l'ha fatta
  la 2.3.
- Fase 3 — la vibrazione con durata regolabile era ancora da fare: l'ha fatta la 1.21. Riscritta
  per dire cosa manca davvero, cioè solo la UI.
- *Prossimo step* parlava ancora di «inglese on/off», che la 2.1 ha sostituito con l'elenco
  `Language`.

### 2026-07-31 — 2.3: il peso delle parole imparate, e il modo di dimenticarle
**Domanda dell'utente:** *"Il peso delle parole imparate è forse esagerato? Non è detto che
una parola imparata debba essere la prima opzione, se è rara."*

**Misurata la distribuzione del corpus prima di rispondere** — più frequente 29.311 · 100ª
1.268 · 500ª 208 · 1.000ª 96 · mediana 2. `BASE_WEIGHT` era **1.000.000**: trentaquattro volte
la parola più frequente della lingua. Non una priorità, un annullamento.

**Il vincolo da non rompere** era il criterio di accettazione della v1 («una parola forzata una
volta è proposta per prima»). Regge lo stesso: per una parola che il corpus **non conosce**
nessun'altra corrisponde esattamente a quella sequenza, quindi resta prima a qualunque peso.
Cambia solo la parola rara *ma nota*, che è esattamente il caso di cui l'utente si lamentava.

**La scelta dell'utente è stata la terza opzione — peso proporzionale più spinta recente — con
una motivazione migliore della mia:** *"così ti accorgi se hai memorizzato una parola per
sbaglio; se la usi una sola volta rimarrà nel vocabolario a vita e non la vedrai quasi mai
più, essendo recente invece potrebbe comparire più facilmente permettendoti di cancellarla."*

**Il buco che quella motivazione ha rivelato: non c'era modo di cancellare.** La spinta
recente avrebbe fatto riemergere l'errore senza dare modo di correggerlo — un modo di mostrare
all'utente un problema che non può risolvere. Quindi due cose insieme, e la seconda non era
opzionale.

**Fatto:**
- **Peso** = abitudine + recenza. Abitudine `200 + 300 × (usi-1)`, tetto 30.000; recenza
  `+50.000` entro l'ora, `/10` entro il giorno, `/100` entro la settimana, poi zero. Tutto
  nell'unità del corpus, così i numeri si leggono contro la distribuzione vera. La decadenza è
  a scalini e non una curva: tre soglie dicibili a parole si ragionano e si testano meglio di
  un tempo di dimezzamento che nessuno riesce a immaginare.
- **Dimenticare**: pressione prolungata su un candidato. Solo le parole personali — il corpus
  non è dell'utente da modificare, e un gesto che sembra cancellare senza cambiare nulla è
  peggio di nessun gesto. La barra dice quale dei due casi è stato.

**File modificati:** `engine/LearnedWordsEngine.kt` (peso, `lastUsed` in RAM, `forget`, clock
iniettabile per testare la decadenza senza aspettare giorni veri), `learning/RoomLearnedWordsStore.kt`,
`ui/SuggestionBarView.kt` + `ui/KeyboardView.kt` (pressione prolungata), `service/T9ImeService.kt`,
`res/values/strings.xml`. Test: `LearnedWordsEngineTest` +6.

**Verificato (2026-07-31):** 158 test verdi. Su emulatore: imparata `cara` scegliendola dalla
barra, la sequenza `2272` la propone **prima di `casa`** (spinta recente); tenuta premuta,
`cara` rientra dietro `casa` — la voce personale è stata cancellata e l'ordine del corpus è
tornato quello di prima.

### 2026-07-31 — 2.2: la `b` prima della `a` — una lettera imparata anni fa
**Segnalazione dell'utente, ripetuta due volte:** premendo `2` la tastiera proponeva `B`/`b`
come prima scelta. *"Questa cosa non deve esistere: `a` si scrive spessissimo."*

**Prima diagnosi sbagliata, e perché.** Avevo attribuito la maiuscola al flag di nome proprio
su `b`/`c` (Step 1.26) e verificato sull'emulatore che la barra leggesse `a b c à`. La verifica
era **viziata**: poco prima avevo dato `pm clear` all'app, azzerando il dizionario personale —
cioè proprio ciò che causava il difetto. L'utente ha insistito con la build corretta in mano,
ed è stato giusto insistere.

**Causa vera.** Il controllo «le lettere singole non si imparano» viveva nel **servizio**
(`T9ImeService.learn`) e non nel dizionario, ed è arrivato solo alla Fase 1.14. Una `b`
confermata una volta con una build precedente è ancora nel `learned_words.db`, e una parola
imparata pesa `BASE_WEIGHT` = 1.000.000 contro i 15.038 di `a`: `SingleLetterEngine` ordina il
gruppo "parole" per peso, e `a` e `b` sono entrambe lettere semplici, quindi il confronto cade
sul peso e la `b` vince. Per sempre.

**Riprodotto prima di correggere**, invece di dedurlo: reso temporaneamente imparabile una
lettera singola, imparata una `b`, il tasto `2` proponeva **`b a c à`** con `B` nel campo —
il sintomo dell'utente, identico.

**Corretto in due punti, perché una regola sola non bastava:**
- **`LearnedWordsEngine.isLearnable`** — la regola scende dove stanno i dati. Viveva in un
  chiamante, ed è esattamente così che le lettere erano entrate.
- **`load()` bonifica l'archivio** — butta *e cancella* le lettere singole già memorizzate.
  Una regola che vale solo per il futuro lascia il danno dov'è: senza questo, il telefono
  dell'utente sarebbe rimasto rotto per sempre. Ha richiesto `delete` nel seam `Store`.

**Verificato sul dispositivo, non solo nei test:** installata la build corretta **sullo stesso
database** appena inquinato, il tasto `2` legge `a b c à`. 152 test verdi, fra cui la
riproduzione al livello dell'archivio.

**Nota di metodo, la lezione più utile della giornata:** `pm clear` prima di una verifica
cancella anche lo stato che *causa* il difetto. Per riprodurre il problema di un utente, lo
stato va ricostruito, non azzerato.

### 2026-07-31 — Fase 2.1: il bilinguismo si accende e si spegne
**Richiesta dell'utente:** *"renderei il bilinguismo attivabile e disattivabile in modo che se
dà fastidio si può rendere inattivo, e magari si potrebbe predisporre in futuro per altre
lingue se mai diventasse un progetto commerciale."*

La preferenza esisteva già (`englishEnabled`, Fase 2) ma **nessuno poteva raggiungerla**, e
una preferenza irraggiungibile non è una preferenza. Due cambiamenti, uno per richiesta.

**1. Una schermata impostazioni** (`settings/SettingsActivity`), con l'icona nel launcher —
la prima fetta di Fase 3, portata avanti perché serviva adesso. Contiene solo le lingue: il
resto (altezza tastiera, durata vibrazione, testo candidati, lato colonna) esiste già in
`KeyboardSettings` e la raggiungerà con la Fase 3 vera.

**2. Un elenco di lingue, non un interruttore per l'inglese.** `model/Language` è l'unico
posto dove una lingua si dichiara (codice, nome, asset), la preferenza è un **insieme di
codici**, e `BilingualDictionaryEngine` è diventato **`LanguagePriorityEngine`** con una
*lista* di secondarie. Aggiungere lo spagnolo costa un dizionario e una riga: manca il corpus,
non il codice. C'è un test che lo verifica con tre lingue, invece di affermarlo.

**Il dizionario si ricostruisce da solo** quando la scelta cambia: `onStartInputView`
confronta le lingue attive con quelle caricate: è il momento in cui la tastiera rientra, e la
ricostruzione va su un thread di fondo che l'utente non sta aspettando.

**Due inciampi risolti sulla schermata, entrambi da `targetSdk 35`:**
- Il contenuto veniva disegnato **sotto le barre di sistema** (edge-to-edge obbligatorio su
  Android 15): il titolo finiva dietro la barra di stato. La tastiera gestiva già i propri
  inset; la schermata no. Ora li applica anche lei.
- La casella prendeva il fuoco all'apertura e lo `ScrollView` saltava a lei, aprendo la
  pagina già oltre la propria intestazione.

**Verificato (2026-07-31):** 149 test verdi. Sull'emulatore, il giro completo: spento
l'inglese dalla schermata, la preferenza si svuota e la sequenza di `homework` non lo propone
più — resta **`gonfiore`**, l'ipotesi italiana a due tasti sbagliati, che è anche una prova
incidentale che lo Step 1.24 lavora. Riacceso, `homework` torna.
Screenshot: `docs/screenshots/fase2.1-impostazioni.png`.

**Nota di metodo:** dopo ogni reinstallazione l'emulatore perde la selezione dell'IME e
mostra un pannello di ripiego. Non è un crash — verificato in `logcat` prima di ipotizzarlo —
va solo rieseguito `bash tools/dev.sh ime`.

### 2026-07-31 — Fase 2: bilingue IT+EN (versione 2.0)
**Italiano e inglese attivi insieme, senza cambio lingua** — il piano §8. Il tastierino è lo
stesso in entrambe (`2`=ABC ovunque nello standard E.161), quindi dell'input non c'è niente di
bilingue: lo è solo il ranking. La colonna resta unica, come il piano prevedeva.

**La decisione dell'utente, e perché conta.** I pesi dei due corpora sono già in *occorrenze
per milione* — cioè davvero confrontabili — quindi una fusione per frequenza era tecnicamente
a portata. È proprio lì il pericolo: `the` sta a ~42.000 per milione e guiderebbe quasi ogni
sequenza che tocca. Interpellato, l'utente ha scelto **italiano sempre prima**, ed è la scelta
con la proprietà più forte: *nessuna sequenza che funzionava prima può ordinarsi diversamente
adesso*. L'inglese compare solo dove l'italiano ha finito.

**Una scoperta di architettura:** `MergingDictionaryEngine`, che il tracker dava per pronto
allo scopo fin dallo Step 1.5, si è rivelato **lo strumento sbagliato**. Fonde per peso, il
che è giusto per personale+corpus (dove si *vuole* che competano) ma è esattamente ciò che
non si vuole fra due lingue. Da lì `BilingualDictionaryEngine`, che **concatena** invece di
fondere.

**Nessun `EnglishDictionaryEngine`**, contro quanto previsto dal tracker: aperto
`ItalianDictionaryEngine` si è visto che di italiano aveva solo il nome — legge un file
`parola peso [P]` e basta. Rinominato **`CorpusDictionaryEngine`** e riusato tale e quale;
duplicarlo per cambiare il percorso di un asset sarebbe stato codice per finta.

**File creati/modificati:**
- `engine/BilingualDictionaryEngine.kt` (nuovo) + `BilingualDictionaryEngineTest` (7 casi).
- `engine/ItalianDictionaryEngine.kt` → **`CorpusDictionaryEngine.kt`** (rinomina, nessun
  cambiamento di comportamento).
- `assets/dict/en.txt` (nuovo): **36.560 parole**, 368 KB, stesse fonti dell'italiano —
  OpenSubtitles `en` (70%) + Leipzig `eng_news_2020_100K` (30%).
- `tools/BuildDictionary.java` — quinto argomento opzionale per la lingua, che decide **solo**
  quali caratteri contano come parola (l'italiano tiene gli accenti, l'inglese è a-z). Fusione,
  normalizzazione per milione e misura dei nomi propri erano già neutre.
- `settings/KeyboardSettings.kt` — `englishEnabled` (attivo di default). A inglese spento il
  motore non viene nemmeno costruito e `en.txt` non viene letto.
- `service/T9ImeService.kt` — il personale resta **sopra** entrambe le lingue, quindi
  l'apprendimento resta un dizionario misto unico senza colonna `lang` (piano §8).

**Verificato (2026-07-31):** 147 test verdi. Su emulatore: la sequenza di `homework` — che in
italiano non dà nulla — propone **`homework`** per prima, con `woodwork` (refuso) e
`homeworld` (completamento) dietro. E il criterio di accettazione della fase, **nessuna
regressione**: `2272` dà `casa cara bara basa barb capa`, lista identica a prima dell'inglese.
Screenshot: `docs/screenshots/fase2-*.png`.

**Limite noto dichiarato:** l'inglese `I` non viene reso maiuscolo. Il corpus lo marca
correttamente come nome proprio, ma la regola dello Step 1.26 ("una lettera sola non è mai un
nome proprio") lo scarta — e quella regola è voluta, perché in italiano `i` è l'articolo. Con
l'italiano primario è il compromesso giusto, ma è un compromesso.

**Costo:** l'APK passa da ~2,2 a **2,6 MB**.

### 2026-07-31 — Step 1.26: riga dello spazio uniformata, e la `B` che non doveva esserci
**Due segnalazioni dell'utente dopo lo Step 1.25.**

**1. La riga dello spazio era rimasta troppo bassa.** Era a peso 0.72, deliberatamente più
sottile delle file di lettere: barra spazio e vicini non sono lettere. Reggeva finché la
tastiera era alta; abbassata tutta, la stessa frazione di un numero più piccolo lasciava una
riga difficile da centrare — e la barra spazio è il tasto più premuto che ci sia. Portata a 1.

**La tastiera non ricresce:** l'altezza complessiva è fissata da `KeyboardView`, quindi le
quattro righe se la dividono in parti uguali. Le file di lettere passano da 214 a 200 px e il
tasto lettera da rapporto 1,45 a **1,56** — appena più piatto, cioè nella direzione in cui lo
Step 1.25 stava già andando.

**2. Premendo `2` la barra offriva "a B C à".** L'utente lo ha letto come *"dà la B maiuscola
come prima opzione"*. Verificato sull'emulatore: `a` **era** la prima ed era ciò che veniva
scritto — ma le maiuscole di `B` e `C` saltano all'occhio e si leggono come le opzioni
importanti. Il difetto quindi c'era, solo in un punto diverso da dove sembrava.

**Causa:** il corpus flagga davvero `b` e `c` come nomi propri (`b 34 P`, `c 38 P`). Nella
prosa giornalistica una lettera isolata è un'iniziale (`B. Rossi`) o un marcatore di elenco
(`a) b) c)`), mai la lettera. È misura, non errore di misura — ma è la misura di un'altra
domanda.

**Correzione alla radice:** `CorpusDictionaryEngine.build` scarta il flag `P` per le parole
di un carattere. Non un caso speciale su `b` e `c`, e nemmeno un ritocco al dizionario: è lo
stesso principio già scritto in `learn()` ("le lettere singole non si imparano") e in
`SingleLetterEngine` — a un carattere il corpus smette di misurare ciò che gli chiediamo.

*Nota sull'osservazione dell'utente:* «`a` è una preposizione a sé stante, non ha senso che
proponga `b`». L'ordinamento in effetti già lo faceva — `a` pesa 15038 contro i 34 di `b` — e
questo è il motivo per cui la correzione riguarda la **maiuscola** e non l'ordine.

**File modificati:** `ui/T9BodyView.kt`, `engine/CorpusDictionaryEngine.kt`, test `+1`.

**Verificato (2026-07-31):** 140 test verdi. Su emulatore la barra legge ora **`a b c à`**,
tutto minuscolo, e la riga dello spazio ha la stessa altezza delle file di lettere.
Screenshot: `docs/screenshots/step-1.26-riga-spazio-e-lettere-minuscole.png`.

### 2026-07-31 — Step 1.25: tastiera più bassa, tasti più larghi che alti
**Aggiustamento grafico chiesto dall'utente:** barra candidati e le tre file di tasti troppo
alte, tasti troppo quadrati, barra "quasi dimezzabile". La colonna era già tarata e non è
stata toccata nella sua logica.

**Misurato su schermo 1280×2856 a 480dpi**, prima → dopo:

| | prima | dopo |
|---|---|---|
| barra candidati | 168 px | **96 px** |
| corpo tastiera | 971 px | **799 px** |
| totale | 1139 px (40% dello schermo) | **895 px (31%)** — taglio del 21% |
| tasto lettere | 311×261, rapporto **1,19** | 311×214, rapporto **1,45** |

1,19 era un tasto **quasi quadrato**: si legge come un tastierino numerico e spende in
altezza ciò di cui il testo sopra ha più bisogno. Più largo che alto è anche la forma che un
pollice colpisce davvero — l'errore comune è orizzontale, non verticale.

**Tre costanti, non una:** `BAR_DP` 56→32, `BODY_HEIGHT_FRACTION` 0.34→0.28 e
`DEFAULT_TEXT_SP` 22→17. Il testo **deve** scendere con la barra, o le parole toccano i bordi
della striscia. Guadagno non previsto: nella barra ci stanno ora **sei** candidati dove ne
entravano quattro.

**Prezzo dichiarato:** 32dp sta sotto i 48dp raccomandati come area di tocco. Accettabile per
una striscia di scelta rapida sopra una tastiera, ma è un prezzo, non un pasto gratis.
L'altezza regolabile resta compito della Fase 3.

**File modificati:** `ui/KeyboardView.kt`, `ui/SuggestionBarView.kt`.

**Verificato (2026-07-31):** su emulatore, digitato `casa` → il campo scrive `Casa` e la barra
mostra sei candidati. **Nota di metodo:** la prima prova dopo il cambio è fallita perché le
coordinate dei tap erano tarate sulla vecchia posizione dei tasti, che ora sono più in alto —
non era un difetto del codice ma del test. Screenshot: `docs/screenshots/step-1.25-*.png`.

### 2026-07-31 — Step 1.24: due tasti invertiti, e due tasti sbagliati
**Richiesta dell'utente:** *"è facile invertire due tasti mentre scrivi, quindi se non rilevi
niente si potrebbero comunque suggerire i candidati più vicini anche su due caratteri
sbagliati o invertiti."*

**Il dettaglio che rende la richiesta ancora più giusta di come è stata posta:** l'inversione
di due tasti **non era coperta affatto**. Nella metrica cancellazione/inserzione/sostituzione
uno scambio vale *due* modifiche, quindi cadeva fuori dalla distanza 1 — lo sbaglio più comune
che esista, assente per una definizione e non per un prezzo. Costa `n-1` varianti in più.

**Fatto:** l'inversione entra fra le scivolate singole (quindi è offerta *sempre*, non solo
quando la barra sarebbe vuota); i **due tasti sbagliati** sono un'ultima risorsa, cercati solo
quando non c'è né una corrispondenza esatta né una a una scivolata, e solo da 6 cifre in su.

**Due limiti deliberati, entrambi sul costo e sul rumore:**
- **Solo sostituzioni doppie**, non tutta la distanza 2: quest'ultima sarebbe ogni variante di
  ogni variante, decine di migliaia di stringhe costruite su una pressione di tasto.
- **Da 6 cifre in su**: su una parola corta due errori lasciano troppo poco di giusto — metà
  di `casa` sbagliata non è un refuso, è un'altra parola.

**Misurato, non supposto:** `FuzzyCostTest` (nuovo) esegue il caso peggiore sul corpus vero —
otto tasti che non somigliano a niente, dove ogni stadio gira fino in fondo prima di
arrendersi: **0,6 ms per pressione** su JVM desktop, quindi pochi millisecondi su telefono. È
un test permanente, così un cambiamento che lo rende costoso si rompe da solo.

**Una costante tolta invece che aggiunta:** avevo introdotto `DEEP_PENALTY` per pesare i
risultati profondi sotto quelli vicini. Un test l'ha smentita: i due insiemi non finiscono
**mai** nella stessa lista (i profondi si cercano solo quando i vicini sono zero), quindi
esprimeva una distinzione che nulla può osservare. Rimossa.

**File modificati:** `engine/FuzzyDictionaryEngine.kt` (inversione, `twoWrongKeys`, varianti
come `Sequence` pigra così la ricerca profonda non alloca finché non serve), test
`FuzzyDictionaryEngineTest` (+5 casi) e `FuzzyCostTest` (nuovo).

**Verificato (2026-07-31):** 139 test verdi. Su emulatore, `problema` (`77625362`) digitato con
il `2` e il `5` invertiti (`77652362`): la barra offre **`problema`**, che prima non trovava
nulla. Screenshot: `docs/screenshots/step-1.24-tasti-invertiti.png`.

### 2026-07-31 — Step 1.23: anteprima leggibile quando niente corrisponde
**Coda dello Step 1.22, sollevata dall'utente:** *"meglio della roba illeggibile."* Digitando
i dieci tasti di `contemporaneamente` la barra aveva già la parola giusta, ma il campo
mostrava `ammtdmpmpa` — le lettere di default.

**Fatto:** in assenza di corrispondenze esatte l'anteprima mostra la **migliore offerta**
(completamento o parola vicina); le lettere di default restano solo quando non c'è nemmeno
quella.

**Il ragionamento, perché contraddice in apparenza la regola "offerte, mai assunzioni":** la
regola serve a non far scavalcare da un'ipotesi ciò che l'utente ha scritto davvero. Qui non
c'è nulla da scavalcare — l'alternativa sono lettere di default che nessuno vorrebbe
confermare comunque. Fra due tentativi vince quello che è una parola vera, e la colonna lo
scavalca con un tocco. **Mentre si sta forzando la regola resta intatta**: le lettere forzate
sono una decisione esplicita e nessuna offerta le sostituisce.

**File modificati:** `service/T9ImeService.kt` (`previewWord` + `bestOffer`).

**Verificato (2026-07-31):** su emulatore, i dieci tasti di `contempora` ora scrivono
`contemporaneamente` nell'anteprima. Letto con `uiautomator dump`, non dai pixel.

### 2026-07-31 — Step 1.22: completamento di parola
**Mancanza notata dall'utente, mai pianificata.** *"Usiamo i tasti premuti per determinare
una parola ma non facciamo delle previsioni predittive su parole più lunghe: se scrivo i
tasti per `contempora` mi potrebbe proporre `contemporaneamente`."*

**Misurato prima di progettare**, e il caso era peggiore di come suonava: la sequenza di
`contempora` (`2668367672`) oggi non restituiva **niente**, perché l'indice risponde solo a
sequenze della stessa lunghezza e nessuna parola italiana di dieci lettere la scrive. Con il
completamento dà sei parole, `contemporaneamente` in testa.

**La soglia è misurata, non scelta a occhio.** Sotto un prefisso di 2 cifre cadono ~3.700
parole del corpus, di 3 ~2.000, di 4 ~507. Il costo di scansione è irrilevante in tutti i
casi; il **rumore** no. Da qui `MIN_LENGTH = 4`.

**Fatto:** dopo le corrispondenze esatte, la barra offre le parole di cui i tasti sono
l'inizio — al massimo 5.

**File creati/modificati:**
- `engine/CompletingDictionaryEngine.kt` (nuovo) — il decoratore, fuori da quello dei refusi,
  così i completamenti finiscono **fra** le esatte e i tentativi sui refusi.
- `engine/DictionaryEngine.kt` — `completions(prefix, limit)`, con implementazione vuota di
  default. **Separato da `lookup` per costo**: il motore dei refusi cerca un centinaio di
  varianti a ogni pressione, e completare ciascuna farebbe cento scansioni invece di una.
- `engine/CorpusDictionaryEngine.kt` — sequenze anche **ordinate**: le corrispondenze di un
  prefisso sono un tratto contiguo, quindi ricerca binaria e scansione che si ferma da sé.
- `engine/LearnedWordsEngine.kt` — scansione semplice: il dizionario personale è di ordini di
  grandezza più piccolo, e un indice lì sarebbe macchinario da mantenere per nulla.
- `engine/MergingDictionaryEngine.kt`, `FuzzyDictionaryEngine.kt`, `SingleLetterEngine.kt` —
  inoltro di `completions`.
- `engine/Candidate.kt` — flag `completion` e `isExact`; `SuggestionBarView` e l'anteprima
  ora ragionano su `isExact` anziché sul solo `fuzzy`.
- Test: `engine/CompletingDictionaryEngineTest.kt` (7 casi) + 4 in `CorpusDictionaryEngineTest`.

**La regola che governa tutto:** un completamento è **un'offerta, mai un'assunzione**. I tasti
sono un *prefisso* della parola, non una sua descrizione, quindi il candidato è marcato, sta
dietro alle esatte, e l'anteprima nel campo continua a seguire una corrispondenza esatta.
Committere d'ufficio diciotto lettere su dieci pressioni sarebbe il tirare a indovinare che
la colonna esiste per impedire.

**Verificato (2026-07-31):** 133 test verdi. Su emulatore, digitati i dieci tasti di
`contempora`: la barra offre `contemporaneamente` e `contemporanea` in grigio mentre il campo
mostra le lettere di default (nessuna esatta), e toccando il candidato la parola viene
inserita **e imparata** (controllato in `learned_words.db`).
Screenshot: `docs/screenshots/step-1.22-*.png`.

**Nota su una stranezza vista negli screenshot:** nel campo compariva una `a` di troppo dopo
la parola. È un residuo di una prova precedente rimasto **a destra del cursore**, letto con
`uiautomator dump` invece di indovinarlo dai pixel — non c'entra con la funzione, che ha
lavorato su dieci cifre esatte.

### 2026-07-31 — Step 1.21: la tastiera vibra sotto il dito
**Punto 4 degli appunti**, l'ultimo. *"Mi sta mancando tanto non sentire il tasto quando
schiaccio."* **Decisione dell'utente: durata regolabile** — quindi il `Vibrator` con durata
esplicita, non `performHapticFeedback`.

**Il prezzo della scelta, pagato qui:** il tick di sistema avrebbe rispettato da sé
l'impostazione dell'utente. Guidando il `Vibrator` direttamente, farlo tocca a noi —
`Haptics` legge `HAPTIC_FEEDBACK_ENABLED` e tace quando è spento. Una tastiera che vibra
dopo che l'hai disattivata è una tastiera che disinstalli.

**File creati/modificati:**
- `ui/Haptics.kt` (nuovo) — il tick, con la lettura dell'impostazione di sistema memorizzata
  per un secondo (è una chiamata binder, e i tasti sono veloci).
- `settings/KeyboardSettings.kt` — `hapticMs`: default **18 ms**, `0` = spento, massimo 60.
  Durata e non booleano, perché è tutto il motivo per cui guidiamo noi il `Vibrator`.
- `ui/KeyViewFactory.kt` — l'aggancio sta qui, l'unico posto dove ogni superficie costruisce
  i tasti, così T9/simboli/emoji/popup non possono divergere. `attachLongPressPopup` ora
  dice se ha preso il controllo del tocco; il tasto senza alternative riceve
  `attachTapFeedback`, un listener il cui unico compito è il tick.
- `AndroidManifest.xml` — permesso `VIBRATE` ("normal", nessun dato).

**Tre decisioni sul quando:** alla **pressione** e non al rilascio; il **backspace tenuto
premuto vibra una volta sola** (un colpo per carattere diventerebbe un ronzio); l'**apertura
del popup ha un colpo doppio**, perché segna un cambio di stato e non una battuta.

**Verificato (2026-07-31):** `:app:testDebugUnitTest` verde (122 test). Sull'emulatore la
vibrazione non si sente, quindi è stata letta nel **registro del servizio**: dopo due tasti,
`dumpsys vibrator_manager` mostra due voci `Step=18ms` da `com.daux.t9keyboard` — cioè
esattamente il nostro one-shot. Poi, spegnendo `haptic_feedback_enabled`, due altri tasti
**non aggiungono alcuna voce**; riaccendendolo la voce ricompare.

**Non coperto da unit test:** `Haptics` e `KeyboardSettings` dipendono dalla piattaforma
(`Vibrator`, `Settings.System`, `SharedPreferences`). La verifica è reale ma manuale.

**Resta per la Fase 3:** lo slider della durata nella schermata impostazioni. Il valore è
già letto fresco a ogni pressione, quindi quella schermata non richiederà modifiche qui.

### 2026-07-31 — Step 1.20: la versione è visibile mentre si prova
**Richiesta dell'utente:** *"per evitare confusione quando testo da telefono vorrei che
versioni apk e nome app in modo da renderci conto che versione sto testando nel frattempo
che viene modificata."*

**Fatto:** `versionName` **è** il numero dello step di questo file, non una numerazione
parallela — e da lì derivano nome dell'app ed etichetta della tastiera. Nel selettore
tastiere si legge **"T9 1.21"**, quindi ciò che si ha in mano si trova nel log a colpo
d'occhio.

**File modificati:**
- `app/build.gradle.kts` — `versionCode`/`versionName`, e `resValue` che genera `app_name` e
  `ime_label` dalla versione: unica fonte, impossibile che restino indietro.
- `res/values/strings.xml` — le due stringhe **tolte**: scritte a mano si sarebbero
  disallineate al primo step.
- `tools/dev.sh` — `apk` stampa la versione e copia l'APK con la versione nel nome
  (`dauxpal-3.7-debug.apk`), così sul telefono non si confondono fra loro.

**Verificato (2026-07-31):** installato, `dumpsys package` riporta `versionName=1.21`, e nelle
impostazioni Android la tastiera compare come **"T9 1.20"** (screenshot preso alla 1.20:
`docs/screenshots/step-1.20-versione-nel-selettore.png`).

**Nota:** ricordarsi di alzare `versionCode`/`versionName` a ogni step — è la sola parte
manuale rimasta, e uno step che se lo dimentica rende il numero una bugia.

### 2026-07-31 — Step 1.19: l'apostrofo dell'elisione sta dentro la parola
**Punti 2 e 3 degli appunti, affrontati insieme** perché dipendono dalla stessa distinzione,
come l'appunto stesso prevedeva: l'apostrofo **elisione** contro l'apostrofo **virgoletta**.

**Il punto 2 non si riproduce.** In un campo di prosa vero esce `J'aveva`, minuscolo. Il
motivo è strutturale ed è documentato nella sezione appunti qui sopra. Non ho scritto una
correzione: sarebbe codice contro un difetto non osservato, col rischio di rompere il caso
virgoletta che oggi funziona. Da riverificare sul telefono con l'APK aggiornato.

**Il punto 3 era reale**, confermato nel database prima di toccare il codice: scritto
`j'aveva` + spazio, risultava imparata `aveva` da sola.

**Fatto:** la parola elisa si impara **intera**, e alla seconda scrittura si ottiene
digitando **solo le lettere** — l'apostrofo lo scrive la tastiera.

**File creati/modificati:**
- `input/Elision.kt` (nuovo) — la regola condivisa: lettera da entrambi i lati = elisione,
  tutto il resto è virgoletta (compreso il troncamento `po'`, che non unisce niente).
- `model/T9Keypad.kt` — `sequenceFor` **salta** l'apostrofo dell'elisione (`l'aveva` →
  `528382`) e restituisce `null` per quello usato come virgoletta: non è una parola sola.
- `service/T9ImeService.kt` — `commitCurrentWord` e `onPickCandidate` leggono cosa precede
  la parola *prima* di scriverla, e imparano la forma elisa intera.
- `input/SentenceRules.kt` — nessun codice nuovo: l'elisione non raggiunge già quel ramo.
  Aggiunta solo la **ragione**, invece di codice morto che sembrasse una correzione.
- Test: `input/ElisionTest.kt` (6 casi), più 2 in `model/T9KeypadTest.kt`.

**Verificato (2026-07-31):** `:app:testDebugUnitTest` verde (122 test). Su emulatore, campo
messaggi: scritto `j'aveva` + spazio → compare **come parola unica** in `learned_words.db`
(prima no). Poi, digitando le sole lettere `5-2-8-3-8-2`, `j'aveva` è **il primo candidato**
e il campo lo scrive con l'apostrofo. Screenshot: `docs/screenshots/step-1.19-*.png`.

**Limite noto dichiarato:** l'adozione dello Step 1.18 si ferma ancora all'apostrofo
(parcheggiando il cursore dopo `l'aveva` si adotta `aveva`). `ComposeState` associa una cifra
a ogni lettera e non sa rappresentare un carattere che non si digita: superarlo è un cambio
strutturale, e merita uno step suo invece di essere infilato qui.

### 2026-07-31 — Step 1.18: riprendere la parola già scritta sotto il cursore
**Punto 1 degli appunti della prova reale.** Prima di toccare il codice, la riproduzione
dettata è stata tracciata contro il dizionario vero — e **la premessa non reggeva**:
`farla` è nel dizionario (riga 1534, peso 61) ed è il primo candidato della sequenza
`32752`, mentre `farà` non esiste (c'è `fara`, 76). Seguendo i passi alla lettera
l'apprendimento funzionava già. Interpellato, l'utente ha confermato il passo mancante:
al momento di «tornare sulla fine della parola» **aveva toccato il campo**. Da lì la
richiesta vera, più larga della segnalazione: *"se voglio scrivere `farà` e parto da
`farla`, cancello la `la`, torno indietro col cursore e scrivo `farà` usando il prefisso
`far` già scritto, deve memorizzarla sullo spazio o sul candidato"*.

**Fatto:** spostando il cursore **con il dito** a fine parola, la tastiera **adotta** la
parola già scritta e continua a comporre su di essa, invece di iniziarne una nuova.

**File modificati:**
- `input/ComposeState.kt` — `adopt(word)` (ricostruisce la sequenza e adotta le lettere
  come **forzate**, così il ranking non riscrive ciò che è scritto) e `forcedPreview()`.
- `service/T9ImeService.kt` — `onUpdateSelection` ora distingue i propri edit da quelli
  dell'utente (flag `selfEdit`) e chiama `adoptWordAtCursor()`; i candidati sono filtrati
  sul prefisso forzato; `previewWord()` preferisce il candidato anche mentre si forza.
- `input/ComposeStateTest.kt` — 6 test nuovi.

**Due difetti emersi strada facendo, corretti qui:**
- Mentre si forzava, l'anteprima era la sola `forcedText()`: le cifre premute dopo erano
  **invisibili** finché non se ne sceglieva la lettera. Adottato `far`, premere `5` non
  avrebbe mostrato nulla. Ora la coda non risolta compare con le lettere di default.
- La barra proponeva candidati che **contraddicevano** le lettere forzate (`dara` a chi
  aveva compitato `far`). Ora sono filtrati sul prefisso forzato — ed è ciò che permette
  all'anteprima di leggere `farla` invece delle lettere di default `farja`.

**Invariante di sicurezza:** l'adozione **non cambia mai il testo**. Se l'anteprima che ne
risulterebbe differisce anche solo per una maiuscola da ciò che è nel campo, la parola è
lasciata stare. Un tocco per spostare il cursore non deve riscrivere nulla.

**Verificato (2026-07-31):** `:app:testDebugUnitTest` verde (114 test). Su emulatore
Android 17: scritto `far` + spazio, toccato il campo dopo la `r` → la parola torna in
composizione; `5`-`2` → il campo legge **`farla`**. Verso inverso: adottato `far`, premuto
`2`, forzata `à` dalla colonna → **`farà`**. L'apprendimento è stato verificato **nel
database**, non a occhio: composto `farz` (assente sia dal dizionario sia dalle parole
imparate) partendo dal prefisso adottato, dopo lo spazio compare in `learned_words.db`
(`adb exec-out run-as com.daux.t9keyboard cat databases/…`, poi ricerca nel file — sul
device non c'è `sqlite3`). Screenshot: `docs/screenshots/step-1.18-*.png`.

**Nota:** l'apostrofo fa da **confine di parola** (`l'aveva` adotta `aveva`). È deliberato:
le elisioni come parola unica sono il punto 3 degli appunti e vanno decise lì, non di
straforo. L'emulatore ha ora `farz` fra le parole imparate, residuo della verifica.

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
`assets/dict/it.txt` (rigenerato, 564 KB), `engine/CorpusDictionaryEngine.kt`
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
- `engine/CorpusDictionaryEngine.kt` — indice in RAM `sequenza → [candidati]`; `fromAssets()`
  + `build(lines)` puro e testabile.
- `model/T9Keypad.kt` — aggiunto `sequenceFor(word)` (parola→cifre, con fold accenti IT).
- `assets/dict/it_test.txt` — dizionario di test (~40 parole, con collisioni per il ranking).
- `ui/SuggestionBarView.kt` — barra suggerimenti orizzontale scrollabile (chip tap-abili).
- `ui/T9KeyboardView.kt` — ora contiene barra suggerimenti + griglia; `setSuggestions()`.
- `service/T9ImeService.kt` — logica predittiva (buffer sequenza, anteprima, commit, pick).
- Test JVM: `model/T9KeypadTest.kt`, `engine/CorpusDictionaryEngineTest.kt`.

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
