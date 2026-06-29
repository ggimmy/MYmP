# MyMP — Personal Music Player for Android

MyMP è un'app Android per ascoltare in streaming la propria libreria musicale da server locali o remoti. Supporta server domestici (NAS, PC) e server pubblici che distribuiscono musica priva di copyright o a licenza libera — contenuti spesso non disponibili sulle piattaforme di streaming commerciali. L'utente configura fino a 3 server tramite indirizzo IP/URL, sincronizza il catalogo dei brani e li riproduce in streaming direttamente dall'app.

---

## Requisiti

### Ambiente di sviluppo
- **Android Studio** Meerkat 2024.3.1 o superiore
- **JDK** 17+
- **Gradle** 9.x (gestito automaticamente da Android Studio)

### Versioni Android
- **minSdk**: 26 (Android 8.0 Oreo)
- **targetSdk**: 35 (Android 15)
- **compileSdk**: 35

### Dipendenze principali
| Libreria | Versione |
|---|---|
| Kotlin | 2.2.10 |
| AGP | 9.1.1 |
| KSP | 2.1.20-2.0.1 |
| Jetpack Compose BOM | 2025.x |
| Room | 2.7.1 |
| Retrofit | 2.x |
| WorkManager | 2.x |
| Navigation Compose | 2.8.4 |
| kotlinx.serialization | 1.x |

### API Key
Nessuna API key necessaria. L'app si connette a server HTTP configurati manualmente dall'utente.

### Formato server
Il server deve esporre un endpoint `GET /manifest.json` che ritorna un array JSON con la seguente struttura:
```json
[
  {
    "id": 1,
    "title": "Nome brano",
    "artist": "Artista",
    "album": "Album",
    "filePath": "http://indirizzo-server/percorso/file.mp3"
  }
]
```

---

## Compilazione ed esecuzione

### Build da Android Studio
1. Clona o scarica il repository
2. Apri Android Studio → `File → Open` → seleziona la cartella del progetto
3. Attendi la sincronizzazione Gradle (prima apertura richiede download dipendenze)
4. Verifica che non ci siano errori nel pannello `Build`

### Run su emulatore
1. Apri `Device Manager` in Android Studio
2. Crea un Virtual Device con API 26 o superiore
3. Premi il tasto **Run ▶** e seleziona l'emulatore
4. Nota: lo streaming da server locali richiede che emulatore e server siano sulla stessa rete virtuale — per test completi è consigliato un dispositivo fisico

### Run su dispositivo fisico
1. Abilita **Opzioni sviluppatore** sul dispositivo (`Impostazioni → Info telefono → tocca 7 volte "Numero build"`)
2. Abilita **Debug USB**
3. Collega il dispositivo via USB e autorizza il debug quando richiesto
4. Premi il tasto **Run ▶** in Android Studio e seleziona il dispositivo

### Permessi richiesti
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
Il permesso `POST_NOTIFICATIONS` viene richiesto a runtime su Android 13+.

---

## Funzionalità implementate

### Gestione server
- Configurazione di **3 slot server** tramite schermata Impostazioni (nome + indirizzo IP/URL)
- Supporto server locali (rete domestica) e server remoti pubblici (Internet)
- Selezione del server attivo tramite dropdown nella schermata principale
- Normalizzazione automatica dell'URL (aggiunta `http://` se assente)

### Sincronizzazione catalogo
- Recupero catalogo brani via chiamata REST (`GET /manifest.json`) con Retrofit
- Sincronizzazione on-demand al click del server
- **Upsert intelligente**: i brani esistenti vengono aggiornati preservando il loro ID locale, i brani rimossi dal server vengono cancellati — le playlist non vengono mai distrutte da una re-sync
- Database locale Room con separazione dei brani per server (`serverId`)
- Gestione via WorkManager con retry automatico (max 3 tentativi)

### Riproduzione
- Streaming audio in rete tramite `MediaPlayer` (locale o remoto)
- Riproduzione in background tramite **Foreground Service** (`MusicService`)
- Notifica di sistema con controlli play/pausa, skip, stop
- **Mini player bar** persistente in fondo alla schermata principale
- **Barra di progresso** con seek interattivo (trascina per spostarti nel brano)
- Aggiornamento in tempo reale della mini player bar su skip, fine brano, pausa e stop

### Playlist
- Creazione di playlist personali **trasversali ai server** (i brani possono provenire da server diversi)
- Aggiunta brani tramite **long press** sulla canzone → dialog con lista playlist esistenti + opzione "Crea nuova"
- Visualizzazione playlist tramite dropdown nella schermata principale
- Eliminazione playlist con rimozione automatica a cascata dei riferimenti ai brani
- Se un brano viene rimosso dal server in una re-sync, sparisce automaticamente anche dalle playlist che lo contenevano (CASCADE)

### Ricerca e ordinamento
- **Ricerca in tempo reale** per titolo o artista tramite barra di ricerca
- **4 modalità di ordinamento**: Titolo A→Z, Titolo Z→A, Artista A→Z, Artista Z→A
- Feedback visivo dell'ordinamento selezionato tramite Toast
- Ordinamento e ricerca applicati sia alla lista brani del server che alla vista playlist

### UI
- **Dark theme** forzato, design Material3
- Dropdown per selezione server e playlist
- Check mark sull'elemento attivo in tutti i dropdown
- Brano in riproduzione evidenziato nella lista con colore accent

---

## Funzionalità extra

- **Singleton database** via `Application` class (`MyMPApplication`): garantisce un'unica istanza Room condivisa tra Activity e Worker, eliminando race condition su scritture concorrenti
- **Comunicazione MusicService → ViewModel** tramite `StateFlow` condivisi in `MyMPApplication`: la mini player bar riflette sempre lo stato reale del servizio, non uno stato ottimistico del ViewModel
- **Upsert intelligente con indice UNIQUE** su `(serverId, remoteId)` in `SongEntity`: permette di aggiornare i metadati dei brani senza rompere i riferimenti nelle playlist
- **Job cancellabile per il collector dei brani** (`songsCollectionJob`): evita l'accumulo di collector multipli su cambi rapidi di server, eliminando race condition sulla lista visualizzata
