package com.idi.vaccinetracker.sync.domain.usecases.syncscope

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.sync.domain.usecases.syncscope.base.DeleteDataFileUseCaseBase
import javax.inject.Inject

class DeleteAllBiometricsTemplatesUseCase @Inject constructor(
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    override val participantDataFileIO: ParticipantDataFileIO,
    private val deleteAllFailedSyncRecordsUseCase: DeleteAllFailedSyncRecordsUseCase,
    private val deleteAllDeletedSyncRecordsUseCase: DeleteAllDeletedSyncRecordsUseCase,
) : DeleteDataFileUseCaseBase() {

    private val syncEntityType = SyncEntityType.BIOMETRICS_TEMPLATE

    suspend fun deleteAllBiometricsTemplates(deleteUploadedDrafts: Boolean) {
        logInfo("deleteAllBiometricsTemplates $deleteUploadedDrafts")
        deleteAllFailedSyncRecordsUseCase.deleteAll(syncEntityType)
        deleteAllDeletedSyncRecordsUseCase.deleteAll(syncEntityType)
        participantDataFileIO.deleteAllSyncBiometricsTemplates()
        participantBiometricsTemplateRepository.deleteAll()
        if (deleteUploadedDrafts) {
            deleteFilesQuery { offset, limit ->
                draftParticipantBiometricsTemplateRepository.findAllUploaded(offset, limit)
            }
            draftParticipantBiometricsTemplateRepository.deleteAllUploaded()
        }
    }
}