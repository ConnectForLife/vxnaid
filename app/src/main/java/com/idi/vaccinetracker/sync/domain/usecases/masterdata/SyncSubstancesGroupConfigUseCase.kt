package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncSubstancesGroupConfigUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<SubstancesGroupConfig>() {
    override suspend fun getMasterDataRemote(): SubstancesGroupConfig = api.getSubstancesGroupConfig()

    override suspend fun storeMasterData(masterData: SubstancesGroupConfig) {
        masterDataRepository.writeSubstancesGroupConfig(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_GROUP_CONFIG
}