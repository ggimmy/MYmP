package com.example.mymp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO per interfacciarsi al database Room e interrogarlo sui server
 */
@Dao
interface ServerDao {

    /**
     * Osserva tutti i server presenti al momento nel database
     */
    @Query("SELECT * FROM servers ORDER BY id ASC")
    fun getAllServers() : Flow<List<ServerEntity>>

    /**
     * Verifica l'esistenza di un server per verificare se fare upsert o insert
     */
    @Query("SELECT COUNT(*) FROM servers WHERE id = :id")
    suspend fun exists(id: Int): Int

    /**
     * Inserisce o aggiorna un nuovo server nei 3 slot
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: ServerEntity) //update or insert

}
