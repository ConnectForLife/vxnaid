package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.sync.data.models.VaccineSchedule
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncVaccineScheduleUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<VaccineSchedule>() {
    override suspend fun getMasterDataRemote() = api.getVaccineSchedule()

    override suspend fun storeMasterData(masterData: VaccineSchedule) {
        masterDataRepository.writeVaccineSchedule(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.VACCINE_SCHEDULE
}