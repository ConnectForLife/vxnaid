package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.mappers.toDto
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.jnj.vaccinetracker.common.data.helpers.Base64
import com.jnj.vaccinetracker.common.data.models.api.request.RegisterParticipantRequest
import com.jnj.vaccinetracker.common.data.models.api.request.UpdateParticipantRequest
import com.jnj.vaccinetracker.common.data.models.api.request.UpdateVisitObservationDto
import com.jnj.vaccinetracker.common.data.models.api.request.VisitUpdateRequest
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.data.models.toDto
import com.jnj.vaccinetracker.common.domain.entities.DraftParticipant
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.exceptions.DuplicateRequestException
import com.jnj.vaccinetracker.common.exceptions.ParticipantAlreadyExistsException
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class UploadUpdateDraftParticipantUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val participantDataFileIO: ParticipantDataFileIO,
    private val base64: Base64,
    private val draftParticipantRepository: DraftParticipantRepository,
) {

    private fun DraftParticipant.toDto(imageBase64: String?) = UpdateParticipantRequest(
        participantId = participantId,
        nin = nin,
        gender = gender,
        isBirthDateEstimated = isBirthDateEstimated,
        birthdate = birthDate.toDto(),
        addresses = listOfNotNull(address?.toDto()),
        attributes = attributes.map { AttributeDto(it.key, it.value) },
        image = imageBase64,
        updateDate = registrationDate,
        participantUuid = participantUuid
    )

    private suspend fun DraftParticipant.readImageBase64(): String? {
        return if (image != null) {
            participantDataFileIO.readParticipantDataFileContent(image)?.let { base64.encode(it) }
        } else null
    }

    private suspend fun updateDraftStates(uploadedDraftUpdateParticipant: DraftParticipant) {
        draftParticipantRepository.updateDraftState(uploadedDraftUpdateParticipant)
    }

    suspend fun upload(updateDraftParticipant: DraftParticipant) {
        require(updateDraftParticipant.draftState.isPendingUpload()) { "UpdateDraftParticipant already uploaded!" }
        val image = updateDraftParticipant.readImageBase64()
        val request = updateDraftParticipant.toDto(imageBase64 = image)
        try {
            api.updateParticipant(request)
        } catch (ex: WebCallException) {
            throw ex
        }
        val uploadedDraftUpdateParticipant = updateDraftParticipant.copy(
            draftState = DraftState.UPLOADED)
        updateDraftStates(uploadedDraftUpdateParticipant)
    }

}