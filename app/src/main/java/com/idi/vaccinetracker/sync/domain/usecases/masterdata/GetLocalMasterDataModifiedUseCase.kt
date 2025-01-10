package com.idi.vaccinetracker.sync.domain.usecases.masterdata

import com.idi.vaccinetracker.common.data.repositories.MasterDataRepository
import com.idi.vaccinetracker.common.domain.entities.MasterDataFile
import com.idi.vaccinetracker.sync.data.models.SyncDate
import javax.inject.Inject

class GetLocalMasterDataModifiedUseCase @Inject constructor(private val masterDataRepository: MasterDataRepository) {

    fun getMasterDataSyncDate(masterDataFile: MasterDataFile): SyncDate? {
        return masterDataRepository.getDateModified(masterDataFile)
    }
}