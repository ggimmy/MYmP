package com.example.mymp

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,
    val serverId: Int = 0,
    val remoteId: Int = 0
)