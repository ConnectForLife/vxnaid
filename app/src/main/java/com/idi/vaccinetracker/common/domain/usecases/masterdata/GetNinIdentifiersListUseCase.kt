package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.NinIdentifiersList
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetNinIdentifiersListUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<NinIdentifiersList, NinIdentifiersList>() {

    init {
        initState()
    }

    override fun getMemoryCache(): NinIdentifiersList? {
        return masterDataMemoryDataSource.getNinIdentifiersList()
    }

    override fun setMemoryCache(memoryCache: NinIdentifiersList?) {
        masterDataMemoryDataSource.setNinIdentifiersList(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.NIN_IDENTIFIERS_LIST

    override suspend fun NinIdentifiersList.toDomain(): NinIdentifiersList {
        return this
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readNinIdentifiersList()

    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getIdentifiersList(Constants.NIN_IDENTIFIER_TYPE_NAME)
}