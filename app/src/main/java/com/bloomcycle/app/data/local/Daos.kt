package com.bloomcycle.app.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodEventDao {
    @Query("SELECT * FROM period_events ORDER BY startDate ASC")
    fun observeAll(): Flow<List<PeriodEventEntity>>

    @Query("SELECT * FROM period_events ORDER BY startDate ASC")
    suspend fun getAll(): List<PeriodEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodEventEntity): Long

    @Query("DELETE FROM period_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM period_events")
    suspend fun clear()
}

@Dao
interface SymptomLogDao {
    @Query("SELECT * FROM symptom_logs ORDER BY date ASC")
    fun observeAll(): Flow<List<SymptomLogEntity>>

    @Query("SELECT * FROM symptom_logs WHERE date = :date")
    suspend fun onDate(date: com.bloomcycle.app.core.time.CycleDate): List<SymptomLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SymptomLogEntity): Long

    @Query("DELETE FROM symptom_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM symptom_logs")
    suspend fun clear()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clear()
}
