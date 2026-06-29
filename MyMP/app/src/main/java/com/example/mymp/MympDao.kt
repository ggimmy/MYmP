package com.example.mymp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Dao per interfacciarsi al database ROOM e interrogarlo sui brani.
 */
@Dao
interface MympDao {
    /**
     * Osserva in tempo reale tutti i brani di un server specifico,
     * ordinati per titolo. Emette una nuova lista a ogni modifica
     * della tabella "songs" per quel server.
     *
     * @param serverId ID dello slot server (1, 2 o 3)
     */
    @Query("SELECT * FROM songs WHERE serverId = :serverId ORDER BY title ASC") //query per singolo server
    fun getAllForServer(serverId: Int): Flow<List<SongEntity>>

    /**
     * Inserisce un brano. In caso di conflitto, sostituisce la riga esistente
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    /**
     * Ormai inutilizzato, veniva chiamato dal vecchio Sync prima dell'implementazione di Upsert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemote(songs: List<SongEntity>)

    /**
     * Aggiorna i campi di un brano esistente
     */
    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("DELETE FROM songs WHERE serverId = :serverId")
    suspend fun clearSongsForServer(serverId: Int) //clear solo per un server

    /* -- QUERY PER UPSERT -- */

    /**
     * Cerca la coppia (serverId, remoteId) per determinare se un brano è nuovo (insert) o già
     * esistente (upsert)
     *
     * Ritorna l'id locale se esiste, null altriementi.
     */
    @Query("SELECT id FROM songs WHERE serverId = :serverId AND remoteId = :remoteId LIMIT 1")
    suspend fun findLocalId(serverId: Int, remoteId: Int): Int?

    /**
     * Restituisce tutti i remoteID di un server per verificare se ci sono differenze con i nuovi
     * arrivi dal manifest
     */
    @Query("SELECT remoteId FROM songs WHERE serverId = :serverId")
    suspend fun getAllRemoteIdsForServer(serverId: Int): List<Int>

    /**
     * Data la coppia (serverId, remoteId) elimina i brani non più presenti in un server da tutto il
     * database (entrambi i parametri sono marcati con onDelete CASCADE)
     */
    @Query("DELETE FROM songs WHERE serverId = :serverId AND remoteId IN (:remoteIds)")
    suspend fun deleteByRemoteIds(serverId: Int, remoteIds: List<Int>)

}