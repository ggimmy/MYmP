package com.example.mymp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface PlaylistDao {

    // --- CRUD Playlist ---

    //Inserisce playlist e ritorna l'id autogenerato
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    //Osservabile via Flow, si aggiorna ogni volta che viene modificato
    @Query("SELECT * FROM playlist ORDER BY playlistName ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    //Elimina la playlist per ID
    @Query("DELETE FROM playlist WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Int)

    // --- Gestione brani in playlist ---


    //Inserisce una entry nella cross-ref
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    //Rimuove un brano dalla playlist identificato dalla tripla (playlistID, serverID, remoteId)
    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND serverId = :serverId AND remoteId = :remoteId")
    suspend fun removeSongFromPlaylist(playlistId: Int, serverId: Int, remoteId: Int)

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_song_cross_ref ON songs.serverId = playlist_song_cross_ref.serverId
        AND songs.remoteId = playlist_song_cross_ref.remoteId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
        ORDER BY songs.title ASC
    """)
    fun getSongsForPlaylist(playlistId: Int): Flow<List<SongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND serverId = :serverId AND remoteId = :remoteId)")
    suspend fun isSongInPlaylist(playlistId: Int, serverId: Int, remoteId: Int): Boolean
}
