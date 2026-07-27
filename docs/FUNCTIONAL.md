# Documentazione funzionale — Tastiera T9 stile TouchPal

> Descrive **come funzionano le feature implementate** dal punto di vista
> comportamentale e architetturale. Cresce man mano che si sviluppa: ogni feature
> completata aggiunge/aggiorna la sua sezione. Non è la specifica (quella è
> `prompt-tastiera-t9-touchpal.md`) né il tracker (quello è `DEVELOPMENT.md`):
> qui si documenta ciò che **esiste e funziona** nel codice.

## Indice
- [Panoramica architettura](#panoramica-architettura)
- [Componenti implementati](#componenti-implementati)

---

## Panoramica architettura

La tastiera è un **IME Android** (`InputMethodService`). Struttura prevista dei package
(sotto `com.daux.t9keyboard`):

| Package | Responsabilità |
|---------|----------------|
| `service` | `T9ImeService`: ciclo di vita IME, creazione della view tastiera, invio testo al campo attivo |
| `ui` | View custom: griglia 12 tasti, colonna di disambiguazione, barra suggerimenti |
| `engine` | `DictionaryEngine` (interfaccia) e implementazioni; ranking dei candidati |
| `dict` | Persistenza: corpus base in RAM (asset binario) + dizionario personale (Room) |
| `settings` | Preferenze utente (posizione colonna, altezza, simboli preferiti…) |

Principio chiave: la UI e la logica di composizione parlano al dizionario **solo**
tramite l'interfaccia `DictionaryEngine`, per rendere indolore il passaggio al
bilingue (Fase 2). Vedi sezioni 5 e 8 del documento di piano.

---

## Componenti implementati

### IME base — `T9ImeService` (Fase 0)

**Cosa fa:** registra l'app come tastiera di sistema (Input Method Editor) tramite un
`InputMethodService`. Quando l'utente seleziona "T9 Keyboard" come metodo di input,
Android chiama `onCreateInputView()` e mostra la view restituita sopra la barra di
navigazione, al posto della tastiera di sistema.

**Comportamento attuale:** mostra `PlaceholderKeyboardView`, un pannello scuro alto
~220dp con una label. È un segnaposto per validare l'installazione e il rendering; non
gestisce ancora input. Verrà sostituito in Fase 1 dalla griglia 12 tasti + colonna.

**File:**
- `service/T9ImeService.kt` — ciclo di vita IME.
- `ui/PlaceholderKeyboardView.kt` — superficie temporanea.
- `AndroidManifest.xml` — dichiara il service con permesso di sistema
  `BIND_INPUT_METHOD` e l'intent-filter `android.view.InputMethod`.
- `res/xml/method.xml` — metadati IME, con un subtype italiano (`it_IT`).

**Privacy:** nessun permesso applicativo sensibile è dichiarato (niente
SMS/contatti/rete). L'unico permesso è `BIND_INPUT_METHOD`, che è concesso solo al
sistema e obbligatorio per qualsiasi IME.

**Come si abilita:** *Impostazioni Android → Gestione generale → Elenco tastiere e
predefinita → abilita "T9 Keyboard"*, poi la si seleziona dal selettore tastiera in un
qualsiasi campo di testo.

### Griglia 12 tasti — `T9KeyboardView` (Fase 1.1)

**Cosa fa:** disegna la tastiera vera e propria: una griglia 4×3 con i tasti numerici
1–9 (con sottotitolo delle lettere ITU-T E.161, es. "2/ABC") più una riga funzione
`⌫  0  ⏎`. Ogni tocco è riportato al service tramite una callback `onKey(KeyAction)`;
la view non contiene logica di input.

**Responsività (piano §6):** nessuna dimensione fissa. Le righe si dividono l'altezza
con `layout_weight`, i tasti si dividono la larghezza della riga con `weight`, e
l'altezza totale è il **42% dell'altezza schermo** (`onMeasure`). Così la stessa UI si
riproporziona tra S25 e S25 Ultra senza layout dedicati. Il ridimensionamento manuale
e la gestione degli inset arriveranno in Fase 3.

**File:** `ui/T9KeyboardView.kt`, `model/KeySpec`/`T9Layout` (layout), `model/T9Keypad`
(mapping lettere).

### Inserimento testo multi-tap — `T9ImeService` (Fase 1.1, temporaneo)

**Cosa fa:** inserimento a **multi-tap** classico come primo slice funzionante.
Toccando ripetutamente la stessa cifra si scorrono le sue lettere (`8`→t→u→v), usando
il *composing text* per l'anteprima; una pausa di 800 ms o il tocco di una cifra diversa
conferma la lettera. `0` inserisce uno spazio, `⌫` cancella (annullando l'anteprima se
in corso), `⏎` esegue l'azione dell'editor (search/done/invio).

> ⚠️ **Temporaneo:** questa modalità è un trampolino per validare la pipeline
> griglia→`InputConnection`. Verrà **sostituita** in Fase 1.2/1.3 dalla modalità
> predittiva T9 (digiti la sequenza di cifre, il motore propone le parole) + la colonna
> di disambiguazione manuale, che sono la funzione centrale del progetto.

**Mapping lettere (`T9Keypad.letters`):** 1=`. , ? ! '`, 2=abc, 3=def, 4=ghi, 5=jkl,
6=mno, 7=pqrs, 8=tuv, 9=wxyz, 0=spazio. È la fonte di verità dei gruppi di lettere,
riusata anche dal motore predittivo e dalla colonna.
