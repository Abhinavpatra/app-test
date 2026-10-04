package com.bloomcycle.app.data.local

import androidx.room3.migration.Migration
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
 * Version 1 has no migrations to run — the point of these tests is that the harness
 * *works* today, so the first real migration is not also the first time
 * `MigrationTestHelper` is exercised. Add a `migration_1_to_2_*` test the moment the
 * schema changes; never bump `AppDatabase.version` without one.
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
            // No migrations to apply at v1 — this validates the current schema against the JSON.
            helper.runMigrationsAndValidate(1, emptyList<Migration>())
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
