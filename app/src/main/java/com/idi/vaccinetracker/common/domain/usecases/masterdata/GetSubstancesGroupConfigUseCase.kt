package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSubstancesGroupConfigUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<SubstancesGroupConfig, SubstancesGroupConfig>() {

    init {
        initState()
    }

    override fun getMemoryCache(): SubstancesGroupConfig? {
        return masterDataMemoryDataSource.getSubstancesGroupConfig()
    }

    override fun setMemoryCache(memoryCache: SubstancesGroupConfig?) {
        masterDataMemoryDataSource.setSubstancesGroupConfig(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SUBSTANCES_GROUP_CONFIG

    override suspend fun SubstancesGroupConfig.toDomain(): SubstancesGroupConfig {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readSubstancesGroupConfig()

    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getSubstancesGroupConfig()
}