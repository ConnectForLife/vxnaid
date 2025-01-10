package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import javax.inject.Inject

class GetParticipantRegimenUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val draftParticipantRepository: DraftParticipantRepository,
) {

    suspend fun getParticipantRegimen(participantUuid: String): String? = participantRepository.findRegimen(participantUuid)
        ?: draftParticipantRepository.findRegimen(participantUuid)
}