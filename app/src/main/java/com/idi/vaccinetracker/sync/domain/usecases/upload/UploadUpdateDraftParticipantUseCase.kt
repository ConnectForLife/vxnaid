package com.idi.vaccinetracker.sync.domain.usecases.upload

import com.idi.vaccinetracker.common.data.database.mappers.toDto
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.files.ParticipantDataFileIO
import com.idi.vaccinetracker.common.data.helpers.Base64
import com.idi.vaccinetracker.common.data.models.api.request.UpdateParticipantRequest
import com.idi.vaccinetracker.common.data.models.api.response.AttributeDto
import com.idi.vaccinetracker.common.data.models.toDto
import com.idi.vaccinetracker.common.domain.entities.DraftParticipant
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.exceptions.WebCallException
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
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
        childNumber = childNumber,
        gender = gender,
        isBirthDateEstimated = isBirthDateEstimated,
        birthdate = birthDate.toDto(),
        addresses = listOfNotNull(address?.toDto()),
        attributes = attributes.map { AttributeDto(it.key, it.value) },
        image = imageBase64,
        updateDate = dateModified,
        participantUuid = participantUuid,
        childFirstName = childFirstName,
        childLastName = childLastName
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