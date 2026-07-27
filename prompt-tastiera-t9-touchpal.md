# Prompt di pianificazione — Tastiera T9 stile TouchPal (Android, Kotlin)

## Obiettivo
Progettare e pianificare (non ancora implementare) un IME (Input Method Editor) Android che replichi, per uso strettamente personale, il comportamento della tastiera T9 di TouchPal così come ricordata e verificata in questa sessione — in particolare il suo meccanismo di disambiguazione manuale a colonna, mai replicato da alternative moderne (es. TT9, che usa solo multi-tap classico senza questa scorciatoia).

## Contesto e vincoli
- Uso personale, non destinato a pubblicazione o distribuzione.
- Target v1: due dispositivi fisici, **Galaxy S25** e **Galaxy S25 Ultra** (Android 15/16) — quindi `targetSdkVersion` aggiornato, non le build datate di TouchPal (che su questo hardware/OS risultano non installabili o non funzionanti). I due modelli differiscono per dimensione fisica, risoluzione e densità: il layout deve essere validato su entrambi (dettagli in sezione 6).
- Linguaggio: Kotlin, `InputMethodService` nativo Android (nessun motore di terze parti tipo Presage necessario per la v1).
- Dizionario: italiano, con frequenze d'uso, gestito localmente (nessuna dipendenza cloud, per privacy — TouchPal è stato storicamente associato ad adware e raccolta dati eccessiva, requisito esplicito è evitare questo).

## Roadmap e fasi
- **Fase 1 / v1 (oggetto principale di questo documento)**: layout T9 a 12 tasti, colonna di disambiguazione posizionale (funzione centrale del progetto), motore predittivo **monolingua italiano**, persistenza ibrida (corpus base in RAM + dizionario personale su Room), layout responsivo validato su Galaxy S25 e S25 Ultra.
- **Fase 2**: dizionario bilingue italiano + inglese con fusione dei candidati nella stessa lista suggerimenti (sezione 8) — esplicitamente rimandata, non parte della v1.
- **Fase 3+ (non pianificata in dettaglio, solo annotata)**: layout T+ QWERTY alternativo, temi, altre lingue oltre IT/EN.

Questa sezione è la fonte di verità sulla priorità: qualunque voce delle sezioni seguenti che parli di bilingue/multi-lingua appartiene alla Fase 2, non alla v1.

