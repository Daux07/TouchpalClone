# Documentazione funzionale — Tastiera T9 stile TouchPal

> Descrive **come funzionano le feature implementate** dal punto di vista
> comportamentale e architetturale. Cresce man mano che si sviluppa: ogni feature
> completata aggiunge/aggiorna la sua sezione. Non è la specifica (quella è
> `prompt-tastiera-t9-touchpal.md`) né il tracker (quello è `DEVELOPMENT.md`):
> qui si documenta ciò che **esiste e funziona** nel codice.
>
> ⚠️ **Direttiva permanente:** questo file va aggiornato **nello stesso commit** della
> feature che documenta, insieme a `DEVELOPMENT.md`. Documentazione disallineata =
> step non finito.

**Allineato a:** 3.7 — backspace ed emoji scambiati nella colonna funzioni (versione 3.7).

**Versione visibile.** `versionName` **è** il numero dello step di `DEVELOPMENT.md`, e da lì
derivano (via `resValue` in `app/build.gradle.kts`) il nome dell'app e l'etichetta della
tastiera: nel selettore si legge **"DauxPal 3.7"**. Provando sul telefono si sa sempre a che punto
del log corrisponde ciò che si ha in mano. Le due stringhe non stanno più in `strings.xml`:
scritte a mano resterebbero indietro.

