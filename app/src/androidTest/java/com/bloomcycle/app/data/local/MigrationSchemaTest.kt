package com.bloomcycle.app.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bloomcycle.app.data.crypto.PassphraseProvider
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.driver.SQLCipherDriver
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards plan.md §6.3: the exported schema in `app/schemas/` is a real, usable baseline.
 *
 * The harness was proven at version 1, so the first real migration (1 → 2, dropping
 * the local chat table for Phase 11b) is not also the first time
 * `MigrationTestHelper` is exercised. Never bump `AppDatabase.version` without a
 * matching `migration_*` test here.
 *
 * The helper is driven through the production SQLCipher driver rather than a plain
 * SQLite one, so a migration is validated against the same stack the app actually runs.
 * Schemas are loaded from `androidTest` assets, which the Room Gradle Plugin wires up
 * from `schemaDirectory`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationSchemaTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = instrumentation.targetContext.getDatabasePath(TEST_DB),
        driver = sqlCipherDriver(instrumentation.targetContext),
        databaseClass = AppDatabase::class,
    )

    @Before
    fun dropLeftovers() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @After
    fun cleanUp() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun the_exported_schema_creates_a_usable_database_at_version_one() {
        runBlocking {
            val connection = helper.createDatabase(1)

            connection
                .prepare(
                    "INSERT INTO period_events (startDate, isSpottingOnly, createdAt, updatedAt) " +
                        "VALUES ('2026-10-02', 0, 0, 0)",
                )
                .use { it.step() }

            connection.prepare("SELECT COUNT(*) FROM period_events").use { statement ->
                statement.step()
                assertEquals(1, statement.getInt(0))
            }
            // Left open on purpose: MigrationTestHelper closes what it hands out when the rule ends.
        }
    }

    @Test
    fun the_live_schema_matches_the_exported_json() {
        runBlocking {
            helper.createDatabase(1)
            // Migrates to the current version and validates against the exported JSON.
            helper.runMigrationsAndValidate(2, listOf(AppDatabase.MIGRATION_1_2))
        }
    }

    /**
     * Phase 11b: the local chat table goes away — messages live in Firestore now and
     * must never sit in the encrypted store beside health data (plan.md §7.4).
     * Anything unsent in the local rooms is discarded by the drop.
     */
    @Test
    fun migration_1_to_2_drops_the_local_chat_table_but_keeps_periods() {
        runBlocking {
            val connection = helper.createDatabase(1)

            connection
                .prepare(
                    "INSERT INTO chat_messages " +
                        "(id, text, authorName, phaseBucket, isOwn, createdAt, scope) " +
                        "VALUES ('m1', 'hello', 'Quiet Fern', 'near ovulation', 1, 0, 'GLOBAL')",
                )
                .use { it.step() }
            connection
                .prepare(
                    "INSERT INTO period_events (startDate, isSpottingOnly, createdAt, updatedAt) " +
                        "VALUES ('2026-10-02', 0, 0, 0)",
                )
                .use { it.step() }

            val migrated = helper.runMigrationsAndValidate(2, listOf(AppDatabase.MIGRATION_1_2))

            migrated
                .prepare(
                    "SELECT name FROM sqlite_master " +
                        "WHERE type = 'table' AND name = 'chat_messages'",
                )
                .use { statement ->
                    assertEquals(false, statement.step())
                }
            migrated.prepare("SELECT COUNT(*) FROM period_events").use { statement ->
                statement.step()
                assertEquals(1, statement.getInt(0))
            }
            // Left open on purpose: MigrationTestHelper closes what it hands out when the rule ends.
        }
    }

    private fun sqlCipherDriver(context: android.content.Context): SQLCipherDriver {
        // The native library must be loaded before any database work happens.
        System.loadLibrary("sqlcipher")
        return SQLCipherDriver(PassphraseProvider(context).getOrCreate(), null, null)
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
