package com.idi.vaccinetracker.sync.domain.usecases.delete

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.DraftParticipantBiometricsTemplateFile
import com.idi.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFile
import com.idi.vaccinetracker.common.domain.entities.ParticipantBiometricsTemplateFileBase
import com.idi.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class DeleteBiometricsTemplateUseCase @Inject constructor(
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
    private val participantBiometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
    private val participantDataFileIO: ParticipantDataFileIO,
) {

    suspend fun delete(template: ParticipantBiometricsTemplateFileBase) {
        logInfo("delete template for participant ${template::class.simpleName} ${template.participantUuid}")
        participantDataFileIO.deleteParticipantDataFile(template)
        when (template) {
            is DraftParticipantBiometricsTemplateFile -> {
                draftParticipantBiometricsTemplateRepository.deleteByParticipantUuid(template.participantUuid)
            }
            is ParticipantBiometricsTemplateFile -> {
                participantBiometricsTemplateRepository.deleteByParticipantUuid(template.participantUuid)
            }
        }.let { }
    }
}