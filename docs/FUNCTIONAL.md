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
