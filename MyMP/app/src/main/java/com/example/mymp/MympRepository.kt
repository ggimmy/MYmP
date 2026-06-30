package com.example.mymp

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

/**
 * Repository che fungerà da nostro punto di accesso ai Dati e unica source of truth
 */
class MympRepository(
    private val dao: MympDao,
    private val serverDao: ServerDao,
    private val playlistDao: PlaylistDao
) {

    /* FUNZIONI CANZONI */

    /**
     * Funzione utilizzata per osservare le canzoni in un server, utilizzata quando si seleziona un
     * server.
     */
    fun getAllSongs(serverId: Int): Flow<List<Song>> =
        dao.getAllForServer(serverId).map { entities ->
            entities.map { entity ->
                Song(
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album,
                    filePath = entity.filePath,
                    serverId = entity.serverId,
                    remoteId = entity.remoteId
                )
            }
        }

    //Non utilizzata, l'insert manuale da parte dell'utente non è previsto.
    suspend fun addSong(song: Song, serverId: Int) {
        val entity = SongEntity(
            id = 0,
            remoteId = 0,
            title = song.title,
            artist = song.artist,
            album = song.album,
            filePath = song.filePath,
            serverId = serverId
        )
        dao.insert(entity)
    }

    /**
     * Refresh che parte quando viene chiamato il Worker per la sincronizzazione del server
     * Prende le canzoni tramite retrofit e le memorizza in una variabile, songsRemote che verrà
     * utilizzata per il confronto, l'upsert e il delete di brani non più presenti.
     *
     * Prende in input il server da sincronizzare e l'url.
     */
    suspend fun refreshSongs(serverId: Int, baseUrl: String) {
        val json = Json { ignoreUnknownKeys = true }
        val api = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MympApiService::class.java)

        val songsRemote = api.getSongs()

        // Upsert intelligente: aggiorna i brani esistenti (preservando il localId),
        // inserisce quelli nuovi
        for (dto in songsRemote) {
            val existingLocalId = dao.findLocalId(serverId, dto.id)

            if (existingLocalId != null) {
                val updated = dto.toEntity(serverId = serverId, localId = existingLocalId)
                dao.update(updated)
            } else {
                val newEntity = dto.toEntity(serverId = serverId)
                dao.insert(newEntity)
            }
        }

        // Rimuovi i brani non più presenti nel manifest
        val remoteIdsFromManifest = songsRemote.map { it.id }.toSet()
        val remoteIdsInDb = dao.getAllRemoteIdsForServer(serverId).toSet()
        val removedRemoteIds = (remoteIdsInDb - remoteIdsFromManifest).toList()

        if (removedRemoteIds.isNotEmpty()) {
            dao.deleteByRemoteIds(serverId, removedRemoteIds)
        }
    }


    /*FUNZIONI SERVER*/

    /**
     * Funzione per osservare i server
     */
    fun getAllServers(): Flow<List<ServerEntity>> = serverDao.getAllServers()

    suspend fun upsertServer(server: ServerEntity) {
        serverDao.upsert(server)
    }

    suspend fun upsertServerIfAbsent(id: Int, defaultName: String) {
        if (serverDao.exists(id) == 0) {
            serverDao.upsert(ServerEntity(id = id, serverName = defaultName, ipAddress = ""))
        }
    }

    /* FUNZIONI PLAYLISTS */

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> =
        playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String): Long =
        playlistDao.createPlaylist(PlaylistEntity(playlistName = name))

    suspend fun deletePlaylist(playlistId: Int) =
        playlistDao.deletePlaylist(playlistId)

    suspend fun addSongToPlaylist(playlistId: Int, serverId: Int, remoteId: Int) =
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                serverId = serverId,
                remoteId = remoteId
            )
        )

    suspend fun removeSongFromPlaylist(playlistId: Int, serverId: Int, remoteId: Int) =
        playlistDao.removeSongFromPlaylist(playlistId, serverId, remoteId)

    fun getSongsForPlaylist(playlistId: Int): Flow<List<Song>> =
        playlistDao.getSongsForPlaylist(playlistId).map { entities ->
            entities.map { entity ->
                Song(
                    title = entity.title,
                    artist = entity.artist,
                    album = entity.album,
                    filePath = entity.filePath,
                    serverId = entity.serverId,
                    remoteId = entity.remoteId
                )
            }
        }

    suspend fun isSongInPlaylist(playlistId: Int, serverId: Int, remoteId: Int): Boolean =
        playlistDao.isSongInPlaylist(playlistId, serverId, remoteId)

}