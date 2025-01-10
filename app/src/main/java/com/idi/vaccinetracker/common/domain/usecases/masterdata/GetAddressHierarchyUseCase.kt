package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.mappers.AddressHierarchyDtoMapper
import com.idi.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.AddressHierarchy
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAddressHierarchyUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers,
    override val syncLogger: SyncLogger,
    private val addressHierarchyDtoMapper: AddressHierarchyDtoMapper,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<AddressHierarchyDto, AddressHierarchy>() {
    init {
        initState()
        observeConfiguration()
    }

    private suspend fun onNewConfigurationLoaded() {
        logInfo("onNewConfigurationLoaded")
        // clear address hierarchy in memory
        setMemoryCache(null)
        // reload the address hierarchy
        try {
            getMasterData()
        } catch (ex: Exception) {
            ex.rethrowIfFatal()
            logError("onNewConfigurationLoaded failed to reload address hierarchy", ex)
        }
    }

    /**
     * we want to monitor configuration changes because it has impact on the address hierarchy mapping
     */
    private fun observeConfiguration() {
        syncLogger.observeMasterDataLoadedInMemory(MasterDataFile.CONFIGURATION)
            .onEach { onNewConfigurationLoaded() }
            .launchIn(scope)
    }

    override fun getMemoryCache(): AddressHierarchy? {
        return masterDataMemoryDataSource.getAddressHierarchy()
    }

    override fun setMemoryCache(memoryCache: AddressHierarchy?) {
        masterDataMemoryDataSource.setAddressHierarchy(memoryCache)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.ADDRESS_HIERARCHY

    override suspend fun AddressHierarchyDto.toDomain(): AddressHierarchy {
        return addressHierarchyDtoMapper.toDomain(this)
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readAddressHierarchy()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getCountryAddressHierarchy()
}