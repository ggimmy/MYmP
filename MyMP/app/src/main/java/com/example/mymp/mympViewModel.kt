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
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class mympViewModel (
    private val repository: mympRepository,
    private val workManager: WorkManager
) : ViewModel() {

    var uiState by mutableStateOf(mympUiState())
        private set

    companion object {
        const val SYNC_TAG = "sync_songs"
    }

    val SyncState: StateFlow<String> = workManager
        .getWorkInfosByTagFlow(SYNC_TAG)
        .map { info -> info.maxByOrNull{it.generation}?.state?.name ?: "IDLE" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "IDLE")

    init {
        // Inizializza i 3 slot server se non esistono ancora
        viewModelScope.launch {
            repository.getAllServers().collect { servers ->
                Log.d("DEBUG_SERVERS", servers.joinToString { "id=${it.id} name=${it.serverName} ip=${it.ipAddress}" })
                uiState = uiState.copy(servers = servers)
            }
        }

        viewModelScope.launch {
            SyncState.collect { state ->
                uiState = uiState.copy(isConnected = state == "SUCCEEDED")
            }
        }

        viewModelScope.launch {
            ensureDefaultServers()
        }

        viewModelScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                uiState = uiState.copy(playlists = playlists)
            }
        }

    }

    private suspend fun ensureDefaultServers() {
        for (i in 1..3) {
            repository.upsertServerIfAbsent(i, "Server $i")
        }
    }

    fun onIpAddressChange(ip: String) {
        uiState = uiState.copy(ipAddress = ip)
    }

    private var songsCollectionJob: Job? = null

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
    

    fun onConnectClick() {
        val active = uiState.activeServer ?: return
        if (uiState.ipAddress.isBlank()) return

        val updated = active.copy(ipAddress = uiState.ipAddress)

        viewModelScope.launch {
            repository.upsertServer(updated)
        }

        uiState = uiState.copy(activeServer = updated)
        connectToServer(updated)
    }

    private fun connectToServer(server: ServerEntity) {
        val baseUrl = server.ipAddress.let {
            if (it.startsWith("http://") || it.startsWith("https://")) it
            else "http://$it"
        }.trimEnd('/') + "/"

        /*
        viewModelScope.launch {
            try {
                repository.refreshSongs(server.id, baseUrl)
                uiState = uiState.copy(isConnected = true)
            } catch (e: Exception) {
                Log.e("ViewModel", "Refresh failed for server ${server.id}: ${e.message}", e)
                uiState = uiState.copy(isConnected = false)
            }
        }*/

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
            putExtra(MusicService.EXTRA_PLAYLIST, Json.encodeToString(uiState.songs))
        }

        context.startForegroundService(intent)
    }

    fun pauseResume(context: Context){
        uiState = uiState.copy(isPlaying = !uiState.isPlaying)
        val intent = Intent(context, MusicService::class.java).apply{
            action = MusicService.ACTION_PAUSE_RESUME
        }
        context.startService(intent)
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

    fun searchSong(): MutableList<Song> {
        val queryResult = mutableListOf<Song>()

        uiState.activePlaylist?.let {
            uiState.currentPlaylistSongs.forEach { song ->
                if (song.title.contains(uiState.searchQuery)) {
                    queryResult.add(song)
                }
            }
        }

        for (song in uiState.songs) {

            if(song.title.contains(uiState.searchQuery)){
                queryResult.add(song)
            }

        }

        return queryResult
    }




    class Factory(
        private val repository: mympRepository,
        private val workManager: WorkManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return mympViewModel(repository, workManager) as T
        }
    }

}