package com.bloomcycle.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters

@Database(
    entities = [
        PeriodEventEntity::class,
        SymptomLogEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodEventDao(): PeriodEventDao
    abstract fun symptomLogDao(): SymptomLogDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        const val NAME = "bloom.db"
    }
}
