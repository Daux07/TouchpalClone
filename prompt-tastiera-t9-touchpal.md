# Prompt di pianificazione — Tastiera T9 stile TouchPal (Android, Kotlin)

## Obiettivo
Progettare e pianificare (non ancora implementare) un IME (Input Method Editor) Android che replichi, per uso strettamente personale, il comportamento della tastiera T9 di TouchPal così come ricordata e verificata in questa sessione — in particolare il suo meccanismo di disambiguazione manuale a colonna, mai replicato da alternative moderne (es. TT9, che usa solo multi-tap classico senza questa scorciatoia).

## Contesto e vincoli
- Uso personale, non destinato a pubblicazione o distribuzione.
- Target: dispositivo Android moderno (Samsung Galaxy S25, Android 15/16) — quindi `targetSdkVersion` aggiornato, non le build datate di TouchPal (che su questo hardware/OS risultano non installabili o non funzionanti).
- Linguaggio: Kotlin, `InputMethodService` nativo Android (nessun motore di terze parti tipo Presage necessario per la v1).
- Dizionario: italiano, con frequenze d'uso, gestito localmente (nessuna dipendenza cloud, per privacy — TouchPal è stato storicamente associato ad adware e raccolta dati eccessiva, requisito esplicito è evitare questo).

## 1. Layout tastiera
- Layout numerico 12 tasti, standard **ITU-T E.161** (2=ABC, 3=DEF, 4=GHI, 5=JKL, 6=MNO, 7=PQRS, 8=TUV, 9=WXYZ, 1=simboli/punteggiatura, 0=spazio, *=cambio modalità, #=maiuscole/minuscole).
- Tasti funzione: backspace, invio, cambio modalità T9/123 (numerico)/simboli, shift (maiuscolo prima lettera / tutto maiuscolo), tasto emoji dedicato (apre pannello emoji separato dal layout principale).
- **Tasto 1 con doppia funzione**: tap breve = valore normale del tasto nel contesto corrente; **pressione prolungata (long-press) = apertura pannello simboli** (punteggiatura estesa, caratteri speciali), senza dover passare dal tasto di cambio modalità.
- **Altezza tastiera regolabile**: l'utente deve poter aumentare/diminuire l'altezza complessiva della tastiera (es. slider nelle impostazioni o gesto di trascinamento sul bordo superiore); il ridimensionamento deve **riproporzionare tutti i tasti in modo uniforme** (larghezza/altezza/font), non solo aggiungere spazio vuoto.
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

## 4. Doppio input con lingua secondaria (italiano + inglese)
- La tastiera deve supportare due dizionari attivi contemporaneamente: italiano (primario) e inglese (secondario), senza richiedere un cambio manuale di layout/lingua per ogni parola.
- Per ogni sequenza numerica digitata, il motore predittivo consulta **entrambi** i dizionari e fonde i candidati in un'unica lista di suggerimenti, ordinata per frequenza (cross-lingua) — così una parola inglese comune (es. "OK", "way") può comparire tra i suggerimenti anche se l'utente non ha cambiato lingua esplicitamente.
- La colonna di disambiguazione manuale resta unica: i gruppi di lettere per tasto (2=ABC, ecc.) sono identici in entrambe le lingue nel mapping ITU E.161, quindi non serve doppia colonna, solo il motore di ranking deve essere bilingue.
- L'apprendimento (parola forzata → peso alto) va registrato nel dizionario della lingua rilevata per quella parola, o in un dizionario "misto" unico se la distinzione risulta inutilmente complessa in fase di implementazione.

## 5. Motore linguistico e dizionario
- Struttura dati: mappa `sequenza numerica → lista di parole ordinate per frequenza` (trie o hash indicizzato per codice cifre).
- Dizionario di partenza: **Leipzig Corpora Collection – lista di frequenza italiana (CC BY-4.0)**, disponibile in tagli da 10K a 1M parole. Scelta preferita rispetto a ItWaC (licenza meno chiara per riuso) e a Paisà (CC BY-NC-SA, più restrittivo senza vantaggi concreti per questo progetto); licenza permissiva con solo obbligo di attribuzione, adatta sia a uso personale sia a un'eventuale condivisione futura del codice.
- Apprendimento persistente: ogni parola forzata via colonna viene salvata con peso alto sulla propria sequenza; l'uso ripetuto di una parola ne aumenta ulteriormente il ranking.
- Nessuna sincronizzazione cloud, nessuna raccolta dati di telemetria.
- **Salvataggio automatico su spazio**: quando l'utente preme spazio dopo aver composto una parola (predetta o forzata via colonna), la parola viene aggiunta/aggiornata in automatico nel dizionario locale, senza richiedere conferma esplicita. Rischio accettato consapevolmente (possibili errori di battitura salvati per sbaglio, rimediabili via cancellazione manuale).
- **Opzione "salvataggio sicuro" nelle impostazioni**: modalità alternativa disattivabile di default, che introduce una conferma o un ritardo prima di salvare permanentemente solo le parole **mai viste prima** nel dizionario (non quelle già presenti), per chi vuole ridurre il rischio di errori senza rinunciare al salvataggio automatico per il resto.
- **Gestione dizionario personale dalle impostazioni**: schermata dedicata che elenca le parole salvate (incluse quelle aggiunte per errore), con possibilità di cancellarle singolarmente.
- **Cancellazione rapida dalla riga candidati**: tenendo premuto (long-press) su una parola nella barra dei suggerimenti sopra la tastiera, deve comparire un'opzione per rimuoverla direttamente dal dizionario personale, senza dover passare dalle impostazioni.

## 6. Architettura Android
- `InputMethodService` con vista custom per il layout a 12 tasti + colonna laterale.
- Gestione stato: classe che mantiene lo stack di coppie (cifra, lettera), il buffer di testo forzato, e l'interfaccia con il motore di ranking.
- Persistenza dizionario: storage locale (SQLite o file binario indicizzato), non condiviso con altre app.
- Compatibilità: build con `targetSdkVersion` aggiornato per garantire funzionamento su Android 15/16 (causa principale di fallimento delle vecchie build TouchPal originali).

## 7. Fuori scope per la v1
- Temi, emoji store, sticker, GIF.
- Swipe/gesture typing (Curve/Wave).
- Multi-lingua (partire da solo italiano).
- Layout T+ QWERTY alternativo (valutare in fase successiva).

## 8. Criteri di accettazione
- Digitando una sequenza numerica nota, il motore propone la parola corretta come prima scelta.
- Se la parola non è nei suggerimenti, la colonna permette di forzarla lettera per lettera seguendo esattamente il meccanismo del punto 3.
- Backspace su una lettera forzata rimuove sempre cifra+lettera insieme, mai solo il carattere visibile.
- Una parola forzata una volta viene proposta per prima al successivo inserimento della stessa sequenza.
- A stack vuoto, la colonna mostra i simboli preferiti scelti dall'utente dalle impostazioni, invece di restare vuota o nascosta.
- La colonna è sempre visibile e la sua posizione (sinistra/destra) è configurabile dalle impostazioni.
- Le vocali accentate italiane sono inseribili tramite long-press sulla vocale base corrispondente.
- Digitando una sequenza ambigua tra italiano e inglese, i suggerimenti includono candidati validi in entrambe le lingue senza bisogno di cambio lingua manuale.
- L'altezza della tastiera è regolabile e tutti i tasti si riproporzionano coerentemente al variare dell'altezza.
- Il long-press sul tasto 1 apre il pannello simboli senza richiedere il cambio modalità esplicito.
- La modalità numerica dedicata è accessibile e priva di ambiguità con l'inserimento T9 alfabetico.
- Premendo spazio dopo una parola, questa viene salvata/aggiornata automaticamente nel dizionario locale.
- Dalle impostazioni è possibile visualizzare ed eliminare singole parole dal dizionario personale.
- Il long-press su una parola nella riga dei candidati offre l'opzione di eliminarla dal dizionario personale.
- L'app gira in modo stabile su Android 15/16 senza richiedere permessi non necessari (niente accesso SMS/contatti se non esplicitamente richiesto dall'utente).
