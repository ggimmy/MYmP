package com.example.mymp

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
*
* Entità intermedia per relazione molti a molti con PK composta
*
* */

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
    val playlistId: Int,
    val serverId: Int,
    val remoteId: Int,
    val position: Int = 0
)