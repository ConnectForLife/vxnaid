package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.domain.usecases.syncscope.base.DeleteDataFileUseCaseBase
import javax.inject.Inject

class DeleteAllImagesUseCase @Inject constructor(
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val participantImageRepository: ParticipantImageRepository,
    override val participantDataFileIO: ParticipantDataFileIO,
    private val deleteAllFailedSyncRecordsUseCase: DeleteAllFailedSyncRecordsUseCase,
    private val deleteAllDeletedSyncRecordsUseCase: DeleteAllDeletedSyncRecordsUseCase,
) : DeleteDataFileUseCaseBase() {

    private val syncEntityType = SyncEntityType.IMAGE

    suspend fun deleteAllImages(deleteUploadedDrafts: Boolean) {
        logInfo("deleteAllImages $deleteUploadedDrafts")
        deleteAllDeletedSyncRecordsUseCase.deleteAll(syncEntityType)
        deleteAllFailedSyncRecordsUseCase.deleteAll(syncEntityType)
        participantDataFileIO.deleteAllSyncImages()
        participantImageRepository.deleteAll()
        if (deleteUploadedDrafts) {
            deleteFilesQuery { offset, limit ->
                draftParticipantImageRepository.findAllUploaded(offset, limit)
            }
            draftParticipantImageRepository.deleteAllUploaded()
        }
    }
}