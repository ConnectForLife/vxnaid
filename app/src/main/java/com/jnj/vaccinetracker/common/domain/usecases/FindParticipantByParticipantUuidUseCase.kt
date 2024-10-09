package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.models.toDomain
import com.jnj.vaccinetracker.common.domain.entities.Participant
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.sync.data.models.GetParticipantsByUuidsRequest
import com.jnj.vaccinetracker.sync.data.models.ParticipantSyncRecord
import com.jnj.vaccinetracker.sync.data.models.toMap
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class FindParticipantByParticipantUuidUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
) {

    /**
     * Maps a ParticipantSyncRecord.Update to the domain entity Participant.
     */
    private fun ParticipantSyncRecord.Update.toDomain(): Participant {
        return Participant(
            participantUuid = participantUuid,
            dateModified = dateModified.date,
            image = null,
            biometricsTemplate = null,
            participantId = participantId,
            nin = nin,
            childNumber = childNumber,
            gender = gender,
            isBirthDateEstimated = isBirthDateEstimated,
            birthDate = birthDate.toDomain(),
            attributes = attributes.toMap(),
            address = address,
            childFirstName = childFirstName,
            childLastName = childLastName
        )
    }

    /**
     * Extracts the participant from a list of ParticipantSyncRecords.
     * Returns null if the record represents a delete or if the list is empty.
     */
    private fun getParticipant(response: List<ParticipantSyncRecord>): Participant? {
        if (response.isEmpty()) {
            return null
        }

        return when (val participantSyncRecord = response[0]) {
            is ParticipantSyncRecord.Delete -> null
            is ParticipantSyncRecord.Update -> participantSyncRecord.toDomain()
        }
    }

    /**
     * Finds a participant by UUID. Searches in the local repository and the API if not found locally.
     * Returns null if the participant is not found or has been deleted.
     */
    suspend fun findByParticipantUuid(participantUuid: String): ParticipantBase? {
        val draftParticipant = draftParticipantRepository.findByParticipantUuid(participantUuid)

        val participant = draftParticipant ?: participantRepository.findByParticipantUuid(participantUuid)

        if (participant != null) {
            return participant
        }

        val remoteResponse = api.getParticipantsByUuids(GetParticipantsByUuidsRequest(listOf(participantUuid)))

        return getParticipant(remoteResponse)
    }
}