## 1. Layout tastiera
- Layout numerico 12 tasti, standard **ITU-T E.161** (2=ABC, 3=DEF, 4=GHI, 5=JKL, 6=MNO, 7=PQRS, 8=TUV, 9=WXYZ, 1=simboli/punteggiatura, 0=spazio, *=cambio modalità, #=maiuscole/minuscole).
- Tasti funzione: backspace, invio, cambio modalità T9/123 (numerico)/simboli, shift (maiuscolo prima lettera / tutto maiuscolo), tasto emoji dedicato (apre pannello emoji separato dal layout principale).
- **Tasto 1 con doppia funzione**: tap breve = valore normale del tasto nel contesto corrente; **pressione prolungata (long-press) = apertura pannello simboli** (punteggiatura estesa, caratteri speciali), senza dover passare dal tasto di cambio modalità.
- **Altezza tastiera regolabile**: l'utente deve poter aumentare/diminuire l'altezza complessiva della tastiera (es. slider nelle impostazioni o gesto di trascinamento sul bordo superiore); il ridimensionamento deve **riproporzionare tutti i tasti in modo uniforme** (larghezza/altezza/font), non solo aggiungere spazio vuoto. Il valore di default (prima di un eventuale override manuale) è calcolato come percentuale dello schermo, non come dp assoluto — vedi sezione 6 per il razionale multi-device.
- **Modalità numerica dedicata**: cambio modalità tramite tap standard sul tasto "123" (visibile nella barra della tastiera, come nello screenshot di riferimento) — passa a una griglia puramente numerica (0-9 + operatori base), distinta dal T9 alfabetico. Non è stato possibile confermare il comportamento esatto originale di TouchPal su questo punto (dettaglio minore, non replicato fedelmente): si adotta il pattern standard delle tastiere T9 (tap sul tasto di cambio modalità) invece di un long-press o gesto non verificato.
- (Fuori scope v1, valutare in futuro) layout alternativo T+ tipo QWERTY diviso, come variante secondaria opzionale.
- **Lettere accentate (à, è, ì, ò, ù)**: non previste dallo standard ITU-T E.161 di base. Si inseriscono con **long-press sulla vocale già selezionata** (es. dopo aver scelto "E" dalla colonna o come lettera predetta, un long-press sulla stessa la trasforma in "È"). Il motore predittivo tratta la sequenza numerica della parola accentata come equivalente alla forma non accentata ai fini del lookup; l'accento è uno step successivo esplicito dell'utente.

## 2. Modalità predittiva standard (T9 classico)
- L'utente digita la sequenza numerica corrispondente alla parola (es. 2-2-7-2).
- Il motore propone come prima scelta la parola più probabile per quella sequenza, in base al dizionario pesato per frequenza (es. "BARA" prima di "CASA" se più frequente).
- Barra suggerimenti orizzontale con 3+ candidati alternativi, selezionabili con tap, come nello screenshot di riferimento allegato in sessione.

## 3. Colonna di disambiguazione manuale (funzione centrale del progetto)
Meccanismo esatto da replicare, verificato passo-passo in questa sessione:

1. **Stato interno**: uno **stack di coppie (cifra, lettera)**, non due liste separate. Ogni coppia rappresenta "a questa posizione ho premuto questo tasto e scelto questa lettera".
2. **Attivazione colonna**: la colonna è **sempre presente e visibile** in parallelo alla barra suggerimenti, non va attivata con un gesto. Posizione configurabile dalle impostazioni: **sinistra (default) o destra** della griglia numerica.
3. **Colonna posizionale**: la colonna mostra le lettere del tasto già premuto in quella posizione della sequenza (non un filtro dinamico sui prefissi del dizionario — è un elenco fisso di 3-4 lettere per tasto).
4. **Selezione e avanzamento**: al tap sulla lettera corretta, la coppia (cifra, lettera) va in cima allo stack, il carattere si aggiunge al testo composto, e la colonna si aggiorna mostrando le lettere del **tasto successivo già digitato nella sequenza originale**.
5. **Fine parola forzata**: completata la sequenza, la parola viene inserita nel testo **e aggiunta permanentemente al dizionario locale** con peso iniziale alto, associata alla sequenza numerica usata — da quel momento il motore predittivo la propone per prima su quella sequenza.
6. **Backspace = pop dell'ultima coppia** (non solo dell'ultimo carattere): rimuove contemporaneamente cifra e lettera, per evitare stati inconsistenti (cifra "orfana" senza lettera associata).
7. **Estendere la parola** (parola più lunga della sequenza forzata finora): si preme una nuova cifra in coda, si apre la colonna per quel tasto, si sceglie la lettera — nuova coppia in push, radice invariata.
8. **Correggere l'ultima lettera**: backspace (pop) seguito da una nuova pressione di tasto (anche diverso da quello originale) e nuova scelta dalla colonna — stessa identica operazione del punto 7, nessuna distinzione di codice tra "correggere" e "continuare".
9. **Limite noto e accettato**: non è previsto editing in-place a metà parola; una correzione a metà sequenza richiede di cancellare (pop ripetuti) tutto ciò che segue e ridigitare.
10. **Stato a riposo (nessuna sequenza in corso)**: quando lo stack è vuoto (nessun tasto numerico ancora premuto per la parola corrente), la colonna non è vuota — mostra invece un elenco di **simboli preferiti scelti manualmente dall'utente** (es. punteggiatura frequente, apostrofo, emoji base), selezionabili con un tap diretto senza dover passare dal tasto simboli/123. L'elenco e il suo ordine sono configurabili dalle impostazioni: è l'utente a decidere quali simboli inserire e in che posizione, non un ranking automatico calcolato dall'uso.

## 4. Motore linguistico e dizionario (v1 — solo italiano)
- Dizionario di partenza: **Leipzig Corpora Collection – lista di frequenza italiana (CC BY-4.0)**, disponibile in tagli da 10K a 1M parole. Scelta preferita rispetto a ItWaC (licenza meno chiara per riuso) e a Paisà (CC BY-NC-SA, più restrittivo senza vantaggi concreti per questo progetto); licenza permissiva con solo obbligo di attribuzione, adatta sia a uso personale sia a un'eventuale condivisione futura del codice.
- Apprendimento persistente: ogni parola forzata via colonna viene salvata con peso alto sulla propria sequenza; l'uso ripetuto di una parola ne aumenta ulteriormente il ranking. Il "dove" e "come" viene salvato è descritto in dettaglio in sezione 5.
- Nessuna sincronizzazione cloud, nessuna raccolta dati di telemetria.
- **Salvataggio automatico su spazio**: quando l'utente preme spazio dopo aver composto una parola (predetta o forzata via colonna), la parola viene aggiunta/aggiornata in automatico nel dizionario locale, senza richiedere conferma esplicita. Rischio accettato consapevolmente (possibili errori di battitura salvati per sbaglio, rimediabili via cancellazione manuale).
- **Opzione "salvataggio sicuro" nelle impostazioni**: modalità alternativa disattivabile di default, che introduce una conferma o un ritardo prima di salvare permanentemente solo le parole **mai viste prima** nel dizionario (non quelle già presenti), per chi vuole ridurre il rischio di errori senza rinunciare al salvataggio automatico per il resto.
- **Gestione dizionario personale dalle impostazioni**: schermata dedicata che elenca le parole salvate (incluse quelle aggiunte per errore), con possibilità di cancellarle singolarmente.
- **Cancellazione rapida dalla riga candidati**: tenendo premuto (long-press) su una parola nella barra dei suggerimenti sopra la tastiera, deve comparire un'opzione per rimuoverla direttamente dal dizionario personale, senza dover passare dalle impostazioni.

