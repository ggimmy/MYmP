package com.example.mymp

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object che rappresenta un brano ricevuto dal server remoto.
 *
 * Mappa direttamente la struttura JSON esposta dall'endpoint GET /manifest.json.
 * Viene convertito in [SongEntity] tramite [SongDto.toEntity] prima
 * di essere persistito nel database locale Room.
 *
 * NB: i campi di questa classe devono corrispondere esattamente
 * ai campi del manifest.json esposto dal server — aggiungere o
 * rimuovere campi qui richiede una modifica lato server.
 *
 * @property id Identificatore univoco del brano lato server.
 *   Corrisponde a [SongEntity.remoteId] dopo la conversione,
 *   ed è usato come chiave nella logica di upsert durante la sync.
 * @property title Titolo del brano
 * @property artist Nome dell'artista
 * @property album Nome dell'album di appartenenza
 * @property filePath URL assoluto del file audio sul server
 *   (es. "http://192.168.1.10:8080/musica/song.mp3")
 */
@Serializable
data class SongDto(
    val id: Int,
    val title: String,
    val artist: String,
    val album: String,
    val filePath: String
)
