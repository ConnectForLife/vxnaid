package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.helpers.logDebug
import javax.inject.Inject

class DeleteAllDeletedSyncRecordsUseCase @Inject constructor(
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) {


    suspend fun deleteAll(syncEntityType: SyncEntityType) = deletedSyncRecordRepository.deleteAll(syncEntityType).also {
        logDebug("deleteAll $syncEntityType")
    }
}