## 5. Persistenza ibrida dei dati
Due storage con ruoli distinti e non intercambiabili — questo è il punto che il piano precedente lasciava vago ("SQLite o file binario" come alternativa, invece che come combinazione):

- **Corpus base (sola lettura, read-heavy)**: dizionario italiano precompilato (Leipzig, taglio 300K–1M parole), convertito in fase di build in un **file binario indicizzato per sequenza numerica T9** (mappa sequenza → lista ordinata di `(parola, peso)`, serializzato in formato compatto — non un DB relazionale). Il file è incluso negli asset dell'app e **caricato interamente in RAM all'avvio del servizio IME** (una volta sola, non ad ogni lookup), in una struttura tipo `Map<String, List<Pair<String, Int>>>` o trie, per garantire lookup senza I/O durante la digitazione. Stima: con ~300K parole e pesi su 16 bit, il file resta nell'ordine di pochi MB — footprint accettabile anche per un servizio IME che resta in background a lungo.
- **Dizionario personale (scrittura frequente)**: parole forzate via colonna e parole apprese da salvataggio automatico su spazio, gestite su **Room/SQLite**, con schema minimo:
  - `learned_words(sequence TEXT, word TEXT, weight INTEGER, last_used INTEGER, PRIMARY KEY(sequence, word))`
  - Le scritture incrementali (una per parola forzata/confermata) sono economiche e sicure su Room, a differenza del file binario del corpus base, che non è pensato per essere riscritto a runtime.
- **Merge a lookup-time**: per ogni sequenza digitata, il motore interroga entrambe le fonti e fonde i risultati. Le entry del dizionario personale (Room) hanno **priorità sul corpus base**: una parola forzata/appresa deve comparire come prima scelta, scavalcando il ranking di frequenza statico del corpus. I candidati restanti vengono completati dal file binario in RAM, ordinati per peso.
- **Cache in-memory del dizionario personale**: per evitare una query Room a ogni tasto premuto, si mantiene una cache in RAM (aggiornata sulle stesse scritture, rare rispetto alle letture) sincronizzata con Room; le cancellazioni dalla schermata impostazioni o dal long-press sui candidati invalidano/aggiornano sia Room sia la cache.
- Nessuna delle due fonti è condivisa con altre app; nessuna sincronizzazione cloud.

## 6. Layout responsivo multi-dispositivo (Galaxy S25 / Galaxy S25 Ultra)
- Dispositivi target v1: **Galaxy S25** (~6.2", 1080×2340, ~416 dpi) e **Galaxy S25 Ultra** (~6.9", 1440×3120, ~505 dpi) — differiscono per dimensione fisica, risoluzione, densità e per l'altezza degli inset di sistema (gesture bar/punch-hole), non solo per dimensione dello schermo.
- Il layout **non deve usare dimensioni fisse in dp** per tasti e colonna: larghezza/altezza dei tasti vanno calcolate a runtime come frazione dello spazio disponibile (`ConstraintLayout` con vincoli percentuali, o `weight` in griglia/`LinearLayout`), così la stessa UI si riproporziona automaticamente tra i due dispositivi senza layout o asset dedicati per device.
- L'altezza di default della tastiera (prima di un eventuale override manuale dall'utente, vedi sezione 1) deriva da una percentuale dell'altezza schermo disponibile al netto degli inset di sistema, non da un valore assoluto — così il rapporto tastiera/contenuto resta coerente tra i due modelli. Il gesto di ridimensionamento manuale opera sulla stessa percentuale: la logica di riproporzionamento uniforme è unica e indipendente dal device.
- Va gestito esplicitamente `WindowInsets` per evitare che la colonna di disambiguazione o la barra suggerimenti finiscano sotto la gesture navigation bar, la cui altezza può differire tra i due modelli.
- **Orientamento**: la v1 assume uso in verticale (portrait), che copre la quasi totalità dell'uso reale di una tastiera IME; il landscape non viene validato esplicitamente (limite noto e accettato, coerente con lo stile già adottato per altri limiti in questo documento).
- **Verifica richiesta prima di considerare la v1 completa**: test manuale su entrambi i dispositivi fisici (o emulatori con le stesse risoluzioni) — nessun taglio di tasti, colonna sempre interamente visibile, font leggibile in entrambi i casi.

