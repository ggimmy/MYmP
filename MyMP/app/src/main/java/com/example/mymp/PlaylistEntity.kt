package com.example.mymp

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
*
* Entità per playlists.
*
* */

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val playlistId: Int = 0,
    val playlistName: String
)
