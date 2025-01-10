package com.idi.vaccinetracker.sync.domain.usecases.delete

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.DraftParticipantImageFile
import com.idi.vaccinetracker.common.domain.entities.ParticipantImageFile
import com.idi.vaccinetracker.common.domain.entities.ParticipantImageFileBase
import com.idi.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class DeleteImageUseCase @Inject constructor(
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val participantImageRepository: ParticipantImageRepository,
    private val participantDataFileIO: ParticipantDataFileIO,
) {

    suspend fun delete(image: ParticipantImageFileBase) {
        logInfo("delete image for participant ${image::class.simpleName} ${image.participantUuid}")
        participantDataFileIO.deleteParticipantDataFile(image)
        when (image) {
            is DraftParticipantImageFile -> {
                draftParticipantImageRepository.deleteByParticipantUuid(image.participantUuid)
            }
            is ParticipantImageFile -> {
                participantImageRepository.deleteByParticipantUuid(image.participantUuid)
            }
        }.let { }
    }
}