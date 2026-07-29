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

**Allineato a:** Step 1.11 (Fase 1 completa).

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
- [10. Impostazioni e persistenza](#10-impostazioni-e-persistenza)
- [11. Copertura dei test](#11-copertura-dei-test)
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

**Privacy:** nessun permesso applicativo sensibile (niente rete/SMS/contatti). L'unico
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
e altezza complessiva (`BAR_DP` 56dp + 34% dell'altezza schermo).

Nelle modalità non-T9 la barra suggerimenti diventa **invisibile ma occupa il suo spazio**,
così l'altezza della tastiera non salta cambiando superficie.

### Modalità — `KeyboardMode` + `KeyAction.Mode(target)`

Il cambio di superficie è tipizzato: `T9`, `SYMBOLS_1`, `SYMBOLS_2`, `EMOJI`. Cambiando
modalità **la parola in corso viene confermata**: uscire dal tastierino a metà parola
lascerebbe un composing text che nessun tasto potrebbe più chiudere.

### `T9BodyView` — il corpo T9

```
┌──────┬───────────────────────────┬─────────┐
│ dis- │  @   abc   def            │   ⌫     │
│ amb. │  ghi jkl   mno            │   ⇧     │
│ col. │  pqrs tuv  wxyz           │   ☺     │
│      ├───────────────────────────┴─────────┤
│      │ 12# ,   [   space   ]  .   ⏎        │
└──────┴─────────────────────────────────────┘
```

Colonna di disambiguazione a sinistra (peso 0.9, si ferma sopra la riga inferiore), griglia
3×3 lettere al centro (5.4), colonna funzioni a destra (1.1), riga inferiore full-width e
**più sottile** (0.72). Tutto a pesi: nessuna dimensione fissa, quindi la stessa UI si
riproporziona fra S25 e S25 Ultra senza layout dedicati.

### `KeyGrid` + `GridKeyboardView` — la griglia riusabile

`KeyGrid` = lista di `KeyRow` (ciascuna con un peso di altezza), `KeyRow` = lista di
`KeySpec` (ciascuno con un peso di larghezza). **Deliberatamente non T9-specifica.**
`GridKeyboardView` disegna un `KeyGrid` qualsiasi: le pagine simboli e il pannello emoji
sono già solo dati, e la **QWERTY alternativa** (pianificata) sarà un `KeyGrid` in più,
non una vista nuova.

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
| `,` `.` | Conferma la parola e inserisce il segno |
| `1` | Attualmente conferma soltanto — il pannello punteggiatura è pianificato |
| `⇧` | Cicla le maiuscole (§7) |
| `12#` / `☺` | Cambia superficie |

**Barra suggerimenti** (`SuggestionBarView`): riga orizzontale scrollabile di chip; il primo
candidato è evidenziato in teal (è anche l'anteprima nel campo), i candidati fuzzy sono in
grigio. Tap = conferma quel candidato. Testo a 22sp (`DEFAULT_TEXT_SP`), esposto come
proprietà `textSizeSp` per essere pilotato dalle impostazioni in Fase 3.

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
- **Anteprima nel campo:** se stai forzando (`isForcing`) il campo mostra la parola forzata;
  altrimenti la migliore predizione **esatta**; se la sequenza è sconosciuta, le **lettere di
  default** (prima lettera di ogni tasto) — **mai le cifre** (`defaultLetters()`).
- **`⌫` = pop dell'intera coppia** (cifra + lettera insieme), mai solo il carattere: evita
  cifre "orfane" (piano §3.6). Se la coda ha una cifra non ancora risolta, rimuove quella.
- **Estendere** una parola e **correggere** l'ultima lettera sono la **stessa identica
  operazione** (backspace + ripressione + scelta): nessuna distinzione di codice.
- **Limite noto:** nessun editing in-place a metà parola; si cancella dalla coda e si
  ridigita (piano §3.9).

### Dimensionamento delle celle

Le celle **riempiono la colonna con 3–4 elementi** (il numero abituale di lettere); oltre 4
mantengono la misura da 4 e la colonna **scorre**. Il calcolo dipende dall'altezza della
colonna, ignota mentre la input view viene costruita, e gli elementi possono arrivare prima o
dopo quel momento: la ricostruzione avviene quindi in `onLayout`, che copre entrambi gli
ordini e la riapertura di una vista riusata (`onSizeChanged` non basterebbe, non scatta a
dimensione invariata).

**A riposo** la colonna non è spazio morto: mostra i simboli preferiti (§9).

---

## 5. Motori di dizionario

Tutto dietro `DictionaryEngine.lookup(sequence): List<Candidate>`. `Candidate(word, sequence,
weight, fuzzy)` — il `weight` è su scala confrontabile, così liste da sorgenti diverse si
fondono con un sort.

**La composizione attiva a regime:**

```
FuzzyDictionaryEngine
  └── MergingDictionaryEngine
        ├── LearnedWordsEngine   (dizionario personale, pesi ≥ 1.000.000)
        └── ItalianDictionaryEngine (corpus Leipzig, 50k parole)
```

### `ItalianDictionaryEngine` — il corpus

Indice in RAM `Map<sequenza, [Candidate ordinati]>`: il lookup durante la digitazione non fa
I/O. Sorgente: `assets/dict/it.txt`, **50.000 parole reali** dal corpus **Leipzig
`ita_news_2022_100K` (CC BY-4.0)**, generato da `tools/ConvertLeipzig.java` (filtra parole
italiane, unisce le varianti maiuscole/minuscole sommando le frequenze, tiene le top 50k).
Caricato in background; i lookup prima del completamento ritornano vuoto. A questa dimensione
il formato testo è adeguato: il binario indicizzato resta un'ottimizzazione futura.

Rigenerazione (se serve un taglio diverso):
```
JAVA="/c/Program Files/Android/Android Studio/jbr/bin/java.exe"
"$JAVA" tools/ConvertLeipzig.java <path/...-words.txt> app/src/main/assets/dict/it.txt 50000
```

### `MergingDictionaryEngine` — l'unione

Fonde più dizionari dietro l'unica `lookup`, deduplicando per parola (tiene il peso più alto).
Serve oggi per personale+corpus e **servirà identico in Fase 2** per IT+EN.

### `FuzzyDictionaryEngine` — tolleranza ai refusi

**Decora** un qualsiasi `DictionaryEngine` proponendo, dopo i match esatti, parole a
**distanza di modifica 1** dalla sequenza digitata: cifra di troppo, cifra mancante, cifra
sbagliata.

**Come, senza costo:** non scandisce il dizionario — genera le **varianti della sequenza**
(qualche decina di stringhe) e le cerca nell'indice esistente. Una manciata di lookup O(1)
per pressione.

**Perché non disturba la digitazione normale:**
- i candidati fuzzy sono marcati (`Candidate.fuzzy`), pesati / 1000, messi **dopo** tutti gli
  esatti, con un tetto di 6;
- sequenze sotto le 3 cifre non vengono corrette (a 2 cifre tutto è a distanza 1 da tutto);
- **soprattutto:** `currentPreview()` considera solo i candidati **esatti**. Un fuzzy è
  un'offerta da toccare, mai qualcosa da confermare di nascosto — altrimenti scrivere una
  parola sconosciuta al dizionario si trasformerebbe in una parola simile, cioè esattamente
  ciò che la colonna serve a impedire;
- nella barra sono resi in grigio.

Sta **fuori** dal merge, quindi tollera i refusi anche sulle parole imparate e (Fase 2) su
entrambe le lingue.

### `T9Keypad.sequenceFor(word)`

Parola → sequenza cifre, con **fold degli accenti** italiani (perché/perche stessa sequenza).
Fonte di verità condivisa da corpus, apprendimento e test.

---

## 6. Apprendimento persistente (dizionario personale)

**La tastiera impara.** Ogni parola effettivamente confermata — con spazio, invio,
punteggiatura, o scegliendo un suggerimento — finisce nel dizionario personale, consultato
**prima** del corpus. Una parola forzata lettera per lettera con la colonna va quindi digitata
"a mano" **una sola volta**: dalla seconda in poi è la prima predizione della sua sequenza.

- **`LearnedWordsEngine`** — il dizionario personale come `DictionaryEngine`. Indice in RAM
  `sequenza → (parola → n. usi)`: durante la digitazione **non si tocca mai il database**.
  Peso = `BASE_WEIGHT (1.000.000) + usi × 1.000`, sopra la frequenza massima del corpus
  (~75k di "di"), così una parola imparata batte sempre le parole di dizionario con la stessa
  sequenza, e le più usate salgono fra loro. Kotlin puro grazie al seam
  `LearnedWordsEngine.Store`.
- **Room** (`learning/`): entità `LearnedWord` (parola PK, sequenza, usi, ultimo uso),
  `LearnedWordDao`, `LearnedWordsDatabase`, `RoomLearnedWordsStore` che **scrive in coda su un
  thread singolo** — la pressione di un tasto non deve mai attendere il disco; l'ordine è
  garantito dall'executor a thread singolo.
- Le parole sono memorizzate **minuscole**: "Casa" e "casa" sono la stessa parola per lookup
  e apprendimento.

---

## 7. Maiuscole, accenti, emoji

### Maiuscole — `ShiftState`

`OFF → ONCE → LOCK → OFF`, ciclato da `⇧`. In T9 la maiuscola vale per la **parola**, non per
il singolo tasto (le lettere le decide il dizionario): `ONCE` capitalizza la parola in corso e
si consuma alla conferma (`afterCommit()`), `LOCK` scrive tutto maiuscolo finché non lo
spegni. Il glifo diventa `⇪` e passa dal teal al bianco quando è attivo.

**Punto chiave:** la maiuscola si applica **all'ultimo momento**, in `currentPreview()`;
composizione e dizionario restano minuscoli, così apprendimento e lookup non sono influenzati
dallo shift.

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
visibile e non aggiunge UI né conflitti col long-press dei preferiti.

### Emoji — `EmojiLayout`

Un pannello di 32 emoji comuni, otto per riga (più spazio dei simboli, per restare
riconoscibili). È **un altro `KeyGrid`**: nessuna vista nuova, solo una voce in `KeyboardMode`.

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

## 10. Impostazioni e persistenza

Due meccanismi, scelti in base a ciò che devono reggere:

| Cosa | Dove | Perché |
|------|------|--------|
| Parole imparate | **Room** (`learned_words.db`) | Migliaia di righe, query per sequenza, conteggi da aggiornare |
| Simboli preferiti | **SharedPreferences** (`KeyboardSettings`) | Sette valori letti una volta e scritti a un tocco: un database sarebbe solo costo |

Entrambi restano nella sandbox dell'app.

---

## 11. Copertura dei test

Unit test JVM (nessun emulatore necessario): `./gradlew :app:testDebugUnitTest`.

| Test | Copre |
|------|-------|
| `T9KeypadTest` | `sequenceFor`, fold accenti |
| `ItalianDictionaryEngineTest` | Costruzione indice, ordinamento per peso |
| `MergingDictionaryEngineTest` | Fusione, deduplica per parola |
| `LearnedWordsEngineTest` | Pesi, incremento usi, caricamento dallo store |
| `FuzzyDictionaryEngineTest` | 9 casi: cancellazione/sostituzione/inserimento, marcatura, tetto |
| `ComposeStateTest` | Forcing, avanzamento, pop-coppia, correzione, `defaultLetters`, accenti |
| `ShiftStateTest` | Ciclo, `apply`, `afterCommit`, `appliesToNext` |
| `FavouriteSymbolsTest` | Normalizzazione, sostituzione, scambio, "ogni default è raggiungibile" |
| `SymbolLayoutTest` | Fra cui "un tasto inserisce esattamente ciò che mostra" |

Le verifiche su emulatore sono documentate step per step in `DEVELOPMENT.md`, con gli
screenshot in `docs/screenshots/`.

---

## Cosa non c'è ancora

Riferimento completo e ordinato: `DEVELOPMENT.md` (Fasi 2 e 3). In sintesi:

- **Popup long-press sul tasto** per accentate e caratteri speciali, stile Gboard — **prossimo
  step (1.12)**, prima della Fase 2. Include due popup di simboli: `.` mostra i preferiti
  (raggiungibili così anche a metà parola, cosa che la colonna non permette) e `1` i simboli
  più usati, con conteggio di frequenza.
- **Bilingue IT+EN** (Fase 2): manca solo `EnglishDictionaryEngine` + corpus, il merge esiste.
- **QWERTY come layout alternativo**: `KeyGrid` e vista sono già pronti, manca la griglia.
- Impostazioni: posizione colonna, altezza tastiera, dimensione candidati, numero di preferiti.
- Maiuscola automatica a inizio frase (`getCursorCapsMode`).
- Schermata di gestione del dizionario personale e rimozione di una parola imparata
  (il DAO ha già `delete(word)`).
- Tasto `1` → pannello punteggiatura; microfono (`KeyAction.Mic`, oggi no-op e fuori layout).
