package com.bloomcycle.app.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.ColumnTypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

@Database(
    entities = [
        PeriodEventEntity::class,
        SymptomLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodEventDao(): PeriodEventDao
    abstract fun symptomLogDao(): SymptomLogDao

    companion object {
        const val NAME = "bloom.db"

        /**
         * Phase 11b drops the local chat table: messages now live in Firestore and
         * must never sit in the encrypted store beside health data (plan.md §7.4).
         * Anything unsent in the local rooms is discarded by the drop — acceptable,
         * since the 11a rooms were always described as local-only previews.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.prepare("DROP TABLE IF EXISTS chat_messages").use { it.step() }
            }
        }
    }
}
