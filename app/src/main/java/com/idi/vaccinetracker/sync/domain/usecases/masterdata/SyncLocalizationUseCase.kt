package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.models.api.response.LocalizationMapDto
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncLocalizationUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository, override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<LocalizationMapDto>() {
    override suspend fun getMasterDataRemote(): LocalizationMapDto {
        return api.getLocalization()
    }

    override suspend fun storeMasterData(masterData: LocalizationMapDto) {
        masterDataRepository.writeLocalizationMap(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.LOCALIZATION
}