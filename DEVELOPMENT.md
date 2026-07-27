# DEVELOPMENT — Tastiera T9 stile TouchPal

> **File di tracciamento dello sviluppo.** Serve a riprendere il lavoro in sessioni
> successive. Aggiornato ad ogni step: task completati marcati `[x]`, stato corrente
> e prossimo passo aggiornati in cima. La fonte di verità sul *cosa* costruire resta
> `prompt-tastiera-t9-touchpal.md`; questo file traccia il *come/quando*.

---

## 🔖 STATO CORRENTE (aggiornare sempre qui)

- **Fase in corso:** Fase 0 — Scaffolding progetto (codice completo; manca solo la verifica su dispositivo/emulatore)
- **Ultimo step completato:** scaffolding Gradle + modulo `app` + `T9ImeService` con view placeholder
- **Prossimo step:** aprire il progetto in Android Studio, generare il Gradle wrapper, buildare, installare ed **eseguire la Milestone Fase 0** (abilitare e selezionare la tastiera). Poi iniziare la **Fase 1** dal layout 12 tasti.
- **Come riprendere:** leggi questa sezione + i task non spuntati della fase corrente qui sotto.

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
- [ ] **Milestone (richiede Android Studio):** buildare, installare, abilitare la tastiera nelle impostazioni di sistema, selezionarla e vederla comparire

## Fase 1 — MVP: funzione centrale (la colonna)

- [ ] Layout 12 tasti ITU-T E.161 (griglia custom) + tasti funzione
- [ ] `DictionaryEngine` (interfaccia) + `ItalianDictionaryEngine` da asset di test
- [ ] Modalità predittiva T9 + barra suggerimenti orizzontale
- [ ] **Colonna di disambiguazione manuale posizionale** (stack di coppie cifra/lettera)
- [ ] Backspace = pop della coppia (cifra+lettera insieme)
- [ ] Estendere/correggere parola (push nuova coppia)
- [ ] Apprendimento persistente (Room) + salvataggio automatico su spazio
- [ ] Integrazione corpus Leipzig italiano → file binario indicizzato in `assets/`
- [ ] Test unitari su `ComposeState` e `DictionaryEngine`

## Fase 2 — Bilingue IT+EN

- [ ] `EnglishDictionaryEngine` + `BilingualDictionaryEngine` (merge candidati)
- [ ] Corpus inglese affiancato al corpus italiano
- [ ] Criteri di accettazione Fase 2 (nessuna regressione sulla v1)

## Fase 3 — Impostazioni, ergonomia, rifiniture

- [ ] Posizione colonna sinistra/destra configurabile
- [ ] Simboli preferiti a stack vuoto (configurabili)
- [ ] Altezza tastiera regolabile con riproporzionamento uniforme
- [ ] Long-press tasto 1 → pannello simboli
- [ ] Modalità numerica dedicata (123)
- [ ] Vocali accentate via long-press
- [ ] Schermata gestione dizionario personale (lista + cancella)
- [ ] Long-press su candidato → rimuovi dal dizionario
- [ ] Opzione "salvataggio sicuro"
- [ ] Pannello emoji base
- [ ] Verifica layout responsive su S25 e S25 Ultra

---

## 📓 Log di sviluppo (append in fondo, più recente in alto)

<!-- Formato: ### AAAA-MM-GG — titolo step -->
<!-- Cosa fatto, file toccati, note/decisioni, come verificare. -->

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
