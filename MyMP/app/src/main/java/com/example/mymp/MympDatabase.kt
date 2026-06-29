package com.example.mymp

import androidx.room.Database
import androidx.room.RoomDatabase


/**
 * Istanza database Room con le 4 tabelle
 */
@Database(
    entities = [SongEntity::class, ServerEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 4
)
abstract class MympDatabase : RoomDatabase(){
    abstract fun mympDao(): MympDao
    abstract fun serverDao(): ServerDao

    abstract fun playlistDao(): PlaylistDao
}
