package com.bloomcycle.app.data.local

import android.content.Context
import com.bloomcycle.app.data.crypto.PassphraseProvider
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import androidx.room3.Room

/**
 * Opens the SQLCipher-encrypted Room database.
 *
 * Room 3 talks to SQLite through an `SQLiteDriver`, so SQLCipher plugs in as a driver
 * rather than through the Room 2 `SupportSQLiteOpenHelper.Factory` API.
 *
 * The passphrase is deliberately *not* zeroed after use: [SQLCipherDriver] retains the
 * array and hands it to SQLite again whenever a connection is opened or reopened, so
 * clearing it would leave the driver holding zeros and the database would stop opening.
 */
class DatabaseFactory(
    private val context: Context,
    private val passphraseProvider: PassphraseProvider,
) {
    fun create(): AppDatabase {
        // The native SQLCipher library must be loaded before any database work happens.
        // Safe to call more than once; the VM only loads a library once per process.
        System.loadLibrary("sqlcipher")

        val passphrase = passphraseProvider.getOrCreate()
        val file = context.getDatabasePath(AppDatabase.NAME)
        file.parentFile?.mkdirs()

        val driver = SQLCipherDriver(passphrase, null, null)

        return Room.databaseBuilder(context, AppDatabase::class.java, file.absolutePath)
            .setDriver(driver)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }
}
