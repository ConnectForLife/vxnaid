package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.models.api.response.AddressHierarchyDto
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncAddressHierarchyUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<AddressHierarchyDto>() {

    override suspend fun getMasterDataRemote(): AddressHierarchyDto {
        return api.getCountryAddressHierarchy()
    }

    override suspend fun storeMasterData(masterData: AddressHierarchyDto) {
        masterDataRepository.writeAddressHierarchy(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.ADDRESS_HIERARCHY
}