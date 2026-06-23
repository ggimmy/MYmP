package com.example.mymp

import kotlinx.serialization.Serializable

/*
@Serializable
data class ResponseDto(
    val songs: List<SongDto>,
    val totalSongs: Int
)
*/

@Serializable
data class SongDto(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String
)
