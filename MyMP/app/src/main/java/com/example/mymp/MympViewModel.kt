package com.example.mymp

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel principale dell'app, responsabile della logica di presentazione.
 *
 * Espone [uiState] come stato osservabile dalla UI e gestisce:
 * - la selezione e connessione ai server
 * - la sincronizzazione dei brani tramite [SyncSongWorker]
 * - la riproduzione audio tramite [MusicService]
 * - la gestione delle playlist
 * - la ricerca e il sorting dei brani
 *
 * Riceve i [MutableStateFlow] condivisi da [MympApplication] per osservare
 * lo stato del [MusicService] (brano corrente, isPlaying, progresso)
 * senza accoppiamento diretto tra ViewModel e Service.
 *
 * @property repository Unica fonte di verità per i dati locali e remoti
 * @property workManager Scheduler per la sincronizzazione asincrona dei brani
 * @property currentSongState Flow condiviso con [MusicService]: brano corrente
 * @property isPlayingState Flow condiviso con [MusicService]: stato play/pausa
 * @property playbackProgressState Flow condiviso con [MusicService]: progresso (0f-1f)
 */
class MympViewModel (
    private val repository: MympRepository,
    private val workManager: WorkManager,
    private val currentSongState: MutableStateFlow<Song?>, //brano corrente
    private val isPlayingState: MutableStateFlow<Boolean>,
    private val playbackProgressState: MutableStateFlow<Float>
) : ViewModel() {

    var uiState by mutableStateOf(MympUiState())
        private set //la UI può solo leggere!

    companion object {
        const val SYNC_TAG = "sync_songs" //tag condiviso dai worker per osservare lo stato
    }

    /**
     * Stato della sincronizzazione, osserva il worker più recente
     */
    val syncState: StateFlow<String> = workManager
        .getWorkInfosByTagFlow(SYNC_TAG)
        .map { info -> info.maxByOrNull{it.generation}?.state?.name ?: "IDLE" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "IDLE")

    init {
        // Osserva i 3 server dal DB
        viewModelScope.launch {
            repository.getAllServers().collect { servers ->
                Log.d("DEBUG_SERVERS", servers.joinToString { "id=${it.id} name=${it.serverName} ip=${it.ipAddress}" })
                uiState = uiState.copy(servers = servers)
            }
        }

        //collect
        viewModelScope.launch {
            syncState.collect { state ->
                uiState = uiState.copy(isConnected = state == "SUCCEEDED")
            }
        }

        //inizializza gli slot per i server
        viewModelScope.launch {
            ensureDefaultServers()
        }

        //Osserva le playlist
        viewModelScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                uiState = uiState.copy(playlists = playlists)
            }
        }

        //Osserva lo StateFlow condiviso con MusicPlayer per progresso brano
        viewModelScope.launch {
            playbackProgressState.collect { progress ->
                uiState = uiState.copy(playbackProgress = progress)
            }
        }

        //collectors per aggiornare player bar
        viewModelScope.launch {
            currentSongState.collect { song ->
                uiState = uiState.copy(currentSong = song)
            }
        }

        viewModelScope.launch {
            isPlayingState.collect { playing ->
                uiState = uiState.copy(isPlaying = playing)
            }
        }

    }

    //crea 3 slot server con valori di default
    private suspend fun ensureDefaultServers() {
        for (i in 1..3) {
            repository.upsertServerIfAbsent(i, "Server $i")
        }
    }

    /*fun onIpAddressChange(ip: String) {
        uiState = uiState.copy(ipAddress = ip) //Funzione deprecata per aggiornamento IP
    }*/

    /**
     * Job del collector delle canzoni del server attivo. Ricreato e distrutto a ogni cambio server
     * (evita race condition su [MympUiState.songs])
     */
    private var songsCollectionJob: Job? = null

    /**
     * Seleziona un server come attivo, aggiorna [uiState] e avvia
     * la sincronizzazione se l'IP è configurato.
     *
     * Cancella il collector precedente di [getAllSongs] prima di
     * lanciarne uno nuovo, evitando che i brani del server precedente
     * sovrascrivano quelli del nuovo server per via del timing delle coroutine.
     */
    fun selectServer(server: ServerEntity) {
        uiState = uiState.copy(activeServer = server, ipAddress = server.ipAddress)

        songsCollectionJob?.cancel()
        songsCollectionJob = viewModelScope.launch {
            repository.getAllSongs(server.id).collect { songs ->
                uiState = uiState.copy(songs = songs)
            }
        }

        if (server.ipAddress.isBlank()) return

        connectToServer(server)
    }
    

    //DEPRECATA
    /*fun onConnectClick() {
        val active = uiState.activeServer ?: return
        if (uiState.ipAddress.isBlank()) return

        val updated = active.copy(ipAddress = uiState.ipAddress)

        viewModelScope.launch {
            repository.upsertServer(updated)
        }

        uiState = uiState.copy(activeServer = updated)
        connectToServer(updated)
    }*/

    /**
     * Normalizza l'URL del server e schedula un [SyncSongWorker] tramite WorkManager.
     *
     * La normalizzazione aggiunge "http://" se assente e garantisce
     * il trailing slash richiesto da Retrofit come base URL.
     * Il Worker riceve [baseUrl] e [server.id] come input data.
     */
    private fun connectToServer(server: ServerEntity) {
        val baseUrl = server.ipAddress.let {
            if (it.startsWith("http://") || it.startsWith("https://")) it
            else "http://$it"
        }.trimEnd('/') + "/"

        val syncRequest = OneTimeWorkRequestBuilder<SyncSongWorker>()
            .addTag(SYNC_TAG)
            .setInputData(workDataOf(
                SyncSongWorker.KEY_IP to baseUrl,
                SyncSongWorker.KEY_SERVER_ID to server.id
            ))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueue(syncRequest)
    }


    /**
     * Avvia la riproduzione di un brano inviando [MusicService.ACTION_PLAY]
     * al [MusicService] tramite Intent.
     *
     * Passa [displayedSongs] come playlist serializzata in JSON —
     * non [MympUiState.songs] — per garantire che lo skip rispetti
     * la vista attualmente visualizzata (server o playlist).
     */
    fun playSong(context: Context, song: Song){
        uiState = uiState.copy(
            currentSong = song,
            isPlaying = true
        )

        val intent = Intent(context, MusicService::class.java).apply{
            action = MusicService.ACTION_PLAY
            putExtra(MusicService.EXTRA_FILE_PATH, song.filePath)
            putExtra(MusicService.EXTRA_TITLE, song.title)
            putExtra(MusicService.EXTRA_ARTIST, song.artist)
            putExtra(MusicService.EXTRA_PLAYLIST, Json.encodeToString(displayedSongs))
        }

        context.startForegroundService(intent)
    }

    fun pauseResume(context: Context){
        uiState = uiState.copy(isPlaying = !uiState.isPlaying)
        val intent = Intent(context, MusicService::class.java).apply{
            action = MusicService.ACTION_PAUSE_RESUME
        }
        context.startService(intent) //non fa partire un nuovo service, ma chiama onStartCommand()
                                          //passandogli il nuovo intent
    }

    fun skipSong(context: Context) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_SKIP
        }
        context.startService(intent)
        val currentIndex = uiState.songs.indexOf(uiState.currentSong)
        if (currentIndex >= 0 && uiState.songs.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % uiState.songs.size
            uiState = uiState.copy(currentSong = uiState.songs[nextIndex])
        }
    }

    fun stopSong(context: Context) {
        uiState = uiState.copy(currentSong = null, isPlaying = false)
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_STOP
        }
        context.startService(intent)
    }

    //funzioni server

    //salva le modifiche du un server tramite upsert
    fun updateServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.upsertServer(server)
        }
    }

    //Funzione Playlist

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Int) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Int, serverId: Int, remoteId: Int) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, serverId, remoteId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Int, serverId: Int, remoteId: Int) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, serverId, remoteId)
        }
    }

    //job del collcetor dei brani di una playlist attiva
    private var playlistSongsJob: Job? = null

    fun loadPlaylistSongs(playlistId: Int) {
        playlistSongsJob?.cancel()
        playlistSongsJob = viewModelScope.launch {
            repository.getSongsForPlaylist(playlistId).collect { songs ->
                uiState = uiState.copy(currentPlaylistSongs = songs)
            }
        }
    }

    fun addSongToNewPlaylist(playlistName: String, song: Song) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(playlistName)
            repository.addSongToPlaylist(playlistId.toInt(), song.serverId, song.remoteId)
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        uiState = uiState.copy(activePlaylist = playlist)
        if (playlist == null) return
        loadPlaylistSongs(playlist.playlistId)
    }

    //Funzione query e sorting

    /**
     * Proprietà calcolata che restituisce la lista di brani da visualizzare,
     * applicando filtro e ordinamento allo stato corrente.
     *
     * La sorgente è [MympUiState.currentPlaylistSongs] se è attiva una playlist,
     * altrimenti [MympUiState.songs] del server attivo.
     * Ricalcolata automaticamente ad ogni accesso, riflettendo sempre
     * [MympUiState.searchQuery] e [MympUiState.sortOrder] aggiornati.
     */
    val displayedSongs: List<Song>
        get() {
            val base = if (uiState.activePlaylist != null) //variabile base da cui filtrare/sorting
                uiState.currentPlaylistSongs
            else
                uiState.songs

            val filtered = if (uiState.searchQuery.isBlank()) base
            else base.filter {
                it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                        it.artist.contains(uiState.searchQuery, ignoreCase = true)
            }

            return when (uiState.sortOrder) {
                SortOrder.TITLE_ASC -> filtered.sortedBy { it.title }
                SortOrder.TITLE_DES -> filtered.sortedByDescending { it.title }
                SortOrder.ARTIST_ASC -> filtered.sortedBy { it.artist }
                SortOrder.ARTIST_DES -> filtered.sortedByDescending { it.artist }
            }
        }


    fun onSearchQueryChange(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun onSortOrderChange(order: SortOrder) {
        uiState = uiState.copy(sortOrder = order)
    }

    //seek con slider
    fun seekTo(context: Context, progress: Float) {
        val intent = Intent(context, MusicService::class.java).apply {
            action = MusicService.ACTION_SEEK
            putExtra(MusicService.EXTRA_SEEK_POSITION, progress)
        }
        context.startService(intent)
    }

    /**
     * Factory per la creazione del ViewModel con parametri custom.
     * Necessaria perché [MympViewModel] non ha un costruttore vuoto —
     * ViewModelProvider non saprebbe come istanziarlo senza questa Factory.
     */
    class Factory(
        private val repository: MympRepository,
        private val workManager: WorkManager,
        private val currentSongState: MutableStateFlow<Song?>,
        private val isPlayingState: MutableStateFlow<Boolean>,
        private val playbackProgressState: MutableStateFlow<Float>
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MympViewModel(repository, workManager, currentSongState, isPlayingState, playbackProgressState) as T
        }
    }

}