package com.bloomcycle.app.data.repo

import com.bloomcycle.app.core.time.CycleClock
import com.bloomcycle.app.core.time.CycleDate
import com.bloomcycle.app.data.local.PeriodEventDao
import com.bloomcycle.app.data.local.SymptomLogDao
import com.bloomcycle.app.data.local.toDomain
import com.bloomcycle.app.data.local.toEntity
import com.bloomcycle.app.domain.model.PeriodEvent
import com.bloomcycle.app.domain.model.SymptomLog
import com.bloomcycle.app.domain.repository.CycleRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleRepositoryImpl(
    private val periodDao: PeriodEventDao,
    private val symptomDao: SymptomLogDao,
    private val clock: CycleClock,
) : CycleRepository {

    override fun observePeriods(): Flow<List<PeriodEvent>> =
        periodDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun periods(): List<PeriodEvent> =
        periodDao.getAll().map { it.toDomain() }

    override suspend fun logPeriod(event: PeriodEvent): Long {
        val now = clock.now()
        val created = if (event.createdAt == Instant.EPOCH) now else event.createdAt
        return periodDao.upsert(event.copy(createdAt = created, updatedAt = now).toEntity())
    }

    override suspend fun updatePeriod(event: PeriodEvent) {
        periodDao.upsert(event.copy(updatedAt = clock.now()).toEntity())
    }

    override suspend fun deletePeriod(event: PeriodEvent) {
        periodDao.deleteById(event.id)
    }

    override fun observeSymptoms(): Flow<List<SymptomLog>> =
        symptomDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun symptomsOn(date: CycleDate): List<SymptomLog> =
        symptomDao.onDate(date).map { it.toDomain() }

    override suspend fun logSymptom(log: SymptomLog): Long =
        symptomDao.upsert(log.toEntity())

    override suspend fun deleteSymptom(log: SymptomLog) {
        symptomDao.deleteById(log.id)
    }

    override suspend fun eraseEverything() {
        periodDao.clear()
        symptomDao.clear()
    }
}
