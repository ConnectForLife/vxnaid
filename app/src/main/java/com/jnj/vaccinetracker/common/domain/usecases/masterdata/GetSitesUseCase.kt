package com.jnj.vaccinetracker.common.domain.usecases.masterdata

import com.jnj.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.jnj.vaccinetracker.common.data.models.api.response.SitesDto
import com.jnj.vaccinetracker.common.data.repositories.MasterDataRepository
import com.jnj.vaccinetracker.common.domain.entities.MasterDataFile
import com.jnj.vaccinetracker.common.domain.entities.Sites
import com.jnj.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSitesUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers, override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<SitesDto, Sites>() {
    init {
        initState()
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.SITES

    override suspend fun SitesDto.toDomain(): Sites {
        logInfo("🌍 GetSitesUseCase.toDomain() - Converting ${this.results.size} sites from DTO to domain")
        this.results.forEach { site ->
            logInfo("🌍 Site DTO: name=${site.name}, uuid=${site.uuid}, locationId=${site.locationId}, parentLocationId=${site.parentLocationId}, parentLocationUuid=${site.parentLocationUuid}")
        }
        return this
    }

    override suspend fun getMasterDataPersistedCache(): Sites {
        val sites = masterDataRepository.readSites()
        val siteCount = sites?.results?.size ?: 0
        logInfo("🌍 GetSitesUseCase.getMasterDataPersistedCache() - Read $siteCount sites from cache")
        return sites ?: Sites(emptyList())
    }

    override suspend fun getMasterDataRemote(): Sites {
        logInfo("🌍 GetSitesUseCase.getMasterDataRemote() - Fetching sites from API")
        val sites = vaccineTrackerApiDataSource.getSites()
        logInfo("🌍 GetSitesUseCase.getMasterDataRemote() - Retrieved ${sites.results.size} sites from API")
        sites.results.forEach { site ->
            logInfo("🌍 Remote Site: name=${site.name}, uuid=${site.uuid}, locationId=${site.locationId}, parentLocationId=${site.parentLocationId}, parentLocationUuid=${site.parentLocationUuid}")
        }
        return sites
    }

    override fun getMemoryCache(): Sites? {
        return masterDataMemoryDataSource.getSites()
    }

    override fun setMemoryCache(memoryCache: Sites?) {
        masterDataMemoryDataSource.setSites(memoryCache)
    }

    suspend fun refreshFromRemote(): Sites {
        logInfo("🌍 GetSitesUseCase.refreshFromRemote() - Force fetching sites from API")
        return getMasterDataRemote().also { setMemoryCache(it) }
    }
}