## 7. Architettura Android
- `InputMethodService` con vista custom per il layout a 12 tasti + colonna laterale (layout e responsività: sezione 6).
- Gestione stato: classe che mantiene lo stack di coppie (cifra, lettera), il buffer di testo forzato, e l'interfaccia con il motore di ranking.
- Persistenza: architettura ibrida corpus-in-RAM + Room, dettagliata in sezione 5 (non una scelta secca tra SQLite e file binario, ma i due insieme con ruoli distinti).
- Compatibilità: build con `targetSdkVersion` aggiornato per garantire funzionamento su Android 15/16 (causa principale di fallimento delle vecchie build TouchPal originali).

## 8. Fase 2 — Dizionario bilingue italiano + inglese
Esplicitamente rimandato rispetto alla v1 (che è monolingua italiano, sezione 4). Da valutare solo dopo che la v1 è stabile e i criteri di accettazione v1 (sezione 10) sono soddisfatti:

- La tastiera dovrà supportare due dizionari attivi contemporaneamente: italiano (primario) e inglese (secondario), senza richiedere un cambio manuale di layout/lingua per ogni parola.
- Per ogni sequenza numerica digitata, il motore predittivo dovrà consultare **entrambi** i dizionari e fondere i candidati in un'unica lista di suggerimenti, ordinata per frequenza cross-lingua — così una parola inglese comune (es. "OK", "way") può comparire tra i suggerimenti anche se l'utente non ha cambiato lingua esplicitamente.
- La colonna di disambiguazione manuale resta unica anche in fase 2: i gruppi di lettere per tasto (2=ABC, ecc.) sono identici in entrambe le lingue nel mapping ITU E.161, quindi non serve doppia colonna, solo il motore di ranking deve diventare bilingue.
- L'apprendimento (parola forzata → peso alto) andrà registrato nel dizionario della lingua rilevata per quella parola, o in un dizionario "misto" unico se la distinzione risulta inutilmente complessa in fase di implementazione.
- Impatto atteso sull'architettura di sezione 5: il file binario del corpus base dovrà includere (o affiancare) un secondo corpus inglese; lo schema Room `learned_words` può restare invariato se si opta per il dizionario misto, altrimenti richiede una colonna `lang`.

## 9. Fuori scope per la v1
- Temi, emoji store, sticker, GIF.
- Swipe/gesture typing (Curve/Wave).
- Layout T+ QWERTY alternativo (valutare in fase successiva).
- Bilingue IT+EN: non fuori scope in senso assoluto, ma esplicitamente rimandato alla Fase 2 (sezione 8) — vedi Roadmap.

## 10. Criteri di accettazione — v1
- Digitando una sequenza numerica nota, il motore propone la parola corretta come prima scelta.
- Se la parola non è nei suggerimenti, la colonna permette di forzarla lettera per lettera seguendo esattamente il meccanismo della sezione 3.
- Backspace su una lettera forzata rimuove sempre cifra+lettera insieme, mai solo il carattere visibile.
- Una parola forzata una volta viene proposta per prima al successivo inserimento della stessa sequenza, grazie alla priorità del dizionario personale (Room) sul corpus base nel merge a lookup-time (sezione 5).
- A stack vuoto, la colonna mostra i simboli preferiti scelti dall'utente dalle impostazioni, invece di restare vuota o nascosta.
- La colonna è sempre visibile e la sua posizione (sinistra/destra) è configurabile dalle impostazioni.
- Le vocali accentate italiane sono inseribili tramite long-press sulla vocale base corrispondente.
- L'altezza della tastiera è regolabile e tutti i tasti si riproporzionano coerentemente al variare dell'altezza, su entrambi i dispositivi target (sezione 6).
- Il long-press sul tasto 1 apre il pannello simboli senza richiedere il cambio modalità esplicito.
- La modalità numerica dedicata è accessibile e priva di ambiguità con l'inserimento T9 alfabetico.
- Premendo spazio dopo una parola, questa viene salvata/aggiornata automaticamente nel dizionario locale (Room).
- Dalle impostazioni è possibile visualizzare ed eliminare singole parole dal dizionario personale.
- Il long-press su una parola nella riga dei candidati offre l'opzione di eliminarla dal dizionario personale.
- Il layout non presenta tasti tagliati o sproporzionati né su Galaxy S25 né su Galaxy S25 Ultra, in verticale.
- L'app gira in modo stabile su Android 15/16 senza richiedere permessi non necessari (niente accesso SMS/contatti se non esplicitamente richiesto dall'utente).

## 11. Criteri di accettazione — Fase 2
- Digitando una sequenza ambigua tra italiano e inglese, i suggerimenti includono candidati validi in entrambe le lingue senza bisogno di cambio lingua manuale.
- L'apprendimento di una parola forzata in fase 2 non degrada il comportamento monolingua della v1 per le sequenze già note in italiano (nessuna regressione sui criteri di sezione 10).
