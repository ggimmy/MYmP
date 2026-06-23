package com.example.mymp

import android.app.Application
import androidx.room.Room

class MympApplication : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            mympDatabase::class.java,
            "mymp-database"
        ).fallbackToDestructiveMigration(true).build()
    }

}