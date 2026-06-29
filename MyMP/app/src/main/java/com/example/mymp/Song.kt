package com.example.mymp

import kotlinx.serialization.Serializable

/**
 * Modello di dominio che rappresenta un brano musicale.
 *
 * È la rappresentazione centrale del brano usata in tutta l'app,
 * dalla UI al ViewModel. Non contiene dettagli di persistenza
 * (come l'id Room) né dettagli di rete (come i campi extra del DTO).
 *
 * Il tag [@Serializable] è necessario per due motivi:
 * - permette a [kotlinx.serialization] di serializzare la lista di brani
 *   in JSON tramite [Json.encodeToString] prima di passarla al [MusicService]
 *   tramite Intent (un Intent può trasportare solo tipi primitivi o stringhe)
 * - permette la deserializzazione nel [MusicService] tramite [Json.decodeFromString]
 *   per ricostruire la playlist interna al servizio
 *
 * @property title Titolo del brano
 * @property artist Nome dell'artista
 * @property album Nome dell'album di appartenenza
 * @property filePath URL assoluto del file audio sul server
 * @property serverId ID del server da cui proviene il brano
 * @property remoteId ID assegnato al brano dal server remoto
 */

@Serializable
data class Song(
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String,
    val serverId: Int = 0, //id del server in cui è situato l'oggetto
    val remoteId: Int = 0 //id remoto, specificato nel manifest
)