package com.example.mymp

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.flow.MutableStateFlow

class MympApplication : Application() {

    //singleton database
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            MympDatabase::class.java,
            "mymp-database"
        ).fallbackToDestructiveMigration(true).build()
    }

    /* - canale di comunicazione fra MusicPlayer e ViewModel - */
    val currentSongState = MutableStateFlow<Song?>(null) //inizializzata a null (nessun brano in riproduzione)
    val isPlayingState = MutableStateFlow(false) //inizializzata a false (non in play)
    val playbackProgressState = MutableStateFlow(0f) //range da 0f a 1f (progresso riproduzione)

}