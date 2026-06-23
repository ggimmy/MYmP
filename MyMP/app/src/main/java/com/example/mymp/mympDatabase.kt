package com.example.mymp

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SongEntity::class, ServerEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 4
)
abstract class mympDatabase : RoomDatabase(){
    abstract fun mympDao(): mympDao
    abstract fun ServerDao(): ServerDao

    abstract fun playlistDao(): PlaylistDao
}
