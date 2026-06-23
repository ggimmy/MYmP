package com.example.mymp

import retrofit2.http.GET

interface mympApiService {

    @GET("manifest.json")
    suspend fun getSongs(): List<SongDto>

}