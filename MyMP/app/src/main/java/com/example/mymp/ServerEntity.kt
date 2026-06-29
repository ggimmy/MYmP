package com.example.mymp

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *
 * Entità relazionale di un server, mappata da room in una tabella SQLite
 *
 * @property id id univoco che va da 1 a 3. Primary key.
 * @property serverName Nome a lunghezza variabile del server modificabile dall'utente
 * @property ipAddress Indirizzo IP o URL che identifica il server.
 *
 */
@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id : Int, //1 ... 3
    val serverName : String,
    val ipAddress : String
)