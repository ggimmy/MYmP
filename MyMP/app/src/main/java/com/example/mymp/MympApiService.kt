package com.example.mymp

import retrofit2.http.GET

interface MympApiService {

    @GET("manifest.json")
    suspend fun getSongs(): List<SongDto>

}