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
            childLastName = childLastName,
            dateCreated = dateCreated
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
        return try {
            val draftParticipant = draftParticipantRepository.findByParticipantUuid(participantUuid)

            val participant = draftParticipant ?: participantRepository.findByParticipantUuid(participantUuid)

            if (participant != null) {
                return participant
            }

            val remoteResponse = api.getParticipantsByUuids(GetParticipantsByUuidsRequest(listOf(participantUuid)))

            getParticipant(remoteResponse)
        } catch (e: Exception) {
            println("Error occurred while finding participant by UUID: ${e.message}")
            null
        }
    }

    suspend fun findByParticipantUuids(participantUuids: Set<String>): List<ParticipantBase> {
        return try {
            val draftParticipants =
                draftParticipantRepository.findByParticipantUuids(participantUuids)

            val notIncludedInDraft =
                participantUuids.filter { participantUuid -> draftParticipants.find { it.participantUuid == participantUuid } == null }
                    .toSet()
            val localParticipants = participantRepository.findByParticipantUuids(notIncludedInDraft)

            val notIncludedInLocal =
                notIncludedInDraft.filter { participantUuid -> localParticipants.find { it.participantUuid == participantUuid } == null }
                    .toSet()

            val remoteParticipants = notIncludedInLocal
                .chunked(256)
                .flatMap { chunk -> api.getParticipantsByUuids(GetParticipantsByUuidsRequest(chunk.toList())) }
                .filterIsInstance<ParticipantSyncRecord.Update>()
                .map { it.toDomain() }

            return draftParticipants + localParticipants + remoteParticipants
        } catch (e: Exception) {
            println("Error occurred while finding participant by UUID: ${e.message}")
            emptyList()
        }
    }
}
