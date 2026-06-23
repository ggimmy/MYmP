package com.example.mymp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
@Dao
interface mympDao {

    @Query("SELECT * FROM songs WHERE serverId = :serverId ORDER BY title ASC") //query per singolo server
    fun getAllForServer(serverId: Int): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemote(songs: List<SongEntity>)

    @Update
    suspend fun update(song: SongEntity)

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("DELETE FROM songs WHERE serverId = :serverId")
    suspend fun clearSongsForServer(serverId: Int) //clear solo per un server

    /* -- QUERY PER UPSERT -- */

    @Query("SELECT id FROM songs WHERE serverId = :serverId AND remoteId = :remoteId LIMIT 1")
    suspend fun findLocalId(serverId: Int, remoteId: Int): Int?

    @Query("SELECT remoteId FROM songs WHERE serverId = :serverId")
    suspend fun getAllRemoteIdsForServer(serverId: Int): List<Int>

    @Query("DELETE FROM songs WHERE serverId = :serverId AND remoteId IN (:remoteIds)")
    suspend fun deleteByRemoteIds(serverId: Int, remoteIds: List<Int>)

}