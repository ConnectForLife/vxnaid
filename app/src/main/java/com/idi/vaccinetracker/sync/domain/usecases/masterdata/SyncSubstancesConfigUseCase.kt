package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.SubstancesConfig
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncSubstancesConfigUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<SubstancesConfig>() {
    override suspend fun getMasterDataRemote() = api.getSubstancesConfig()

    override suspend fun storeMasterData(masterData: SubstancesConfig) {
        masterDataRepository.writeSubstancesConfig(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_CONFIG
}