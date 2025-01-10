package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.common.domain.entities.NinIdentifiersList
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.masterdata.base.SyncMasterDataUseCaseBase
import javax.inject.Inject

class SyncNinIdentifiersListUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    override val masterDataRepository: MasterDataRepository,
    override val syncLogger: SyncLogger,
) : SyncMasterDataUseCaseBase<NinIdentifiersList>() {
    override suspend fun getMasterDataRemote(): NinIdentifiersList = api.getIdentifiersList(Constants.NIN_IDENTIFIER_TYPE_NAME)

    override suspend fun storeMasterData(masterData: NinIdentifiersList) {
        masterDataRepository.writeNinIdentifiersList(masterData)
    }

    override val masterDataFile: MasterDataFile
        get() = MasterDataFile.NIN_IDENTIFIERS_LIST
}