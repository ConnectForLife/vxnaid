package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.OtherSubstancesConfig
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncOtherSubstancesConfigUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<OtherSubstancesConfig>() {
    override suspend fun getMasterDataRemote() = api.getOtherSubstancesConfig()

    override suspend fun storeMasterData(masterData: OtherSubstancesConfig) {
        masterDataRepository.writeOtherSubstancesConfig(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.OTHER_SUBSTANCES_CONFIG
}