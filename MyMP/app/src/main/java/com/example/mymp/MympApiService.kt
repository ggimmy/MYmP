package com.example.mymp

import retrofit2.http.GET

/**
 * Istanza retrofit che fa una richiesta HTTP GET /manifest.json, recuperando le canzoni in una lista
 * di DTO serializzati
 */
interface MympApiService {

    @GET("manifest.json")
    suspend fun getSongs(): List<SongDto>

}