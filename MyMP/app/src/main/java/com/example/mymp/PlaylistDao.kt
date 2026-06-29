package com.example.mymp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO per interfacciarsi al database Room e interroggarlo sulle playlist
 */
@Dao
interface PlaylistDao {

    // --- CRUD Playlist ---

    /**
     * Inserisce playlist e ritorna l'id autogenerato
     *
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    /**
     * Osserva le playlist presenti nel DB
     */
    @Query("SELECT * FROM playlist ORDER BY playlistName ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /**
     * Elimina la playlist per ID
     *
     */
    @Query("DELETE FROM playlist WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    // --- Gestione brani in playlist ---


    //Inserisce una entry nella cross-ref
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    //Rimuove un brano dalla playlist identificato dalla tripla (playlistID, serverID, remoteId)
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND serverId = :serverId AND remoteId = :remoteId")
    suspend fun removeSongFromPlaylist(playlistId: Int, serverId: Int, remoteId: Int)

    /**
     * Recupera tutti i brani di una playlist tramite JOIN tra
     * [PlaylistSongCrossRef] e [SongEntity] sulla coppia (serverId, remoteId).
     *
     * Restituisce un [Flow] che emette automaticamente una nuova lista
     * ogni volta che i brani della playlist cambiano — ad esempio
     * quando un brano viene aggiunto, rimosso, o eliminato dal server
     * durante una re-sync (CASCADE).
     */
    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_song_cross_ref ON songs.serverId = playlist_song_cross_ref.serverId
        AND songs.remoteId = playlist_song_cross_ref.remoteId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
        ORDER BY songs.title ASC
    """)
    fun getSongsForPlaylist(playlistId: Int): Flow<List<SongEntity>>

    /**
     * Verifica se un brano è già presente in una playlist specifica.
     * Usato nella UI per mostrare un feedback visivo all'utente
     * nel dialog di aggiunta brano.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND serverId = :serverId AND remoteId = :remoteId)")
    suspend fun isSongInPlaylist(playlistId: Int, serverId: Int, remoteId: Int): Boolean
}
