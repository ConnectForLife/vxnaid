package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.OtherSubstancesConfig
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetOtherSubstancesConfigUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<OtherSubstancesConfig, OtherSubstancesConfig>() {

    init {
        initState()
    }

    override fun getMemoryCache(): OtherSubstancesConfig? {
        return masterDataMemoryDataSource.getOtherSubstancesConfig()
    }

    override fun setMemoryCache(memoryCache: OtherSubstancesConfig?) {
        masterDataMemoryDataSource.setOtherSubstancesConfig(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.OTHER_SUBSTANCES_CONFIG

    override suspend fun OtherSubstancesConfig.toDomain(): OtherSubstancesConfig {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readOtherSubstancesConfig()

    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getOtherSubstancesConfig()
}