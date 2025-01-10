package com.idi.vaccinetracker.common.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.datasources.MasterDataMemoryDataSource
import com.idi.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.LanguageMap
import com.idi.vaccinetracker.common.domain.entities.LocalizationMap
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.TranslationMap
import com.idi.vaccinetracker.common.domain.usecases.masterdata.base.GetMasterDataUseCaseBase
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLocalizationMapUseCase @Inject constructor(
    private val vaccineTrackerApiDataSource: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val dispatchers: AppCoroutineDispatchers, override val syncLogger: SyncLogger,
    private val masterDataMemoryDataSource: MasterDataMemoryDataSource,
) : GetMasterDataUseCaseBase<LocalizationMapDto, LocalizationMap>() {

    init {
        initState()
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.LOCALIZATION

    override suspend fun LocalizationMapDto.toDomain(): LocalizationMap {
        return LocalizationMap(LanguageMap(localization.mapValues { TranslationMap(it.value) }))
    }

    override suspend fun getMasterDataPersistedCache() = masterDataRepository.readLocalizationMap()
    override suspend fun getMasterDataRemote() = vaccineTrackerApiDataSource.getLocalization()

    override fun getMemoryCache(): LocalizationMap? {
        return masterDataMemoryDataSource.getLocalization()
    }

    override fun setMemoryCache(memoryCache: LocalizationMap?) {
        masterDataMemoryDataSource.setLocalization(memoryCache)
    }
}