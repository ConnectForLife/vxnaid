package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import javax.inject.Inject

class FindParticipantByParticipantUuidUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
) {
    suspend fun findByParticipantUuid(participantUuid: String): ParticipantBase? {
        return participantRepository.findByParticipantUuid(participantUuid)
            ?: draftParticipantRepository.findByParticipantUuid(participantUuid)
    }
}