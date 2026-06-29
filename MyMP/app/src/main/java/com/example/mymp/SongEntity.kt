package com.example.mymp

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 *
 * Entità RoomDB che rappresenta un brano persistito nel database locale
 *
 * Notare che serverId e remoteId sono UNIQUE per far si che lo stesso brano non venga inserito due
 * volte e per fungere da identificatore univoco in una playlist (come foreign key in [PlaylistSongCrossRef])
 * per il CASCADE delete in caso un brano venga rimosso da un server.
 *
 * @property id Identificatore univoco, primary key.
 * @property remoteId Identificatore univoco assegnato dal server remoto
 * @property title Titolo brano
 * @property artist Nome autore
 * @property album Nome album di appartenenza
 * @property filePath Locazione del brano nel sever remoto
 * @property serverId id locale del server di appartenenza
 *
 */
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


/**
 *
 * Converte un DTO ricevuto dalla rete in un entità ROOM
 *
 */
fun SongDto.toEntity(serverId: Int, localId: Int = 0): SongEntity {
    return SongEntity(
        id = localId, // room genera automaticamente un nuovo id per lui (utile in upsert)
        remoteId = this.id,
        title = this.title,
        artist = this.artist,
        album = this.album,
        filePath = this.filePath,
        serverId = serverId
    )
}