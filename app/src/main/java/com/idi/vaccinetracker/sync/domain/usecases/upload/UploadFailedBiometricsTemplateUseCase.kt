package com.idi.vaccinetracker.sync.domain.usecases.upload

import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.domain.entities.BiometricsTemplateBytes
import com.idi.vaccinetracker.common.domain.entities.DraftParticipantBiometricsTemplateFile
import com.idi.vaccinetracker.common.exceptions.*
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class UploadFailedBiometricsTemplateUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val participantDataFileIO: ParticipantDataFileIO,
) {

    sealed class Result {
        object InvalidTemplate : Result()
        object Success : Result()
        object TemplateAlreadyExists : Result()
        object ParticipantNotFound : Result()
        object LocalTemplateNotAvailable : Result()
        class UnknownWebCallError(val exception: Exception) : Result()
    }

    private suspend fun DraftParticipantBiometricsTemplateFile.readBytes() = participantDataFileIO.readParticipantDataFileContent(this)
        ?.let { BiometricsTemplateBytes(it) }

    suspend fun upload(draftParticipantBiometricsTemplateFile: DraftParticipantBiometricsTemplateFile): Result {
        val templateBytes = draftParticipantBiometricsTemplateFile.readBytes()
        return if (templateBytes != null) {
            try {
                api.personTemplate(draftParticipantBiometricsTemplateFile.participantUuid, templateBytes)
                Result.Success
            } catch (ex: WebCallException) {
                logError("error upload person template", ex)
                when (ex.cause) {
                    is TemplateInvalidException -> Result.InvalidTemplate
                    is TemplateAlreadyExistsException, is DuplicateRequestException -> Result.TemplateAlreadyExists
                    is ParticipantNotFoundException -> Result.ParticipantNotFound
                    else -> Result.UnknownWebCallError(ex)
                }
            }
        } else {
            Result.LocalTemplateNotAvailable
        }
    }
}