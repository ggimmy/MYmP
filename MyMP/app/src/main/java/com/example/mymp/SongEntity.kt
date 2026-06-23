package com.example.mymp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(tableName = "songs", indices = [Index(value = ["serverId", "remoteId"], unique = true)])
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val remoteId: Int,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,
    val serverId: Int
)

fun SongDto.toEntity(serverId: Int, localId: Int = 0): SongEntity {
    return SongEntity(
        id = localId,
        remoteId = this.id,
        title = this.title,
        artist = this.artist,
        album = this.album,
        filePath = this.filePath,
        serverId = serverId
    )
}