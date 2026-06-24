package com.example.mymp

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow

class MympApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            mympDatabase::class.java,
            "mymp-database"
        ).fallbackToDestructiveMigration(true).build()
    }

    val currentSongState = MutableStateFlow<Song?>(null) //inizializzata a null (nessun brano in riproduzione)
    val isPlayingState = MutableStateFlow(false) //inizializzata a false (non in play)

}