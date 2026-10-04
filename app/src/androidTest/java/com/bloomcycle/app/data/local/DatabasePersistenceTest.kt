package com.bloomcycle.app.data.local

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bloomcycle.app.data.crypto.PassphraseProvider
import com.bloomcycle.app.domain.model.FlowLevel
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Phase 4 acceptance test from plan.md: data written through SQLCipher survives a
 * close and a cold reopen, and the file on disk never contains readable period data.
 *
 * "Reopen" here means a genuinely fresh [DatabaseFactory] — a new SQLCipher driver that
 * re-reads the passphrase out of SharedPreferences and re-unwrap it through the Keystore.
 * That round trip is the part that breaks in the wild (screen-lock change, process death,
 * backup restore), and it is the same path a cold process takes. The stricter
 * *separate-process* variant needs two instrumentation runs and is blocked on Q9.
 */
@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun startClean() {
        context.deleteDatabase(AppDatabase.NAME)
    }

    @After
    fun wipe() {
        context.deleteDatabase(AppDatabase.NAME)
    }

    @Test
    fun written_data_survives_close_and_a_fresh_reopen() {
        runBlocking {
            val marker = PeriodEventEntity(
                startDate = LocalDate.parse("2026-10-02"),
                endDate = LocalDate.parse("2026-10-06"),
                flow = FlowLevel.HEAVY,
                isSpottingOnly = false,
                notes = "canary-written-by-the-first-connection",
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

            val first = openDatabase()
            val id = first.periodEventDao().upsert(marker)
            first.close()

            // Fresh factory: new passphrase read, new driver, new Room instance.
            val second = openDatabase()
            val rows = second.periodEventDao().getAll()
            second.close()

            assertEquals(1, rows.size)
            assertEquals(id, rows[0].id)
            assertEquals(marker.startDate, rows[0].startDate)
            assertEquals(marker.endDate, rows[0].endDate)
            assertEquals(FlowLevel.HEAVY, rows[0].flow)
            assertEquals(marker.notes, rows[0].notes)
        }
    }

    @Test
    fun the_on_disk_database_contains_no_readable_period_data() {
        runBlocking {
            val canary = "BLOOM_PLAINTEXT_CANARY_20261002"
            val db = openDatabase()
            db.periodEventDao().upsert(
                PeriodEventEntity(
                    startDate = LocalDate.parse("2026-10-02"),
                    endDate = null,
                    flow = null,
                    isSpottingOnly = false,
                    notes = canary,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            )
            db.close()

            val file = context.getDatabasePath(AppDatabase.NAME)
            assertTrue("database file should exist", file.exists())
            assertTrue("database file should not be empty", file.length() > 0)

            // Room runs in WAL mode, so plaintext would surface in the sidecar files if
            // encryption were only applied to the main one. Check every byte on disk that
            // belongs to this database.
            val onDisk = buildList {
                add(file)
                listOf("-wal", "-shm", "-journal").forEach { suffix ->
                    java.io.File(file.path + suffix).takeIf { it.exists() }?.let(::add)
                }
            }

            // The acceptance criterion in plan.md §4, checked in-process instead of via xxd:
            // if SQLCipher is actually on, none of this canary is recoverable from any of them.
            onDisk.forEach { candidate ->
                val bytes = candidate.readBytes()
                assertNull(
                    "${candidate.name}: period notes must not appear as plaintext",
                    indexOfAscii(bytes, canary),
                )
                assertNull(
                    "${candidate.name}: the ISO period date must not appear as plaintext",
                    indexOfAscii(bytes, "2026-10-02"),
                )
            }
        }
    }

    private fun openDatabase(): AppDatabase =
        DatabaseFactory(context, PassphraseProvider(context)).create()

    /** First offset of [needle] read as ASCII/UTF-8 out of [haystack], or null. */
    private fun indexOfAscii(haystack: ByteArray, needle: String): Int? {
        val target = needle.encodeToByteArray()
        if (target.isEmpty()) return 0
        outer@ for (i in 0..haystack.size - target.size) {
            for (j in target.indices) {
                if (haystack[i + j] != target[j]) continue@outer
            }
            return i
        }
        return null
    }
}
