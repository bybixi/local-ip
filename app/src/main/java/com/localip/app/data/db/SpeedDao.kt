package com.localip.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDao {

    @Query("SELECT * FROM speed_record ORDER BY timestamp DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<SpeedRecord>>

    @Insert
    suspend fun insert(record: SpeedRecord): Long

    @Query("DELETE FROM speed_record")
    suspend fun clear()

    @Query(
        "DELETE FROM speed_record WHERE id NOT IN " +
            "(SELECT id FROM speed_record ORDER BY timestamp DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)
}