**Il nome è "DauxPal" dallo Step 3.5** (scelta dell'utente). L'`applicationId` resta
`com.daux.t9keyboard`: cambiarlo significherebbe disinstallare e reinstallare l'app, e con essa
il **dizionario personale** che l'utente ha costruito scrivendo. "T9" continua a indicare la
tecnica di scrittura, che non è cambiata — solo il prodotto ha un nome proprio.

**Ogni build lascia un APK con la versione nel nome** (`dauxpal-3.7-debug.apk`), prodotto sia da
`bash tools/dev.sh apk` sia da `install`. Prima lo faceva solo `apk`: usando `install` — cioè
quasi sempre — si ritrovava il solo `app-debug.apk`, un nome che non dice quale build sia e che
ogni ricostruzione sovrascrive.

⚠️ **L'icona è un segnaposto**: una "D" bianca disegnata a mano, come prima era una "T". Ne
arriverà una vera.

## Indice
- [Panoramica architettura](#panoramica-architettura)
- [1. IME base — `T9ImeService`](#1-ime-base--t9imeservice)
- [2. Superfici e layout](#2-superfici-e-layout)
- [3. Inserimento predittivo T9](#3-inserimento-predittivo-t9)
- [4. Colonna di disambiguazione](#4-colonna-di-disambiguazione--la-funzione-centrale)
- [5. Motori di dizionario](#5-motori-di-dizionario)
- [6. Apprendimento persistente](#6-apprendimento-persistente-dizionario-personale)
- [7. Maiuscole, accenti, emoji](#7-maiuscole-accenti-emoji)
- [8. Cancellazione](#8-cancellazione-tap-tenuto-premuto-parole)
- [9. Simboli preferiti](#9-simboli-preferiti-nella-colonna)
- [10. Popup a pressione prolungata](#10-popup-a-pressione-prolungata)
- [11. Maiuscole automatiche e spazio automatico](#11-maiuscole-automatiche-e-spazio-automatico)
- [12. Impostazioni e persistenza](#12-impostazioni-e-persistenza)
- [13. Copertura dei test](#13-copertura-dei-test)
- [Cosa non c'è ancora](#cosa-non-cè-ancora)

---

## Panoramica architettura

La tastiera è un **IME Android** (`InputMethodService`). Package sotto
`com.daux.t9keyboard`:

| Package | Responsabilità |
|---------|----------------|
| `service` | `T9ImeService`: ciclo di vita IME, orchestrazione di tutto, invio testo al campo attivo |
| `model` | Dati puri: mapping tasti, layout (`KeyGrid`), azioni, simboli preferiti, emoji |
| `input` | Stato della digitazione: `ComposeState` (composizione), `ShiftState` (maiuscole) |
| `ui` | View custom: superficie tastiera, corpi T9/griglia, colonna, barra suggerimenti |
| `engine` | `DictionaryEngine` (interfaccia) e implementazioni; ranking dei candidati |
| `learning` | Dizionario personale su Room (entità, DAO, database, store) |
| `settings` | Preferenze utente su `SharedPreferences` |

**Due principi che reggono tutta la struttura:**

1. **Tutto il dizionario passa da `DictionaryEngine.lookup(sequence)`.** UI e service non
   conoscono le implementazioni: corpus, dizionario personale, tolleranza ai refusi e (Fase 2)
   la seconda lingua si compongono per decorazione/merge senza toccare il resto.
2. **La logica pura è separata dalle view Android**, così è coperta da unit test JVM senza
   emulatore: `ComposeState`, `ShiftState`, `T9Keypad`, `FavouriteSymbols`, `SymbolLayout`
   e tutti gli engine non hanno dipendenze Android (dove serve accesso alla piattaforma c'è
   un seam, es. `LearnedWordsEngine.Store`).

---

## 1. IME base — `T9ImeService`

**Cosa fa:** registra l'app come tastiera di sistema. Quando l'utente seleziona
"T9 Keyboard", Android chiama `onCreateInputView()` e mostra la `KeyboardView`.

**Ciclo di vita rilevante:**
- `onCreate()` — costruisce le impostazioni, il dizionario personale (disponibile **subito**)
  e lancia un thread daemon che carica il corpus da 50k parole; a caricamento finito
  `engine` diventa il merge dei due. La tastiera compare istantaneamente e impara anche
  mentre il corpus si sta ancora caricando.
- `onStartInputView()` — ogni campo nuovo riparte dalla modalità `abc` (T9), ovunque il campo
  precedente avesse lasciato la tastiera, e azzera la composizione.
- `onFinishInput()` — chiude il composing text pendente.
- `onEvaluateInputViewShown()` → sempre `true`: forza la comparsa anche con tastiera hardware
  collegata (no-op sui telefoni reali, indispensabile sull'emulatore).

**Privacy:** nessun permesso applicativo sensibile (niente rete/SMS/contatti). C'è
`VIBRATE`, di categoria "normal": concesso all'installazione, non chiede nulla e non dà
accesso ad alcun dato — serve al ritorno tattile (§2). Per il resto l'unico
permesso è `BIND_INPUT_METHOD`, obbligatorio per qualsiasi IME e concesso solo al sistema.
Il dizionario personale è un database locale nella sandbox dell'app e non esce mai dal
dispositivo.

**Come si abilita:** *Impostazioni Android → Gestione generale → Elenco tastiere e
predefinita → abilita "T9 Keyboard"*, poi la si seleziona dal selettore tastiera.

---

## 2. Superfici e layout

### `KeyboardView` — la radice

Ospita la **barra suggerimenti** in alto e sotto il **corpo della modalità corrente**.
Possiede ciò che è comune a tutte le modalità: sfondo scuro, **inset della barra di
navigazione** (targetSdk 35 è edge-to-edge, altrimenti i tasti finirebbero sotto la nav bar)
e altezza complessiva (`BAR_DP` **32dp** + una quota dello schermo regolabile, default **29%**).

**Le proporzioni, e perché sono queste (Step 1.25).** Misurato su schermo 1280×2856 a 480dpi:

| | prima | dopo |
|---|---|---|
| barra candidati | 168 px | **96 px** |
| corpo tastiera | 971 px | **799 px** |
| totale | 1139 px (40% dello schermo) | **895 px (31%)** — taglio del 21% |
| tasto lettere | 311×261 px, rapporto **1,19** | 311×214, rapporto **1,45** |

Un rapporto di 1,19 è un tasto **quasi quadrato**: si legge come un tastierino numerico,
non come una tastiera, e spende in altezza ciò di cui il testo sopra ha più bisogno.
Più largo che alto è anche la forma che un pollice colpisce davvero — l'errore comune è
orizzontale, non verticale.

La barra è quasi dimezzata, con il testo dei candidati sceso di pari passo (22sp → 17sp):
le due cose **devono** muoversi insieme, o le parole toccano i bordi della loro striscia. Il
guadagno non è solo di spazio: nella barra ci stanno ora **sei** candidati dove ne entravano
quattro. Il prezzo dichiarato è che 32dp sta sotto i 48dp raccomandati come area di tocco —
accettabile per una striscia di scelta rapida sopra una tastiera, ma è un prezzo.

**La riga dello spazio è un decimo più alta di una riga di lettere (Step 2.5).** L'utente
segnalava di mancare lo spazio ogni tanto — il tasto più premuto che ci sia. Cresce **tutta la
riga**, non il solo spazio: una riga di tasti disuguali si legge come uno sbaglio, non come un
bersaglio più grande.

I dieci centesimi sono **altezza nuova, non altezza tolta alle lettere**: la frazione del corpo
è passata da 0,28 a **0,287**, cioè 0,28 × 4,1/4, esattamente la quota della riga in più e
niente altro. Una riga di lettere resta 0,07 dello schermo, la misura che ha dallo Step 1.25.
È anche il motivo per cui il peso della riga può restare leggibile — `1.1`, «un decimo più di
una riga di lettere»: a corpo fisso lo stesso 10% avrebbe richiesto `1.14`, perché il peso è
una quota del tutto e il tutto sarebbe cresciuto insieme a lei.

**L'altezza è regolabile dallo Step 3.3**: le misure qui sopra sono quelle del default (29%), e
chi la vuole diversa lo dice dal cursore nelle impostazioni — vedi §12.

Nelle modalità non-T9 la barra suggerimenti diventa **invisibile ma occupa il suo spazio**,
così l'altezza della tastiera non salta cambiando superficie.

### Modalità — `KeyboardMode` + `KeyAction.Mode(target)`

Il cambio di superficie è tipizzato: `T9`, `SYMBOLS_1`, `SYMBOLS_2`, `EMOJI`. Cambiando
modalità **la parola in corso viene confermata**: uscire dal tastierino a metà parola
lascerebbe un composing text che nessun tasto potrebbe più chiudere.

### `T9BodyView` — il corpo T9

**Il backspace sta in basso a destra dallo Step 3.7**, scambiato con le emoji su richiesta
dell'utente: *"avendo usato la Gboard ultimamente si trova in una posizione più simile e riesco
ad usare la memoria muscolare meglio"*. Dove sta il tasto più premuto dopo lo spazio non è una
questione di gusto ma di cosa il pollice ha già imparato altrove.

```
┌──────┬───────────────────────────┬─────────┐
│ dis- │  @   abc   def            │   ☺     │
│ amb. │  ghi jkl   mno            │   ⇧     │
│ col. │  pqrs tuv  wxyz           │   ⌫     │
│      ├───────────────────────────┴─────────┤
│      │ 12# ,   [   space   ]  .   ⏎        │
└──────┴─────────────────────────────────────┘
```

Colonna di disambiguazione a sinistra (peso 0.9, si ferma sopra la riga inferiore), griglia
3×3 lettere al centro (5.4), colonna funzioni a destra (1.1), riga inferiore full-width
**della stessa altezza di una fila di lettere** (peso 1). Tutto a pesi: nessuna dimensione
fissa, quindi la stessa UI si riproporziona fra S25 e S25 Ultra senza layout dedicati.

La riga inferiore era più sottile (0.72), deliberatamente: barra spazio e vicini non sono
lettere. Reggeva finché la tastiera era alta; abbassata tutta (Step 1.25), la stessa frazione
di un numero più piccolo lasciava una riga troppo bassa da centrare — e la barra spazio è il
tasto più premuto che ci sia. Uniformata nello Step 1.26 **senza far ricrescere la tastiera**:
l'altezza complessiva è fissata da `KeyboardView`, quindi le quattro righe se la dividono in
parti uguali e i tasti lettera diventano appena più piatti (rapporto 1,45 → **1,56**).

### `KeyGrid` + `GridKeyboardView` — la griglia riusabile

`KeyGrid` = lista di `KeyRow` (ciascuna con un peso di altezza), `KeyRow` = lista di
`KeySpec` (ciascuno con un peso di larghezza). **Deliberatamente non T9-specifica.**
`GridKeyboardView` disegna un `KeyGrid` qualsiasi: le pagine simboli e il pannello emoji
sono già solo dati, e la **QWERTY alternativa** (pianificata) sarà un `KeyGrid` in più,
non una vista nuova.

### Ritorno tattile — `Haptics`

Il tasto **si sente** sotto il dito. Vive in `KeyViewFactory`, l'unico posto dove ogni
superficie costruisce i suoi tasti, così T9, pagine simboli, emoji e popup non possono
finire per rispondere in modo diverso.

**Perché il `Vibrator` e non `performHapticFeedback`.** Il tick di sistema è quello che
decide il telefono e non è tarabile; un tick che non si sente equivale a non averlo.
Scegliendo il `Vibrator` la durata diventa una preferenza (`KeyboardSettings.hapticMs`,
default **18 ms**, `0` = spento, massimo 60), ma **rispettare l'impostazione di sistema
diventa un nostro compito**: `Haptics` legge `HAPTIC_FEEDBACK_ENABLED` e tace se l'utente
ha spento il feedback tattile. Una tastiera che vibra dopo che l'hai disattivata è una
tastiera che disinstalli. La lettura è memorizzata per un secondo: abbastanza spesso da
reagire mentre la tastiera è aperta, abbastanza di rado da non essere una chiamata binder
a ogni tasto.

Tre decisioni sul *quando*:

- **Alla pressione** (`ACTION_DOWN`), non al rilascio: è il momento in cui il dito chiede
  conferma. Anche il tasto senza alternative, che declina il gesto del popup, riceve un
  listener il cui unico compito è il tick — restituisce `false` e lascia lavorare il click.
- **Il backspace tenuto premuto vibra una volta sola**, all'inizio. Un colpo per ogni
  carattere cancellato si fonderebbe in un ronzio: un telefono che sembra rotto, non una
  tastiera che risponde.
- **L'apertura del popup ha un colpo suo**, di durata doppia: segna un cambio di stato, non
  una battuta — la stessa distinzione che il sistema fa fra `KEYBOARD_TAP` e `LONG_PRESS`.

Richiede il permesso `VIBRATE`: "normal", concesso all'installazione, senza accesso a dati.
La durata regolabile dall'utente arriverà con la schermata impostazioni (Fase 3); il valore
è già letto fresco a ogni pressione, quindi quella schermata non richiederà modifiche qui.

### `KeyViewFactory` — il disegno del singolo tasto

Faccia arrotondata con stato premuto, label centrata, numerino teal d'angolo. **Condiviso**
fra T9, pagine simboli, emoji e futura QWERTY, così le superfici non possono divergere
visivamente: cambiare l'aspetto di un tasto si fa una volta sola, qui. Contiene anche la
ripetizione a pressione prolungata del backspace (§8).

### Pagine simboli — `SymbolLayout`

Due pagine in stile QWERTY (dieci tasti stretti per riga) dietro `12#`; `1/2` ↔ `2/2` le
scambia, `abc` torna al T9. Pagina 1 = cifre + punteggiatura di tutti i giorni; pagina 2 =
segni rari (valute, matematica, parentesi, marchi). Due pagine perché su una sola i tasti
diventerebbero troppo piccoli per il pollice.

---

## 3. Inserimento predittivo T9

I tasti 2–9 costruiscono una **sequenza di cifre**; ad ogni pressione il service interroga
il motore e mostra la parola più probabile come *composing text* nel campo, con l'intera
lista nella barra suggerimenti.

| Tasto | Comportamento |
|-------|---------------|
| `2`–`9` | Estende la sequenza |
| `0` / `space` | Conferma la parola in composizione (e la impara) + inserisce spazio |
| `⌫` | Pop dell'ultima coppia (cifra, lettera); a sequenza vuota cancella nel campo. Tenuto premuto: vedi §8 |
| `⏎` | Conferma la parola ed esegue l'azione dell'editor (search/done/invio) |
| `,` `.` | Conferma la parola e inserisce il segno (tenuti premuti: vedi §10) |
| `1` | Conferma la parola e inserisce `@` — ciò che il tasto mostra; il resto è nel suo popup (§10) |
| `⇧` | Cicla le maiuscole (§7) |
| `12#` / `☺` | Cambia superficie |

**Barra suggerimenti** (`SuggestionBarView`): riga orizzontale scrollabile di chip; il primo
candidato è evidenziato in teal (è anche l'anteprima nel campo), le **offerte** — refusi e
completamenti — sono in grigio. Tap = conferma quel candidato. Testo a **17sp**
(`DEFAULT_TEXT_SP`), esposto come proprietà `textSizeSp` per essere pilotato dalle
impostazioni in Fase 3; va tenuto in accordo con `BAR_DP` (§2).

**Mapping lettere (`T9Keypad.letters`, ITU-T E.161):** 1=`. , ? ! '`, 2=abc, 3=def, 4=ghi,
5=jkl, 6=mno, 7=pqrs, 8=tuv, 9=wxyz, 0=spazio. È la fonte di verità unica, riusata da
etichette dei tasti, motore predittivo e colonna.

> ℹ️ **Il multi-tap non esiste più.** È stato un trampolino dello Step 1.1 per validare la
> pipeline griglia→`InputConnection`, rimpiazzato dal predittivo allo Step 1.2.

---

## 4. Colonna di disambiguazione — la funzione centrale

**È la funzione centrale del progetto.** Permette di comporre una parola qualsiasi — anche
**non presente nel dizionario** — scegliendo le lettere una per una da una colonna sempre
visibile a lato della griglia (piano §3).

### Modello di stato (`ComposeState`)

Due liste parallele con invariante `chosen.length ≤ digits.size`:
- `digits` — le cifre premute, nell'ordine (la "sequenza originale");
- `chosen` — le lettere forzate finora, una per cifra, risolte da sinistra a destra.

Una posizione risolta `i` è la coppia `(digits[i], chosen[i])` — lo "stack di coppie
(cifra, lettera)" del piano. La colonna indirizza sempre la **prima posizione non risolta**
(`activeColumnDigit`).

### Comportamento

- Premi una cifra 2–9 → si aggiunge alla sequenza; la colonna mostra le lettere di quella
  posizione (dopo `2` → `A B C À`).
- Tocchi una lettera → viene forzata, la colonna avanza alla posizione successiva.
- **Anteprima nel campo**, in quest'ordine: la migliore predizione **esatta**; se non ce n'è
  nessuna, la **migliore offerta** (completamento o parola vicina — vedi sotto); e solo se
  manca anche quella, le **lettere di default** (`defaultLetters()`, o `forcedPreview()` se
  si sta forzando) — **mai le cifre**.

  L'offerta entra nell'anteprima **solo in assenza di corrispondenze esatte**, dove non c'è
  nulla da scavalcare: l'alternativa sono le lettere di default, che su dieci tasti leggono
  `ammtdmpmpa` — illeggibili, sbagliate allo stesso modo, e nemmeno una parola che qualcuno
  vorrebbe confermare. Fra due tentativi, quello che è una parola vera è il migliore, e la
  colonna lo scavalca con un tocco. **Mentre si sta forzando questo non vale**: le lettere
  forzate sono una decisione esplicita dell'utente e nessuna offerta le sostituisce.
- **Mentre si forza, i candidati sono filtrati** su ciò che è già stato forzato: chi ha
  compitato `far` non si vede proporre `dara`. La colonna esiste per scavalcare il ranking,
  e proporre parole che la contraddicono disferebbe il lavoro appena fatto. È anche ciò che
  permette all'anteprima di fidarsi del dizionario anche a metà forzatura: forzato `far` e
  premuti `5`-`2`, il campo legge `farla` e non le lettere di default `farja`.
- **`⌫` = pop dell'intera coppia** (cifra + lettera insieme), mai solo il carattere: evita
  cifre "orfane" (piano §3.6). Se la coda ha una cifra non ancora risolta, rimuove quella.
- **Estendere** una parola e **correggere** l'ultima lettera sono la **stessa identica
  operazione** (backspace + ripressione + scelta): nessuna distinzione di codice.
- **Limite noto:** nessun editing in-place a metà parola; si cancella dalla coda e si
  ridigita (piano §3.9).

### Riprendere una parola già scritta (`adopt`)

Spostando il cursore **con il dito** alla fine di una parola già nel campo, la tastiera la
**adotta**: ne ricostruisce la sequenza di cifre (`T9Keypad.sequenceFor`) e riprende a
comporre su di essa, invece di iniziare una parola nuova. Parcheggia il cursore dopo `far`,
premi `5`-`2` e ottieni `farla`, che lo spazio (o il tocco su un candidato) **impara** —
mentre prima la tastiera avrebbe composto una parola separata `la`, lasciando `farla`
sconosciuta al dizionario personale.

Le regole che la rendono sicura:

- **Le lettere sono adottate come forzate**, non ridigitate: ciò che è scritto deve restare
  scritto. Adottare `dar` come sequenza nuda lascerebbe vincere il più frequente `far`, che
  riscriverebbe il testo dell'utente.
- **L'adozione non cambia mai il testo.** Se l'anteprima che ne risulterebbe differisce anche
  solo per una maiuscola da ciò che è già nel campo, la parola viene lasciata stare e la
  tastiera riparte da zero. Un tocco per spostare il cursore non deve riscrivere nulla.
- **Solo a fine parola.** Con lettere ancora a destra il cursore è *dentro* la parola, e
  proseguirla inserirebbe in mezzo a ciò che è scritto.
- **La maiuscola è riletta dal testo** (`Far` → `ShiftState.ONCE`, `FAR` → `LOCK`): è un fatto
  sulla parola a schermo, non una regola che la tastiera stia applicando adesso.
- **Solo gli spostamenti dell'utente.** `onUpdateSelection` riceve anche le nostre modifiche;
  il flag `selfEdit`, alzato quando la tastiera agisce e abbassato dal cambio di selezione che
  ne consegue, distingue i due casi. Senza, la tastiera combatterebbe il proprio lavoro.
- L'apostrofo fa da **confine di parola**: `l'aveva` adotta `aveva`. È un limite noto, non
  la decisione sulle elisioni — quella è presa (§6, *Elisioni*): `ComposeState` associa una
  cifra a ogni lettera e non sa ancora rappresentare un carattere che non si digita.

### Dimensionamento delle celle

Le celle **riempiono la colonna con 3–4 elementi** (il numero abituale di lettere); oltre 4
mantengono la misura da 4 e la colonna **scorre**. Il calcolo dipende dall'altezza della
colonna, ignota mentre la input view viene costruita, e gli elementi possono arrivare prima o
dopo quel momento: la ricostruzione avviene quindi in `onLayout`, che copre entrambi gli
ordini e la riapertura di una vista riusata (`onSizeChanged` non basterebbe, non scatta a
dimensione invariata).

**A riposo** la colonna non è spazio morto: mostra i simboli preferiti (§9). Tenendo premuto un
tasto lettera si ottiene la stessa scelta della colonna senza spostare il pollice (§10).

---

## 5. Motori di dizionario

Tutto dietro `DictionaryEngine.lookup(sequence): List<Candidate>`. `Candidate(word, sequence,
weight, fuzzy)` — il `weight` è su scala confrontabile, così liste da sorgenti diverse si
fondono con un sort.

**La composizione attiva a regime:**

```
SingleLetterEngine                  (ultima parola su cosa offre un tasto solo)
  └── FuzzyDictionaryEngine
        └── MergingDictionaryEngine
              ├── LearnedWordsEngine        (dizionario personale, pesi nell'unità del corpus)
              └── CorpusDictionaryEngine   (corpora fusi, 50k parole)
```

### `LanguagePriorityEngine` — più lingue insieme (Fase 2)

Più lingue senza cambiare lingua. Il tastierino è lo stesso in entrambe — `2`=ABC ovunque
nello standard ITU-T E.161 — quindi **dell'input non c'è niente di bilingue**: lo è solo il
ranking, ed è tutto ciò che questa classe fa. La colonna di disambiguazione resta unica,
come previsto dal piano §8.

**Una lingua secondaria non scavalca mai la primaria**, e le secondarie non si scavalcano fra loro fuori dall'ordine in cui sono elencate. Ogni parola italiana che i tasti
scrivono esattamente viene prima, nel suo ordine; l'inglese segue, nel suo. Deliberatamente
**non** una fusione per frequenza, benché i pesi lo permetterebbero: `CorpusDictionaryEngine`
normalizza entrambi i corpora a **occorrenze per milione**, quindi le due scale sono davvero
confrontabili — ed è proprio lì il pericolo. `the` sta a ~42.000 per milione e guiderebbe
quasi ogni sequenza che tocca, spingendo giù le parole italiane sulla barra di una tastiera
usata soprattutto in italiano.

La scelta ha una proprietà che vale più della resa: **nessuna sequenza che funzionava prima
può ordinarsi diversamente adesso**. L'inglese può comparire solo dove l'italiano ha finito.
Verificato: digitando `2272` la barra legge `casa cara bara basa barb capa`, identica a prima
dell'inglese; digitando la sequenza di `homework` — che in italiano non dà nulla — la barra
la propone per prima.

Una parola che entrambe le lingue conoscono (`bar`, `film`, `radio`) è tenuta **una volta
sola**, dalla parte italiana: è la stessa parola, non due candidati.

**L'apprendimento resta un dizionario misto unico**, come il piano consente: sta *sopra*
questo motore, quindi una parola confermata vince qualunque lingua l'abbia proposta, e
`learned_words` non ha bisogno di una colonna `lang`.

### Quali lingue, e come si accendono

`Language` (in `model/`) è **l'unico posto dove una lingua si dichiara**: codice, nome
mostrato, percorso dell'asset. Aggiungerne una è pensato per costare *un dizionario e una
riga* — il motore che lo legge non ha niente di specifico per lingua, il tastierino è lo
stesso in ogni lingua E.161, e il ranking tratta ogni secondaria allo stesso modo. Per lo
spagnolo manca il corpus, non il codice.

La preferenza è un **insieme di codici** (`KeyboardSettings.secondaryLanguages`) e non un
booleano, proprio perché la terza lingua non debba toccarla. La primaria non è nell'elenco:
è la lingua per cui la tastiera è fatta, sempre caricata, sempre prima.

Si accendono dalla **schermata impostazioni** (`SettingsActivity`, icona nel launcher), la
prima fetta di Fase 3 portata avanti perché *una preferenza che nessuno può raggiungere non
è una preferenza*. Il dizionario si ricostruisce al rientro della tastiera nel campo
successivo (`onStartInputView` confronta la scelta con quella caricata): una lingua spenta
**non viene nemmeno letta** e non occupa memoria. Con nessuna secondaria attiva resta
esattamente la tastiera della v1.

**Limite noto:** l'inglese `I` non viene reso maiuscolo. Il corpus lo marca correttamente
come nome proprio, ma la regola "una lettera sola non è mai un nome proprio" (§7) lo scarta —
regola voluta, perché in italiano `i` è l'articolo e deve restare minuscolo. Con l'italiano
primario è il compromesso giusto.

### `CorpusDictionaryEngine` — il corpus

Indice in RAM `Map<sequenza, [Candidate ordinati]>`: il lookup durante la digitazione non fa
I/O.

**Non ha niente di italiano dentro** — si chiamava `ItalianDictionaryEngine` fino alla Fase 2,
e il nome era l'unica cosa legata alla lingua. Legge un file `parola peso [P]` e basta, quindi
serve entrambe le lingue senza una riga di differenza:

| File | Parole | Fonte |
|---|---|---|
| `assets/dict/it.txt` | 50.000 | OpenSubtitles `it` + Leipzig `ita_news_2022_100K` |
| `assets/dict/en.txt` | 36.560 | OpenSubtitles `en` + Leipzig `eng_news_2020_100K` |

Entrambi costruiti da `tools/BuildDictionary.java` fondendo **due corpora**, perché nessuno
dei due da solo descrive la lingua che si scrive al telefono:

| Corpus | Peso | Cosa porta |
|--------|------|------------|
| **OpenSubtitles** (hermitdave/FrequencyWords, CC BY-SA 4.0) | 70% | Dialoghi di film e serie: il registro più vicino alla conversazione |
| **Leipzig `ita_news_2022_100K`** (CC BY-4.0) | 30% | Il vocabolario che ai dialoghi manca (istituzioni, geografia, registro formale) e — unico dei due — **le maiuscole**, cioè le prove per i nomi propri |

La differenza di registro è enorme e si misura: `ciao` compare **27** volte nel corpus
giornalistico e **225.358** nei sottotitoli; `beh` 47 contro 415.077. Le due scale di frequenza
non sono confrontabili (milioni di token contro migliaia), quindi ciascuna è convertita in
**occorrenze per milione** prima di essere fusa.

Caricato in background; i lookup prima del completamento ritornano vuoto. A questa dimensione
il formato testo è adeguato: il binario indicizzato resta un'ottimizzazione futura.

Formato: `parola peso [P]`, dove il peso è in occorrenze per milione e `P` marca i **nomi
propri** — misurati sul corpus che conserva le maiuscole (§11), non elencati a mano.

Rigenerazione (nessuna delle due fonti è nel repo):
```
JAVA="/c/Program Files/Android/Android Studio/jbr/bin/java.exe"
curl -sL -o subs_it.txt \
  "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/it/it_full.txt"
curl -sL -o corpus.tar.gz \
  "https://downloads.wortschatz-leipzig.de/corpora/ita_news_2022_100K.tar.gz"
tar -xzf corpus.tar.gz
"$JAVA" tools/BuildDictionary.java subs_it.txt <path/...-words.txt> \
  app/src/main/assets/dict/it.txt 50000 it

# Inglese: stesse fonti, stesso strumento, quinto argomento diverso
curl -sL -o subs_en.txt \
  "https://raw.githubusercontent.com/hermitdave/FrequencyWords/master/content/2018/en/en_full.txt"
curl -sL -o eng.tar.gz \
  "https://downloads.wortschatz-leipzig.de/corpora/eng_news_2020_100K.tar.gz"
tar -xzf eng.tar.gz
"$JAVA" tools/BuildDictionary.java subs_en.txt <path/...-words.txt> \
  app/src/main/assets/dict/en.txt 50000 en
```

Il quinto argomento (`it` per difetto) decide **solo** quali caratteri contano come parola:
l'italiano tiene le vocali accentate, l'inglese è a-z puro. Fusione, normalizzazione per
milione e misura dei nomi propri sono neutre rispetto alla lingua — per questo l'inglese ha
richiesto un parametro e non un secondo strumento.

### `MergingDictionaryEngine` — l'unione

Fonde più dizionari dietro l'unica `lookup`, deduplicando per parola (tiene il peso più alto).
Serve per **personale + corpus italiano**, dove la fusione per peso è quella giusta: una
parola imparata pesa da `BASE_WEIGHT` in su e vince sempre.

Per IT+EN si è rivelato **lo strumento sbagliato**, ed è la scoperta interessante della Fase 2:
la fusione per peso funziona quando le fonti sono commensurabili *e si vuole* che competano.
Fra due lingue i pesi sono commensurabili — entrambi per milione — ma la competizione è
proprio ciò che non si vuole. Da lì `LanguagePriorityEngine`, che concatena invece di
fondere.

### `FuzzyDictionaryEngine` — tolleranza ai refusi

**Decora** un qualsiasi `DictionaryEngine` proponendo, dopo i match esatti, parole a **una
scivolata** dalla sequenza digitata: cifra di troppo, cifra mancante, cifra sbagliata, e
**due cifre invertite**.

L'inversione è stata aggiunta nello Step 1.24, ed è il caso più istruttivo: è lo sbaglio più
comune che esista — le dita arrivano nell'ordine sbagliato — e prima **sfuggiva del tutto**,
perché nella metrica cancellazione/inserzione/sostituzione uno scambio vale **due**
modifiche. Costa `n-1` varianti in più: era assente per una definizione, non per un prezzo.

**Due tasti sbagliati, come ultima risorsa.** Quando non c'è né una corrispondenza esatta né
una a una scivolata, e la sequenza è lunga almeno 6 cifre, si cercano le sequenze con **due
cifre sbagliate**. Due limiti deliberati:

- **Solo sostituzioni doppie**, non tutta la distanza di modifica 2. Quest'ultima sarebbe
  ogni variante di ogni variante — decine di migliaia di stringhe costruite su una pressione
  di tasto. Due tasti sbagliati è il doppio errore che capita davvero su una parola lunga, e
  costa un paio di migliaia di lookup.
- **Solo da 6 cifre in su.** Su una parola corta due errori lasciano troppo poco di giusto:
  metà di `casa` sbagliata non è un refuso, è un'altra parola.

Il costo si paga **esattamente quando non ci sarebbe nulla da mostrare comunque**, mai
mentre la digitazione normale funziona. Misurato sul corpus vero (50k parole) nel caso
peggiore — otto tasti che non somigliano a niente, dove ogni stadio gira fino in fondo prima
di arrendersi: **0,6 ms per pressione** su JVM desktop, quindi pochi millisecondi su
telefono. `FuzzyCostTest` lo tiene sotto controllo a ogni build.

**Come, senza costo nel caso normale:** non scandisce il dizionario — genera le **varianti
della sequenza** e le cerca nell'indice esistente. Le varianti sono prodotte come `Sequence`
pigra, così la ricerca profonda non alloca nulla finché non serve davvero.

**Perché non disturba la digitazione normale:**
- i candidati fuzzy sono marcati (`Candidate.fuzzy`), pesati / 1000, messi **dopo** tutti gli
  esatti, con un tetto di 6;
- sequenze sotto le 3 cifre non vengono corrette (a 2 cifre tutto è a distanza 1 da tutto);
- **soprattutto:** un fuzzy non scavalca **mai** una corrispondenza esatta. Altrimenti
  scrivere una parola sconosciuta al dizionario si trasformerebbe in una parola simile, cioè
  esattamente ciò che la colonna serve a impedire. Entra nell'anteprima solo quando di esatte
  non ce n'è nessuna (§4), dove l'alternativa sono lettere di default illeggibili;
- nella barra sono resi in grigio.

Sta **fuori** dal merge, quindi tollera i refusi anche sulle parole imparate e (Fase 2) su
entrambe le lingue.

### `CompletingDictionaryEngine` — le parole più lunghe

Dopo le parole che i tasti scrivono **esattamente**, offre quelle di cui i tasti sono
**l'inizio**. È qui che una tastiera T9 si ripaga su una parola lunga: dieci tasti per
`contemporaneamente`. Prima di questo, quei dieci tasti non corrispondevano **a nulla** —
l'indice risponde solo a sequenze della stessa lunghezza, e nessuna parola italiana di
esattamente dieci lettere scrive quella sequenza.

**Un'offerta, mai un'assunzione.** I tasti digitati sono un *prefisso* del completamento,
non una sua descrizione: il candidato è marcato (`Candidate.completion`), sta dietro a ogni
corrispondenza esatta, e non ne scavalca **mai** una (`Candidate.isExact`). Finisce
nell'anteprima solo quando di esatte non ce n'è nessuna (§4). Committare al posto di una
corrispondenza esatta una parola da
diciotto lettere su dieci pressioni sarebbe esattamente il tirare a indovinare che la
colonna di disambiguazione esiste per impedire.

**Da 4 cifre in su**, misurato sul corpus: con 2 cifre sotto il prefisso cadono ~3.700
parole e con 3 ~2.000 — la barra smetterebbe di parlare di ciò che si sta scrivendo per
diventare un elenco della lingua. Con 4 sono ~500 e la testa di quella lista è una
previsione vera. Massimo 5 offerte.

**Dove sta e perché.** Fuori da `FuzzyDictionaryEngine`, così i completamenti finiscono
**fra** le corrispondenze esatte e i tentativi sui refusi: una parola più lunga che stai
probabilmente scrivendo vale più di una parola che potresti aver sbagliato a scrivere.

Chiede i completamenti attraverso `DictionaryEngine.completions(prefix, limit)` e **non**
attraverso `lookup`, ed è una scelta di costo: il motore dei refusi cerca un centinaio di
varianti della sequenza a ogni pressione, e completare ciascuna trasformerebbe una
scansione per prefisso in cento. Il metodo ha implementazione vuota di default, così ogni
decoratore che non lo inoltra semplicemente non ha completamenti da offrire.

**Come si cerca un prefisso.** `CorpusDictionaryEngine` tiene le sequenze indicizzate anche
**ordinate**: le corrispondenze di un prefisso sono sempre un tratto *contiguo* dell'ordine,
quindi una ricerca binaria trova dove inizia e la scansione si ferma alla prima sequenza che
non comincia più con esso — niente scansione delle 50.000. Il dizionario personale, che è di
ordini di grandezza più piccolo, si scandisce e basta: un indice lì sarebbe macchinario da
mantenere a ogni parola imparata per nulla.

### `SingleLetterEngine` — un tasto solo non è una parola

Ordinare i risultati di **una sola cifra** per frequenza dà una lista che sembra casuale,
perché a una lettera la frequenza smette di misurare ciò che serve: le forme accentate
finiscono davanti alle altre lettere del tasto (`e è é d f`) e una lettera che da sola non
compare mai — `q` — sparisce del tutto dal tasto 7.

Per una cifra sola la lista è quindi **ricostruita dal tastierino**: ci sono **tutte** le
lettere del tasto, quelle che sono parole vere vengono prima, e un accento non precede mai la
lettera semplice a cui appartiene. Il `3` offre `e è d f é`, il `7` offre `p q r s`
nell'ordine stampato sul tasto. Sopra le mille occorrenze una voce di una lettera è una parola
che si scrive davvero (`e`, `a`, `è`); sotto è residuo del corpus — iniziali, abbreviazioni —
che non ha motivo di precedere le lettere del tasto.

Le sequenze più lunghe non vengono toccate: lì la frequenza è la risposta giusta.

> Nel primo istante dopo l'avvio, finché il corpus è in caricamento, un tasto singolo mostra
> semplicemente l'ordine del tastierino: nessun peso è ancora noto. Si sistema da sé.

### `T9Keypad.sequenceFor(word)`

Parola → sequenza cifre, con **fold degli accenti** italiani (perché/perche stessa sequenza).
Fonte di verità condivisa da corpus, apprendimento e test.

**L'apostrofo dell'elisione viene saltato**: `l'aveva` → `528382`, cioè le sole lettere.
Una parola elisa si digita quindi **senza apostrofo**, e la tastiera lo riscrive lei. Usato
come virgoletta (`'ciao`, `po'`) l'apostrofo non unisce nulla e la funzione restituisce
`null`: quella non è una parola sola, e non ha una sequenza. La distinzione è per posizione —
lettera da entrambi i lati = elisione — ed è la stessa regola di [`Elision`](#elisioni).

---

## 6. Apprendimento persistente (dizionario personale)

**La tastiera impara.** Ogni parola effettivamente confermata — con spazio, invio,
punteggiatura, o scegliendo un suggerimento — finisce nel dizionario personale, consultato
**prima** del corpus. Una parola forzata lettera per lettera con la colonna va quindi digitata
"a mano" **una sola volta**: dalla seconda in poi è la prima predizione della sua sequenza.

- **`LearnedWordsEngine`** — il dizionario personale come `DictionaryEngine`. Indice in RAM
  `sequenza → (parola → usi + ultimo uso)`: durante la digitazione **non si tocca mai il
  database**. Kotlin puro grazie al seam `LearnedWordsEngine.Store`.

### Quanto pesa una parola imparata

**Compete con il corpus, non lo sostituisce.** Fino alla 2.2 ogni parola imparata pesava
`1.000.000`: trentaquattro volte la parola più frequente dell'italiano. Non era una priorità,
era un annullamento — una parola confermata per sbaglio **una volta** restava davanti a `casa`
per il resto della vita del dizionario.

I numeri sono nell'unità del corpus (occorrenze per milione), così si leggono contro la
distribuzione reale — *più frequente 29.311 · 100ª 1.268 · 500ª 208 · 1.000ª 96 · mediana 2*:

| | peso | posizione |
|---|---|---|
| confermata 1 volta | 200 | ~500ª parola |
| 5 volte | 1.400 | ~90ª |
| 10 volte | 2.900 | ~40ª |
| 100 volte | 30.000 (tetto) | sopra tutto |
| **appena usata** | **+50.000** | sopra tutto, per un'ora |

**I pesi del corpus sono fissi, quelli personali no** — ed è ciò che impedisce a una parola
rara di restare in coda per sempre. Scriverla *è* impararla, quindi il suo peso personale
cresce mentre quello del corpus sta fermo, e il merge tiene il maggiore dei due. Misurato in
`MergingDictionaryEngineTest`, su `2272` dove `bara` vale 50 contro i 900 di `casa`:

| usi | ordine nella barra |
|---|---|
| 1 | casa · cara · **bara** |
| 3 | casa · **bara** · cara |
| 4 | **bara** · casa · cara |

Quattro conferme e la parola guida la sua sequenza in modo stabile — senza contare l'ora
successiva a ogni uso, in cui la spinta recente la mette prima comunque.

**L'abitudine cresce con l'uso e ha un tetto.** La recenza è una spinta a parte, che svanisce
a scalini — *adesso*, *oggi*, *questa settimana* — perché tre soglie dicibili a parole sono
più facili da ragionare, testare e spiegare di una curva con un tempo di dimezzamento che
nessuno riesce a immaginare.

**Perché la spinta recente esiste**, oltre a tenere a portata un termine ripetuto nella stessa
conversazione: fa **riemergere una parola imparata per sbaglio**, che altrimenti sprofonderebbe
nell'archivio senza farsi notare e ci resterebbe a vita. È il motivo per cui è arrivata
insieme al modo di dimenticare — senza quello sarebbe solo un modo di mostrare all'utente un
errore che non può correggere.

### Dimenticare una parola

**Pressione prolungata su un candidato** = dimenticala. È l'unico posto in cui l'utente
incontra una parola imparata faccia a faccia, quindi è lì che dev'essere possibile disfarla.
Sparisce dalla RAM e dall'archivio.

Si possono dimenticare **solo le parole personali**: il corpus non è dell'utente da modificare,
e una pressione prolungata su `casa` che sembrasse cancellarla senza poi cambiare nulla sarebbe
peggio di nessun gesto. La barra dice quale dei due casi è stato.
- **Room** (`learning/`): entità `LearnedWord` (parola PK, sequenza, usi, ultimo uso),
  `LearnedWordDao`, `LearnedWordsDatabase`, `RoomLearnedWordsStore` che **scrive in coda su un
  thread singolo** — la pressione di un tasto non deve mai attendere il disco; l'ordine è
  garantito dall'executor a thread singolo.
- La **chiave** è minuscola: "Casa" e "casa" sono la stessa parola per lookup, deduplica e
  conteggio.

- **Ma la forma scritta viene ricordata quando dice qualcosa** (Step 3.6). Accanto alla chiave
  c'è un `display`, valorizzato solo quando la parola porta una maiuscola **che nessuna regola
  avrebbe potuto metterci** — cioè dopo il primo carattere:

  | Scritto | Memorizzato come | Perché |
  |---|---|---|
  | `casa` | `casa` | niente da dire |
  | `Casa` | `casa` | la maiuscola iniziale la fanno un punto, un campo nuovo o un nome proprio |
  | `CIAO` | `ciao` | tutto maiuscolo è un tono di voce, non un'ortografia |
  | `xD`, `iPhone`, `McDonald` | **`xD`, `iPhone`, `McDonald`** | qualcuno l'ha voluto |

  E una forma che non dice niente **non cancella** una che diceva qualcosa: `xd` scritto a
  inizio frase arriva come `Xd`, e senza questa cautela spazzerebbe via lo `xD` voluto.

  La colonna è arrivata con la **versione 2** del database e una migrazione scritta a mano.
  `fallbackToDestructiveMigration` sarebbe stata una riga, e avrebbe cancellato il dizionario
  personale di chi ne aveva già uno — l'unico dato dell'app che non si può recuperare, perché lo
  si costruisce scrivendo. Le righe preesistenti prendono `NULL`, che è giusto: sono state
  imparate quando le maiuscole non si ricordavano.
- **Le lettere singole non si imparano** (`LearnedWordsEngine.isLearnable`). Cosa offre un
  tasto solo lo decide `SingleLetterEngine` dal tastierino, non la cronologia.

  **Il motivo è cambiato con la 2.3, la regola no** (decisione dell'utente, 04/08). Fino alla
  2.2 il motivo era che una parola imparata batteva l'intero corpus (`BASE_WEIGHT` 1.000.000),
  quindi una lettera finita lì dentro una volta sedeva in cima al suo tasto **per sempre**. Con
  i pesi della 2.3 non è più vero: una lettera imparata una volta peserebbe 200 contro i 15.038
  di `a`. Il motivo per cui la regola resta è la **prevedibilità** di un tasto premuto da solo:
  la spinta recente (+50.000) metterebbe una `b` appena scritta davanti ad `a` **per un'ora**,
  poi la lascerebbe ricadere — lo stesso sintomo della 2.2, non più permanente ma ricorrente a
  ogni uso.

  **Cosa non dipende da questa regola:** che un accento stia dopo la sua lettera semplice.
  `SingleLetterEngine` ordina su quel flag *prima* di guardare i pesi, quindi quella garanzia
  reggerebbe anche se le lettere singole diventassero imparabili.

  **La regola sta con i dati, non con il chiamante.** Fino alla 2.2 viveva nel servizio, e
  un chiamante che se ne dimenticasse la aggirava: è esattamente così che le lettere singole
  erano entrate prima della Fase 1.14.

- **Le lettere singole già memorizzate vengono buttate al caricamento, e cancellate**
  (`load()`). Una regola che vale solo per il futuro lascia il danno dov'è: chi aveva una `b`
  in archivio da una build vecchia se la sarebbe tenuta per sempre. Verificato sul dispositivo
  riproducendo lo stato — imparata una `b`, il tasto `2` proponeva `b a c à`; installata la
  build con la bonifica, **lo stesso database** legge `a b c à`.

### Elisioni

In italiano l'apostrofo dell'elisione **unisce due parole in una**: `l'albero`, `un'amica`,
`quest'anno`. Non è punteggiatura fra parole, è parte della parola — e ciò che l'utente ha
confermato scrivendo `l'aveva` è `l'aveva`, non `aveva`. Imparare la sola coda non è tanto
sbagliato quanto **inutile**: memorizza la metà che il dizionario già conosceva.

Al momento di confermare (spazio, invio, punteggiatura, o scegliendo un candidato) la
tastiera guarda quindi cosa precede la parola: se è una **testa elisa**, le due metà vengono
imparate insieme. `Elision` decide, per posizione, cosa è elisione e cosa virgoletta —
lettera da entrambi i lati è elisione, tutto il resto no, compreso il troncamento `po'`, che
non unisce niente.

Il guadagno si vede alla seconda scrittura: siccome `sequenceFor` salta l'apostrofo,
`l'aveva` sta nel dizionario personale sotto `528382` e si ottiene **digitando solo le
lettere**, con l'apostrofo scritto dalla tastiera.

**Una coda di una lettera sola viene unita solo se accentata** (Step 2.4). Senza questa
regola, scrivere `l` + apostrofo + `a` componeva `l'a` — tre caratteri, quindi imparabile —
che finiva sulla sequenza `52` accanto a `la`; riprodotto sull'emulatore, per l'ora della
spinta recente la barra offriva **`l'a` prima di `la`**.

La regola guarda l'accento e non la lunghezza perché la lunghezza non basta: **`c'è` ha
esattamente la forma di `l'a`**, una lettera unita a una lettera. A separarle è che le
elisioni corte che l'italiano ha davvero — `c'è`, `n'è`, `s'è`, `v'è` — finiscono tutte per
vocale accentata, mentre la spazzatura è sempre una vocale semplice (`l'a`, `l'e`, `l'o`).

E `c'è` va protetta proprio qui: **nessun corpus contiene apostrofi**, né l'italiano né
l'inglese, quindi essere imparata è l'unico modo in cui possa comparire fra i candidati.
Scritta una volta, si riscrive con **due tasti** dalla sequenza `23`.

**Limite noto:** l'adozione di una parola sotto il cursore (§4) si ferma ancora
all'apostrofo, perché `ComposeState` associa una cifra a ogni lettera e non sa rappresentare
un carattere che non si digita. Parcheggiando il cursore dopo `l'aveva` si adotta `aveva`.

---

## 7. Maiuscole, accenti, emoji

### Maiuscole — `ShiftState`

`OFF → ONCE → LOCK → OFF`, ciclato da `⇧`. In T9 la maiuscola vale per la **parola**, non per
il singolo tasto (le lettere le decide il dizionario): `ONCE` capitalizza la parola in corso e
si consuma alla conferma (`afterCommit()`), `LOCK` scrive tutto maiuscolo finché non lo
spegni. Il glifo diventa `⇪` e passa dal teal al bianco quando è attivo.

**Il glifo è disegnato più grande dei suoi vicini — 26sp contro 20sp (Step 2.5).** Non è una
preferenza: `⇧` è una freccia di contorno con quasi tutto il suo riquadro vuoto, mentre `⌫` e
`☺` sono pieni, e alla stessa misura nominale la freccia si legge come il tasto più piccolo
della colonna — proprio quello che deve dire in che maiuscole si sta per scrivere. La misura
nominale è l'unica leva che c'è: il glifo si allarga crescendo.

**Punto chiave:** la maiuscola si applica **all'ultimo momento**, in `currentPreview()`;
composizione e dizionario restano minuscoli, così apprendimento e lookup non sono influenzati
dallo shift.

#### Una maiuscola in mezzo alla parola (Step 3.6)

`ONCE` e `LOCK` sanno fare due cose: la prima lettera, o tutte. Non c'è uno stato «questa lettera
qui», e non potrebbe essercene uno in generale — nella scrittura predittiva si premono cifre e le
lettere le sceglie il dizionario, quindi «la prossima lettera» non indica niente.

**Ma mentre si forza dalla colonna sì**, perché lì l'utente sta dettando la parola una lettera
per volta. Quindi, **passata la prima lettera**, `⇧` vale per la lettera che si sta per scegliere:
`x` → `⇧` → `D` dà **`xD`**. È l'unico modo di scrivere `iPhone` o `McDonald`, che nessuna regola
sulla prima lettera può raggiungere.

`ComposeState` tiene le posizioni capitalizzate a mano. **La posizione 0 non è mai fra queste**:
la prima lettera è affare di `ShiftState`, e due meccanismi che rispondono dello stesso carattere
sono due meccanismi che prima o poi si contraddicono. Un `⇧` singolo si consuma sulla lettera
scelta, e mentre si forza non capitalizza all'indietro la prima lettera — si vedrebbe la parola
lampeggiare in una forma diversa da quella che si sta scegliendo.

**Ciò che si vede è ciò che si scriverà.** `appliesToNext(atWordStart)` decide, e il service
lo chiama **due volte con posizioni diverse**: i tasti scrivono il carattere dopo l'ultima
cifra premuta (`state.isEmpty()`), la colonna risolve la prima posizione non ancora risolta
(`!state.isForcing()`). Con la maiuscola singola le due cose non sono sempre a inizio parola
insieme, ed è giusto che si comportino diversamente: premuto `⇧` i tasti passano a `ABC/DEF/…`,
alla prima cifra tornano minuscoli perché il resto della parola lo sarà.

### Vocali accentate — nella colonna

`T9Keypad.accentedLetters`: 2→à, 3→è/é, 4→ì, 6→ò, 8→ù, offerte **dopo** le lettere normali del
tasto (`columnLetters(digit)`); la colonna scorre già, quindi non serve altro spazio.

Non entrano in `letters` di proposito: non devono toccare le etichette dei tasti né
`sequenceFor` (che gli accenti li ripiega comunque). La sequenza resta pulita, quindi "perché"
si cerca e si impara come qualsiasi altra parola.

**Perché la colonna:** la colonna *è* già il meccanismo "quale lettera esattamente", è sempre
visibile e non aggiunge UI né conflitti col long-press dei preferiti. Dallo Step 1.12 le
accentate si raggiungono **anche** dal popup del tasto (§10), che però fa la stessa,
identica operazione — non è una seconda strada con regole proprie.

### Emoji — `EmojiLayout`

Un pannello di 32 emoji comuni, otto per riga (più spazio dei simboli, per restare
riconoscibili). È **un altro `KeyGrid`**: nessuna vista nuova, solo una voce in `KeyboardMode`.

**È un pannello base, e 32 sono poche:** una pagina sola, senza categorie, recenti, ricerca né
toni della pelle. Ampliarlo è in Fase 3, con la decisione da prendere prima — a pagine come i
simboli o a categorie con una barra propria.

🐞 **Difetto noto:** `❤️`, `✌️` e `☀️` sono disegnate a 18sp invece di 22 e si vedono più
piccole delle vicine. `KeyViewFactory.labelSize` dà 22sp ai glifi di **un solo code point**, e
quelle tre portano il selettore di variazione `U+FE0F` — che le rende due. Non corretto: vedi
la voce in `DEVELOPMENT.md`, dove sta anche la riga che lo sistemerebbe.

---

## 8. Cancellazione (tap, tenuto premuto, parole)

`⌫` si comporta a tre livelli, gestito in `KeyViewFactory.attachHoldToDelete`:

| Fase | Effetto |
|------|---------|
| Tap | Una sola cancellazione (pop di una coppia, o un carattere nel campo) |
| Tenuto oltre 400 ms | Un carattere ogni 55 ms |
| Dopo 10 caratteri | **Parole intere** ogni 140 ms |

I 400 ms sono abbastanza perché un tocco normale non inneschi mai la ripetizione. Il tasto è
gestito **interamente a eventi touch** (niente click listener), così un tocco singolo cancella
esattamente una volta; lo stato "premuto" è pilotato a mano perché gli eventi vengono
consumati.

`KeyAction.DeleteWord` **non sta su nessun tasto**: è ciò in cui il tenere premuto si
trasforma. Cancella la parola in corso di composizione se c'è, altrimenti la parola prima del
cursore **spazi finali inclusi**, così la seconda ripetizione non si limita a mangiare lo
spazio lasciato dalla prima.

---

## 9. Simboli preferiti nella colonna

A riposo la colonna mostra **7 simboli preferiti** (`@ ? ! / - ' "` di default), quindi scorre.
**Tap** = inserisce. **Long-press** = apre le pagine simboli per sostituire *quella* posizione,
con la barra dei suggerimenti che spiega cosa sta aspettando ("Scegli il simbolo per la
posizione 2").

Mentre la scelta è in sospeso (`pendingFavouriteSlot`) i tasti simbolo **assegnano invece di
scrivere**; `1/2` resta navigabile perché restare dentro le pagine simboli tiene aperta la
domanda; qualsiasi altro tasto (incluso `abc`) annulla e si comporta normalmente.

**Riordino senza drag&drop:** se il simbolo scelto è **già** fra i preferiti, i due slot si
**scambiano** (`FavouriteSymbols.replace`). Un long-press + un tap spostano un preferito dove
vuoi, senza duplicati e senza perdere nulla.

`FavouriteSymbols.normalize` riporta sempre la lista a esattamente `COUNT` slot usabili: una
preferenza corrotta o vecchia (salvata quando `COUNT` era più piccolo) non può svuotare la
colonna.

---

## 10. Popup a pressione prolungata

Tenendo premuto un tasto per **400 ms** si apre un pannello di alternative; si scorre il dito
e si rilascia sulla scelta. Un tocco normale resta esattamente quello che era.

**Il dito non copre mai il pannello.** Non si scorre *sopra* le alternative — la mano
nasconderebbe proprio i caratteri fra cui si sta scegliendo. Il dito resta sulla tastiera e la
selezione lo segue **più in alto**, come su Gboard: il punto usato per la scelta è la
traduzione del dito dentro il pannello, che parte dalla **cella preselezionata** (vedi sotto).

**Lo zero del gesto è il dito, non il tasto.** Lo spostamento si misura da dov'era il dito
quando il pannello si è aperto: dove dentro il tasto è caduta la pressione non deve spostare
il punto di partenza. Rende anche il gesto identico per un tasto di bordo, il cui pannello
viene rientrato nello schermo e quindi non è più centrato sopra di lui.

**Il movimento è amplificato**, ma **non allo stesso modo sui due assi**:
`HORIZONTAL_GAIN` ×1.5, `VERTICAL_GAIN` ×2.5. Con un inseguimento 1:1 il pannello andava
attraversato alla sua misura reale — la riga sopra costava un passo di riga (l'altezza di un
tasto) e l'estremità di una riga da 5 due larghezze di cella — cioè bisognava portare il dito
sul pannello, proprio ciò che l'inseguimento dal basso esiste per evitare.

I due assi hanno guadagni diversi perché hanno problemi diversi: **di lato** le distanze sono
brevi (una cella) e troppo guadagno rende l'evidenziazione nervosa; **in su** il dito deve
coprire un passo di riga *e* restare fuori da un pannello che gli sta appena sopra. Valori
tarati a mano, prima sull'emulatore e poi sul telefono (2/2 risultava troppo veloce di lato e
ancora insufficiente in su). Oggi, su un pannello 3+2: riga sotto ~20dp, estremità della riga
~63dp.

**Il pannello si stacca dal tasto di `POPUP_GAP_DP` (10dp)**, che è spazio sottratto alla
mano. Non serve però alla **prima fila di tasti**: lì il pannello è già appoggiato al bordo
superiore della tastiera e non può salire oltre — la finestra dell'IME è tutto lo spazio che
c'è, e un pannello a due righe (~108dp) non entra sopra un tasto che dista 61dp dal bordo. Su
quella fila il dito finisce comunque sotto al pannello, ed è il guadagno verticale a
compensare.

La geometria di partenza si ricava dal pannello reale (centro dell'ultima riga e centro del
pannello, misurati a schermo), quindi resta corretta anche quando il pannello viene rientrato
perché non ci starebbe.

Finché il dito non si è mosso davvero (10dp) è selezionata **la prima cella, sempre**. Su un
tasto numerico è la cifra: tenerlo premuto e rilasciare **scrive il suo numero**, senza mirare
— la via più corta che ci sia a una cifra su un tastierino che non ha tasti 0–9. Il pannello
si apre già evidenziato, quindi il comportamento si vede prima di produrlo.

**Nessuna eccezione, nemmeno il `.`.** Per un momento (Step 1.12g–i) il pannello dei preferiti
non preselezionava niente, non avendo un default ovvio. Una regola sola batte un'eccezione
difendibile: un pannello che si comporta come tutti gli altri non ha bisogno di spiegazioni, e
il primo preferito è un default che l'utente ha già scelto, visto che i preferiti sono
ordinati e configurabili.

**Per annullare** si scorre via dal pannello e si rilascia dove non è selezionato niente. Fino
allo Step 1.12f bastava rilasciare senza muoversi; è il prezzo della scorciatoia, pagato
consapevolmente.

**Il punto di partenza è la cella preselezionata**, non un punto qualsiasi del pannello: il
primo movimento scorre via da lì invece di saltare altrove. Fino allo Step 1.12h i due
posti erano diversi (evidenziata la prima cella, ma il gesto partiva dal centro della riga in
basso) e superare i 10dp teletrasportava la selezione — su `jkl` (`5 j k l`) un colpetto a
destra andava da `5` direttamente a `k`, scavalcando `j`.

Poiché la cella preselezionata è la prima, cioè **in alto a sinistra**, il resto del pannello si
raggiunge andando a destra e **in giù**. È il compromesso scelto: un gesto che parte da dove
l'occhio già guarda, invece che dal punto più vicino al dito.

### Cosa offre ciascun tasto

| Tasto | Popup |
|-------|-------|
| `2`–`9` | La **cifra**, poi le lettere del tasto, **accenti inclusi** (`3` · `d e f è é`) |
| `1` | `1` · `@` `()` `/` `%` `+` `=` `€` — ciò che le altre superfici rendono costoso |
| `1` in campo email/URL | `1` · `@` `.com` `.it` `.net` `.org` `/` |
| `,` | `0` · `,` `;` `:` `"` |
| `.` | I 7 **simboli preferiti**, gli stessi della colonna |
| `⌫`, `space`, `⇧`, `⏎` | Nessuno |

### Le due semantiche

Un popup non fa sempre la stessa cosa, e non potrebbe: sui tasti 2–9 le lettere le decide il
dizionario, quindi infilare una `à` grezza a metà composizione romperebbe l'invariante di
`ComposeState` — il campo mostrerebbe una cosa e la sequenza ne conterrebbe un'altra.

- **Celle lettera → `KeyAction.ForceLetter(digit, letter)`**: forzano quella lettera, cioè
  **la stessa operazione del tap sulla colonna**. Il popup è una scorciatoia posizionale della
  colonna, non un secondo meccanismo con regole proprie. Le lettere vengono da
  `T9Keypad.columnLetters`, la fonte di verità che la colonna già usa, quindi i due **non
  possono divergere**. La cifra viaggia dentro l'azione perché non è sempre derivabile dalla
  lettera: le vocali accentate non stanno nella mappa inversa lettera→cifra.
- **Tutto il resto → `Insert`** (o `InsertPair`): conferma la parola in corso e scrive.

**Un dettaglio di correttezza che non si vede ma conta.** `chooseLetter` risolve la *prima*
posizione non ancora risolta. Forzare una lettera dal popup dopo aver digitato "cas" in
predittivo avrebbe quindi risolto la posizione 0 con quella lettera, trasformando la parola in
tutt'altro. Prima di appendere, `resolvePendingFromPreview()` chiude le posizioni ancora
aperte **con ciò che il campo sta già mostrando**: la parola non cambia sotto le mani
dell'utente.

### Le cifre

Ogni tasto numerico offre la **propria cifra come prima cella**, resa in teal come il
numerino d'angolo che rappresenta. Non è un abbellimento: il tastierino **non ha tasti 0–9**
(i numeri sulle facce sono etichette), quindi prima dello Step 1.12 un numero si poteva
scrivere solo passando da `12#`.

**Prima e non ultima** (scelta dell'utente, Step 1.12f): un pannello può stare su una riga o
su due, e solo la cella d'apertura significa la stessa cosa in entrambi i casi — l'ultima
passa da "fine della riga" a "in basso a destra" appena la lista va a capo.

Ed essendo la prima è anche quella **preselezionata all'apertura** (Step 1.12g): tenere
premuto un tasto numerico e rilasciare scrive il suo numero, che è il percorso più corto
possibile. Vedi sopra, "il dito non copre mai il pannello".

Lo `0` sta sul popup della **virgola**, che infatti ora mostra `0` nell'angolo. In ITU-T E.161
la sua casa sarebbe la barra spazio, ma quel long-press è **riservato allo scorrimento del
cursore** (Fase 3), che vale più di un percorso più breve per una cifra.

### Coppie

La cella `()` inserisce **entrambe le parentesi con il cursore in mezzo**
(`KeyAction.InsertPair`): apri, scrivi, chiudi in un gesto solo. Implementata con
`commitText(open, 1)` + `commitText(close, 0)` — una posizione non positiva è misurata
dall'inizio del testo inserito, quindi il cursore atterra in mezzo senza alcun calcolo di
posizione assoluta.

### Come è costruito

- **`LongPressKeys`** — quali celle per quale tasto: dati puri, quindi testabili. I preferiti
  e il tipo di campo arrivano dall'esterno, non vengono letti qui.
- **`KeyPopupView`** — il pannello. **Non riceve mai eventi propri:** il dito appartiene al
  tasto che lo ha aperto, che gli inoltra le coordinate finché non si solleva. È ciò che rende
  il gesto uno solo, ed è il motivo per cui può essere una normale vista figlia invece di un
  `PopupWindow`: nessun token di finestra, niente che possa sopravvivere alla tastiera.

  **Oltre 4 celle va a capo** (`LongPressKeys.MAX_PER_ROW`), su righe bilanciate e centrate:
  otto celle in fila coprivano quasi tutto lo schermo. I popup da cinque (`2 a b c à`,
  `7 p q r s`, `6 m n o ò`) diventano quindi **3+2**.

  La soglia è scesa da 5 a 4 nello Step 1.12k, e non per gusto: partendo il gesto dalla prima
  cella, l'ultima di una riga da cinque sta **quattro passi di cella** più a destra, ~125dp di
  corsa del dito. Sulla **colonna destra del tastierino** (`6`, `9`) restano solo ~117dp di
  schermo a destra del tasto, quindi quella cella non era raggiungibile senza uscire dal
  bordo. Andando a capo la corsa orizzontale si dimezza e la differenza si paga con un passo
  in giù, dove lo spazio c'è.

  Non meno di 4: `rows()` bilancia, quindi a 4 un pannello da sei celle resta 3+3 e uno da
  otto 4+4, mentre a 3 il popup di `1` (otto celle) diventerebbe di **tre righe** — più alto,
  e l'altezza è la dimensione che scarseggia.

  La cella sotto il dito si **riempie di teal** con il glifo invertito allo scuro del tema,
  come su Gboard — non un pallino, che coprirebbe il carattere proprio mentre lo si legge.
  L'inversione serve: la cella cifra è già teal e sparirebbe dentro il proprio evidenziatore.

  La cella scelta è la **più vicina** al puntatore tradotto, non la prima che lo contiene:
  con due righe qualsiasi tolleranza attorno a una cella invaderebbe la riga accanto, e un
  test di contenimento risponderebbe sempre con la riga visitata per prima. La tolleranza è
  generosa di lato (scorrere oltre il fondo di una riga ne prende comunque l'ultima cella) e
  più stretta in verticale, così allontanarsi molto restituisce "nessuna scelta" invece di
  restare aggrappati al bordo più vicino.
- **`KeyViewFactory.PopupHost`** — il gesto. Un tasto senza alternative è lasciato del tutto
  in pace: il listener declina al `DOWN` e il click normale funziona come prima. Quando invece
  prende il controllo, un rilascio senza pannello aperto chiama comunque `performClick()`.
- **`KeyboardView`** — è un `FrameLayout` proprio per questo: il corpo tastiera è un figlio, e
  il popup un fratello disegnato sopra, centrato sul tasto e **rientrato ai bordi** (un tasto
  di prima colonna spingerebbe fuori schermo un pannello più largo di sé).

Le celle lettera seguono le maiuscole come tasti e colonna; la cella cifra no. Come sempre
solo le *etichette* sono maiuscole: l'azione porta la lettera minuscola, così composizione e
dizionario restano indifferenti allo shift.

---

## 11. Maiuscole automatiche e spazio automatico

Due aiuti alla scrittura che si sostengono a vicenda: la maiuscola serve a inizio frase, e lo
spazio automatico è ciò che crea quel confine di frase. Entrambi hanno già la loro preferenza
in `KeyboardSettings` (`autoCapitalise`, `autoSpace`, accese di default), pronte per la
schermata impostazioni di Fase 3 — un aiuto automatico è esattamente il genere di cosa che
qualcuno vuole poter spegnere.

### Dove gli aiuti tacciono (`FieldRules`)

**Prima regola, perché è quella che evita danni.** Maiuscole e spazi aiutano nella prosa e
rovinano tutto il resto: uno spazio dopo il punto in `nome.cognome@posta.it`, una maiuscola
all'inizio di una password, uno spazio dentro `3,14`. Il campo dichiara che tipo è, quindi si
aiuta **solo dove il campo è testo semplice**: fuori — email, URL, password, numeri, telefono,
date, e i campi che chiedono esplicitamente nessun suggerimento (terminali, editor di codice)
— entrambi gli automatismi sono spenti.

### Maiuscola automatica

La domanda "qui ci va la maiuscola?" la risponde **Android**, non noi:
`InputConnection.getCursorCapsMode(inputType)`. Così si coprono senza casi speciali l'inizio
del campo, l'inizio riga, la parola dopo `.`, `!` o `?` — e **gratis** i campi che chiedono
ogni parola maiuscola (il nome in una rubrica) o tutto maiuscolo, che diventano
rispettivamente `ONCE` e `LOCK`.

Viene ricalcolata dove il contesto può essere cambiato: apertura di un campo, conferma di una
parola, cancellazione (tornare indietro oltre un punto rimette la maiuscola) e spostamento del
cursore (`onUpdateSelection`).

**Due cose il sistema non può sapere, e le mette `SentenceRules`:**

- **Le abbreviazioni.** Per Android `ecc.` è un punto seguito da spazio, quindi "maiuscola".
  In italiano è falso abbastanza spesso da dare fastidio: `ecc.`, `dott.`, `pag. 12`, `p.v.`
  Il confronto avviene sul **token prima del punto**, così `p.v.` funziona nonostante il punto
  interno. (`vedi` non ha punto: le forme che lo hanno sono `v.` e `vd.`, ed è quelle che la
  lista contiene.)
- **Le virgolette e le parentesi di apertura.** In `Ciao. «Come stai` la maiuscola appartiene
  alla parola dentro le virgolette, non al simbolo.

**La parte delicata non è quando mettere la maiuscola, ma quando *non* toccarla.** Spegnere
`⇧` a inizio frase è un gesto deliberato, e una tastiera che la riaccende subito dopo sta
litigando con chi scrive. La regola è la **proprietà** (`AutoShift.resolve`): la tastiera può
cambiare solo ciò che ha impostato lei; uno stato scelto dall'utente resta finché quella parola
non è confermata, dopodiché la parola successiva è una decisione nuova.

### Nomi propri (`ProperNouns`)

Alcune parole vogliono la maiuscola ovunque cadano: `Roma`, `Italia`, `Natale`. È una proprietà
della **parola**, non della posizione del cursore, quindi si applica nell'anteprima e nella
barra dei candidati — una proposta che legge "roma" e atterra come "Roma" farebbe sembrare che
la tastiera abbia cambiato idea.

**L'elenco non è scritto a mano: è misurato.** In costruzione, `tools/BuildDictionary.java`
conta per ogni parola la **quota di occorrenze maiuscole** nel corpus che le conserva (quello
giornalistico), prima di unire le varianti; sopra il **90%** (con almeno 30 occorrenze,
altrimenti è rumore) la parola prende il flag `P` nel dizionario. Sono **442 nomi propri
ricavati dalle prove**, e la differenza rispetto a una lista compilata a mano si vede
esattamente dove una lista sbaglierebbe:

| Parola | Maiuscole nel corpus | Esito |
|---|---|---|
| `roma` | 99% (831 occorrenze) | **Roma** |
| `milano`, `italia`, `pasqua` | 100% | maiuscole |
| `rosa` | 17% | resta il fiore |
| `viola` | 27% · `bianca` 44% | restano colore e aggettivo |
| `prato` 72% · `camera` 65% | sotto soglia | restano nomi comuni |
| `marzo` 5% · `domenica`, `lunedì` | bassissime | mesi e giorni **minuscoli**, senza bisogno di eccezioni |

Le maiuscole di inizio frase, che gonfiano la statistica di ogni parola, arrivano al massimo
intorno al 20% (`il` 18%, `quando` 19%): abbondantemente sotto la linea.

**Una lettera sola non è mai un nome proprio**, qualunque cosa abbia misurato il corpus. Il
corpus flagga davvero `b` e `c`: nella prosa giornalistica una lettera isolata è un'iniziale
(`B. Rossi`) o un marcatore di elenco (`a) b) c)`), mai la lettera. Lasciato passare, premere
`2` offriva **"a B C à"** — e le maiuscole si leggono come le opzioni importanti quando la
risposta è la lettera semplice. Il flag viene quindi scartato in costruzione per le parole di
un carattere. È lo stesso principio già applicato in `learn()` e in `SingleLetterEngine`: a un
carattere il corpus smette di misurare ciò che gli stiamo chiedendo.

L'insieme arriva **con il corpus**, sul thread di caricamento, un istante dopo la comparsa
della tastiera; fino a quel momento nessuna parola viene capitalizzata da sola — il modo
innocuo di sbagliare. E nei campi della blacklist i nomi propri restano minuscoli come tutto
il resto: `casa.milano` in un campo email non diventa `casa.Milano`.

### Spazio automatico

Scegliere un candidato dalla barra inserisce anche **lo spazio dopo**, così la parola seguente
si comincia subito. Solo lì: premere spazio o invio significa che il separatore lo sta già
mettendo l'utente, e aggiungerne un altro lo raddoppierebbe.

**Lo spazio è provvisorio,** ed è questo a decidere se la funzione piace o si disattiva subito:
senza una regola, scegliere "casa" e digitare un punto lascerebbe `casa .` e costringerebbe a
cancellare uno spazio mai chiesto. Quindi la punteggiatura che sta attaccata alla parola
precedente (`. , ; : ! ? …` e le chiusure `) ] } » "`) **si riprende lo spazio**; se poi è un
segno che chiude una frase, uno spazio nuovo va **dopo** — che è dove comincia la frase
successiva e dove la maiuscola automatica poi cade.

Un segno porta lo spazio **solo dopo una lettera** (`AutoSpace.deservesFollowingSpace`): è ciò
che tiene interi `3,14`, `10:30` e `www.sito.it`, dove uno spazio sarebbe attivamente sbagliato.
La blacklist dei campi non basterebbe: un orario si scrive dentro un messaggio, in un campo di
testo normale.

**Le altre regole della spaziatura italiana:**

| Caso | Comportamento |
|------|---------------|
| Aperture `( [ { «` | Spazio **prima** (se lì finisce una parola), mai dopo |
| Chiusure `) ] } »` | Niente prima, spazio **dopo** |
| `"` | È l'unico simbolo che apre *e* chiude: dopo una lettera o una cifra può solo chiudere, altrove apre |
| Apostrofo `'` | **Nessuno spazio, mai**: in italiano unisce le parole (`l'albero`, `un'amica`) |
| `...` | Trattati come un segno solo: il secondo punto non prende spazio, il terzo sì |
| Abbreviazioni | Il punto di `ecc.` non è una fine frase e non ne prende la spaziatura |
| Spazio esplicito dopo uno automatico | Ignorato: resta un solo spazio |

Il carattere inserito resta **quello digitato**: per le virgolette dritte il ruolo (apre o
chiude) serve solo a decidere gli spazi, non a sostituirle con quelle tipografiche.

### Annullare con il backspace

Un backspace subito dopo una maiuscola automatica significa "non quella maiuscola": la
disinnesca, e non la riarma un istante dopo. Ma **cancella comunque un carattere**, perché una
pressione che non produce nulla di visibile sembra un tocco perso. Lo spazio automatico non ha
bisogno di un caso speciale: *è* l'ultimo carattere, quindi una cancellazione normale toglie
esattamente lui.

**Ci si fida del campo, non del flag.** Prima di togliere lo spazio si verifica che davanti al
cursore ci sia davvero uno spazio: il flag può sopravvivere al suo spazio (cursore spostato,
testo riscritto dall'app) e cancellare un carattere per una convinzione stantia significherebbe
mangiare qualcosa che l'utente ha scritto. Per lo stesso motivo `onUpdateSelection` **non**
azzera il flag: lì arrivano anche le nostre modifiche, e azzerarlo disferebbe la funzione un
istante dopo che ha agito.

---

## 12. Impostazioni e persistenza

Due meccanismi, scelti in base a ciò che devono reggere:

| Cosa | Dove | Perché |
|------|------|--------|
| Parole imparate | **Room** (`learned_words.db`) | Migliaia di righe, query per sequenza, conteggi da aggiornare |
| Simboli preferiti | **SharedPreferences** (`KeyboardSettings`) | Sette valori letti una volta e scritti a un tocco: un database sarebbe solo costo |
| Maiuscola e spazio automatici | **SharedPreferences** (`autoCapitalise`, `autoSpace`) | Due interruttori, accesi di default, mostrati dallo Step 3.1 |

Entrambi restano nella sandbox dell'app.

### La schermata — `SettingsActivity`

**Due strade per arrivarci:** l'icona nel launcher e — dallo Step 3.1 — una **rotellina sopra la
colonna di disambiguazione**, all'altezza dei candidati. La seconda esiste perché la prima
obbliga a *uscire dall'app in cui si sta scrivendo* per cambiare come si scrive.

La rotellina usa gli stessi pesi e lo stesso rientro di 3dp di `T9BodyView` (0.9 su 7.4), quindi
sta **sopra** la colonna e non genericamente vicino; occupa la corsia che è già "non lettere",
così niente si è dovuto spostare. Resta visibile anche dove la barra dei candidati non lo è
(simboli, emoji). Il glifo è `⚙︎` con `U+FE0E`, la variante *testuale*: senza, Android lo
disegnerebbe come emoji a colori, l'unica cosa colorata di tutta la tastiera.

`KeyboardView` non conosce le Activity — riceve una lambda `onSettings` come per ogni altro
tasto, e apre il servizio. Là servono due cose non negoziabili: **`FLAG_ACTIVITY_NEW_TASK`**
(una tastiera è un servizio e non ha un task proprio: senza il flag `startActivity` lancia) e
**`requestHideSelf`** (la schermata non è un campo di testo, e una tastiera aperta sopra
coprirebbe i comandi per cui ci si è andati).

**Cosa contiene, in ordine di quanto spesso si cambia:**

| Sezione | Comandi |
|---|---|
| Scrittura | Maiuscola automatica a inizio frase, spazio automatico dopo un candidato |
| Vibrazione | Durata da 0 a `MAX_HAPTIC_MS` (60 ms); `0` = spenta |
| Lingue | Le secondarie dichiarate in `Language.SECONDARIES` |

Le lingue erano prime solo perché erano arrivate prime (2.1): gli aiuti alla scrittura sono
quelli che si provano, non piacciono e si riprovano, una lingua si mette una volta.

**Lo slider della vibrazione vibra**, ed è il punto. 12 ms e 24 ms sono lo stesso numero da
guardare e due tastiere diverse da usare, quindi lo slider chiama la `Haptics` vera — lo stesso
percorso di codice di una pressione — e ciò che si sente lì è ciò che si sentirà scrivendo.
Vibra **al rilascio**: un colpo per pixel di trascinamento sarebbe un sonaglio, e coprirebbe la
cosa da giudicare.

#### L'anteprima viva (Step 3.2)

In fondo alla schermata sta una **tastiera vera**: `KeyboardView`, la stessa classe che mostra
l'IME, con le callback che non vanno da nessuna parte. Non un disegno, non un mock-up. È
possibile perché `KeyboardView` è un normale `FrameLayout` che prende un `Context` e delle
lambda, e non ha mai saputo nulla di `InputMethodService`.

Serve alle **misure**: altezza della tastiera e dimensione del testo dei candidati (Step 3.3)
non si scelgono alla cieca, e qui la cosa misurata sta sulla stessa schermata del comando che la
misura. Ma si ripaga già prima: i tasti sono vivi, quindi la durata della vibrazione si giudica
**premendo un tasto**, non dal solo colpo al rilascio del cursore — stesso `KeyViewFactory`,
stessa `Haptics`.

Tre dettagli che sembrano piccoli e non lo sono:
- **La rotellina dell'anteprima non porta da nessuna parte.** È già la schermata impostazioni: un
  comando che riapre la schermata su cui sei è una trappola.
- **`FOCUS_BLOCK_DESCENDANTS`**: ogni tasto è focalizzabile, perché nell'IME sono tasti veri. In
  un modulo diventerebbero tappe del giro del fuoco. Un'anteprima si guarda e si preme, non si
  attraversa col tab.
- **L'inset della barra di stato lo reclama la vista più esterna**, che dalla 3.2 è il
  contenitore e non più la `ScrollView`. Il basso resta della tastiera, che legge da sé l'inset
  della barra di navigazione.

#### Le dimensioni (Step 3.3)

Due cursori sopra l'anteprima, che si muove sotto le dita mentre li si sposta.

| Comando | Intervallo | Default | Dove finisce |
|---|---|---|---|
| Altezza tastiera | 22–40% dello schermo | **29%** | `KeyboardSettings.bodyHeightPercent`, letta a ogni `KeyboardView.onMeasure` |
| Testo dei candidati | 12–24 sp | **17 sp** | `SuggestionBarView.textSizeSp` |
| Larghezza tasti | 60–100% dello schermo | **100%** | `KeyboardSettings.keyboardWidthPercent` |
| Tasti a sinistra | interruttore | **spento** | `KeyboardSettings.keyboardOnLeft` |

**Il riproporzionamento uniforme è gratis**, ed è il dividendo della scelta di costruire la
disposizione con **pesi** invece che con misure: ogni tasto è una quota di qualunque altezza
venga fuori, quindi muovere il totale riproporziona tutto insieme invece di stirare una riga.

**I limiti hanno una ragione.** Il minimo del 22% sta sopra il pavimento che lo Step 1.25 aveva
trovato scendendo troppo (e da cui la 2.5 è dovuta risalire); il massimo del 40% è dove i tasti
tornano **più alti che larghi**, la forma che la 1.25 ha misurato e scartato. Il tetto di 24 sp è
quanto sta ancora nella striscia dei candidati, che **non** cresce col testo — `BAR_DP` è fisso e
la 1.25 aveva dimensionato le due cose insieme apposta.

**Il default dichiara il suo arrotondamento:** gli Step 1.25 e 2.5 avevano misurato **28,7%**, e
un cursore che si muove in punti interi non può tenerlo. 29% è circa 3px più alta su uno schermo
da 2856 — meno della riga fra due tasti.

**I cursori di dimensione scrivono e ridisegnano a ogni movimento**, al contrario di quello della
vibrazione che agisce al rilascio. Non è un'incoerenza: un colpo va sentito uno per volta, una
dimensione va *guardata cambiare*. Il prezzo è una scrittura nelle preferenze per pixel di corsa,
piccolo e dichiarato; l'alternativa sarebbe un disaccordo fra ciò che si vede e ciò che è
memorizzato per tutta la durata del trascinamento.

**La larghezza stringe i tasti, non la tastiera (Step 3.4).** Il pannello scuro resta largo
quanto lo schermo — è lo sfondo della `KeyboardView`, mentre a stringersi è il solo `content` con
una `gravity` a destra o a sinistra. Serve ad **avvicinare i tasti al pollice** su uno schermo
grande, non a rimpicciolirli: colonna, tasti, riga dello spazio, barra dei candidati e rotellina
si stringono insieme, essendo tutti figli di `content` che se la divide a pesi.

Il default resta il 100% perché stringere **costa precisione** — gli stessi tasti con meno spazio
— e conviene solo a chi non arriva dall'altra parte dello schermo.

⚠️ **Da non confondere con la posizione della colonna** (ancora da fare): questa sposta la
tastiera *dentro lo schermo*, quella sposta la colonna *dentro la tastiera*. Sono complementari.

**Il limite dei popup segue i tasti, non la vista.** `positionPopup` tiene i pannelli long-press
dentro `content.left`/`content.right`. Finché i tasti erano larghi quanto la `KeyboardView` le
due cose coincidevano; con i tasti stretti, limitarsi alla vista lascerebbe un popup di bordo
scivolare nella fascia vuota accanto, staccato dal tasto che lo ha aperto.

Le nuove misure raggiungono la tastiera vera al rientro in un campo (`onStartInputView` →
`applySizeSettings`), lo stesso momento in cui si rileggono le lingue.

---

## 13. Copertura dei test

Unit test JVM (nessun emulatore necessario): `./gradlew :app:testDebugUnitTest`.

| Test | Copre |
|------|-------|
| `T9KeypadTest` | `sequenceFor`, fold accenti, e l'apostrofo: saltato nell'elisione (`l'aveva` → `528382`), rifiutato come virgoletta (`po'`, `'ciao`) |
| `ElisionTest` | La distinzione per posizione fra elisione e virgoletta, che la parola imparata sia quella intera (`l'aveva`, non `aveva`), e le due facce della regola sulla coda di una lettera: **`l'a` rifiutata**, **`c'è` tenuta** |
| `CorpusDictionaryEngineTest` | Costruzione indice, ordinamento per peso, ricerca per prefisso (fra cui il caso `contempora` → `contemporaneamente`), e che **una lettera sola non sia mai un nome proprio** |
| `MergingDictionaryEngineTest` | Fusione, deduplica per parola |
| `LanguagePriorityEngineTest` | 9 casi: che ogni parola italiana preceda ogni inglese (anche quando l'inglese pesa di più), che una parola comune a entrambe sia tenuta una volta sola, che l'inglese risponda da solo dove l'italiano tace, che una **terza lingua** si accodi alla seconda, e che senza secondarie resti esattamente la tastiera v1 |
| `LearnedWordsEngineTest` | Pesi, incremento usi, caricamento dallo store; la **forma scritta** (`xD` e `iPhone` tenuti, `Casa` e `CIAO` no, e una maiuscola ordinaria che non cancella quella voluta); e le lettere singole: **mai imparate**, e **cancellate** dall'archivio se ve le aveva messe una build vecchia; la curva dei pesi (abitudine con tetto + spinta recente che svanisce) e il **dimenticare** una parola |
| `FuzzyDictionaryEngineTest` | 14 casi: cancellazione/sostituzione/inserimento, **inversione di due tasti**, **due tasti sbagliati** (e che non si cerchino su parole corte né quando qualcosa già corrisponde), marcatura, tetto |
| `FuzzyCostTest` | Guardia sul costo: la ricerca profonda sul corpus vero (50k parole) resta abbondantemente dentro il tempo di una pressione |
| `CompletingDictionaryEngineTest` | 7 casi: che i tasti che non scrivono nulla offrano comunque la parola lunga, l'ordine **esatte → completamenti → refusi**, niente doppioni, la soglia delle 4 cifre e il tetto |
| `ComposeStateTest` | Forcing, avanzamento, pop-coppia, correzione, `defaultLetters`, accenti; e l'adozione di una parola scritta (`adopt`, anche accentata, con rifiuto di ciò che il tastierino non sa scrivere) più `forcedPreview`, che rende visibili le cifre premute dopo la forzatura |
| `ShiftStateTest` | Ciclo, `apply`, `afterCommit`, `appliesToNext` |
| `FavouriteSymbolsTest` | Normalizzazione, sostituzione, scambio, "ogni default è raggiungibile" |
| `SymbolLayoutTest` | Fra cui "un tasto inserisce esattamente ciò che mostra" |
| `LongPressKeysTest` | Contenuto dei popup, entrambe le semantiche, variante email; e **"ogni cifra 0–9 è raggiungibile da esattamente un popup"**, che impedisce sia il ritorno dei numeri intypabili sia una cifra offerta da due posti |
| `AutoSpaceTest` | Quale punteggiatura si riprende lo spazio e quale porta il proprio; e che dopo una cifra il punto non lo porti (`3.14`) |
| `AutoShiftTest` | La regola di proprietà: fra i casi, **"una maiuscola spenta dall'utente non viene riaccesa"** |
| `SentenceRulesTest` | Abbreviazioni (compreso `p.v.` col punto interno), virgolette di apertura, puntini di sospensione |
| `FieldRulesTest` | Dove gli aiuti tacciono: indirizzi, password, numeri, campi senza suggerimenti |
| `ProperNounsTest` | Che il flag del dizionario decida, e che le parole che sono **anche** nomi comuni (`rosa`, `viola`, `bianca`, `vera`) restino minuscole |
| `SingleLetterEngineTest` | L'ordine di un tasto solo: fra i casi, **"ogni lettera del tasto è offerta"** (era sparita la `q`) e **"un accento imparato non scavalca la lettera semplice"** |

**Cosa questi test non coprono:** `Haptics` e `KeyboardSettings` dipendono dalla
piattaforma (`Vibrator`, `Settings.System`, `SharedPreferences`) e non hanno unit test JVM.
Il ritorno tattile è verificato sull'emulatore leggendo il registro del servizio vibrazione
(`dumpsys vibrator_manager`), che è una verifica reale ma manuale.

Le verifiche su emulatore sono documentate step per step in `DEVELOPMENT.md`, con gli
screenshot in `docs/screenshots/`.

---

## Cosa non c'è ancora

Riferimento completo e ordinato: `DEVELOPMENT.md` (Fasi 2 e 3). In sintesi:

- **Bilingue IT+EN** (Fase 2, prossimo): manca solo `EnglishDictionaryEngine` + corpus, il
  merge esiste.
- **Scorrimento del cursore trascinando sulla barra spazio**: il long-press dello spazio è
  tenuto libero apposta (vedi §10).
- **QWERTY come layout alternativo**: `KeyGrid` e vista sono già pronti, manca la griglia.
- Impostazioni: posizione colonna, altezza tastiera, dimensione candidati, numero di preferiti,
  e gli interruttori per maiuscola e spazio automatici (le preferenze esistono già).
- Schermata di gestione del dizionario personale e rimozione di una parola imparata
  (il DAO ha già `delete(word)`).
- Microfono (`KeyAction.Mic`, oggi no-op e fuori dal layout attuale).
