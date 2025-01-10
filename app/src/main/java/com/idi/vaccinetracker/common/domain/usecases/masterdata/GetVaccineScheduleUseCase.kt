package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.sync.data.models.VaccineSchedule
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetVaccineScheduleUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<VaccineSchedule, VaccineSchedule>() {
    init {
        initState()
    }

    override fun getMemoryCache(): VaccineSchedule? {
        return masterDataMemoryDataSource.getVaccineSchedule()
    }

    override fun setMemoryCache(memoryCache: VaccineSchedule?) {
        masterDataMemoryDataSource.setVaccineSchedule(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.VACCINE_SCHEDULE

    override suspend fun VaccineSchedule.toDomain(): VaccineSchedule {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readVaccineSchedule()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getVaccineSchedule()
}