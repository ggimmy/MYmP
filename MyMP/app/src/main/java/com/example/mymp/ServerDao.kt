package com.example.mymp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY id ASC")
    fun getAllServers() : Flow<List<ServerEntity>>

    @Query("SELECT COUNT(*) FROM servers WHERE id = :id")
    suspend fun exists(id: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(server: ServerEntity) //update or insert

}
