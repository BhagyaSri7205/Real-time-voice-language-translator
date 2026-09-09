package com.voxtranslate.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Delete
    suspend fun delete(entity: HistoryEntity)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
