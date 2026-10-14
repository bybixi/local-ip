package com.localip.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM conn_record ORDER BY timestamp DESC LIMIT :limit")
    fun observe(limit: Int): Flow<List<ConnRecord>>

    @Query("SELECT * FROM conn_record ORDER BY timestamp DESC")
    suspend fun all(): List<ConnRecord>

    @Insert
    suspend fun insert(record: ConnRecord): Long

    @Query("SELECT COUNT(*) FROM conn_record")
    suspend fun count(): Int

    @Query("DELETE FROM conn_record")
    suspend fun clear()

    /** 只保留最近 keep 条 */
    @Query(
        "DELETE FROM conn_record WHERE id NOT IN " +
            "(SELECT id FROM conn_record ORDER BY timestamp DESC LIMIT :keep)"
    )
    suspend fun trim(keep: Int)
}
