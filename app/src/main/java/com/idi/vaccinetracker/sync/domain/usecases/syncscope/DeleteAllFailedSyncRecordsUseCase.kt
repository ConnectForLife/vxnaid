package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.repositories.FailedSyncRecordDownloadRepository
import com.idi.vaccinetracker.common.data.database.repositories.SyncErrorRepository
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorState
import javax.inject.Inject

class DeleteAllFailedSyncRecordsUseCase @Inject constructor(
    private val failedSyncRecordDownloadRepository: FailedSyncRecordDownloadRepository,
    private val syncErrorRepository: SyncErrorRepository,
) {

    /**
     *- Delete all [SyncErrorMetadata.StoreSyncRecord] sync errors that have corresponding failed record downloads of [syncEntityType]
     *- Delete all failed records downloads of [syncEntityType]
     * @return amount of failed records deleted
     */
    suspend fun deleteAll(syncEntityType: SyncEntityType): Int {
        val participantUuids = failedSyncRecordDownloadRepository.findAllParticipantUuids(syncEntityType)
        if (participantUuids.isNotEmpty()) {
            // mark the errors as resolved so they'll be deleted in the backend and subsequently locally as well
            val syncErrors = participantUuids.map { SyncErrorMetadata.StoreSyncRecord(syncEntityType, it, null) }
            syncErrorRepository.updateSyncErrorState(SyncErrorState.RESOLVED, syncErrors)
            // delete all failed records
            failedSyncRecordDownloadRepository.deleteAll(syncEntityType)
        }
        val countDeleted = participantUuids.size
        logInfo("deleted $countDeleted records for $syncEntityType")
        return countDeleted
    }
}