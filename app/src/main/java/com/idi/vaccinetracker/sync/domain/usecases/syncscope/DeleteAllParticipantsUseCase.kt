package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class DeleteAllParticipantsUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
    private val deleteAllFailedSyncRecordsUseCase: DeleteAllFailedSyncRecordsUseCase,
    private val deleteAllDeletedSyncRecordsUseCase: DeleteAllDeletedSyncRecordsUseCase,
) {

    private val syncEntityType = SyncEntityType.PARTICIPANT
    suspend fun deleteAllParticipants(deleteUploadedDrafts: Boolean) {
        logInfo("deleteAllParticipants $deleteUploadedDrafts")
        deleteAllFailedSyncRecordsUseCase.deleteAll(syncEntityType)
        deleteAllDeletedSyncRecordsUseCase.deleteAll(syncEntityType)
        participantRepository.deleteAll()
        if (deleteUploadedDrafts) {
            //normally these are already deleted
            draftParticipantRepository.deleteAllUploaded()
        }
    }
}