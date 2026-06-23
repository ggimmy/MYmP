package com.example.mymp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id : Int, //1 ... 3
    val serverName : String,
    val ipAddress : String
)