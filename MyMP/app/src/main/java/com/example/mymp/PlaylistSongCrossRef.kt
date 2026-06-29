package com.example.mymp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entità relazionale per associazione molti a molti tra [PlaylistEntity] e [SongEntity] con PK composta.
 *
 * La primary key di questa tabella è composta dalla tripla (playlistId, serverId, remoteId), le
 * quali a loro volta sono foreign keys.
 *
 * Questo per garantire l'unicità e la consistenza dei brani in playlist trasversali.
 * (Le foreign key sono marcate con ON DELETE CASCADE quindi quando la loro ref principale viene
 * rimossa, si cancella anche nelle altre tabelle)
 *
 */
@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "serverId", "remoteId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["serverId", "remoteId"],
            childColumns = ["serverId", "remoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["serverId", "remoteId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Int, //collegamento a PlaylistEntity
    val serverId: Int,   //collegamento a SongEntity
    val remoteId: Int,   //collegamento a SongEntity
    val position: Int = 0 //inutilizzato
)