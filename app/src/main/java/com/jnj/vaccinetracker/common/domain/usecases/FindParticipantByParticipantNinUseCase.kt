package com.jnj.vaccinetracker.common.domain.usecases

import com.jnj.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.sync.domain.entities.DeletedSyncRecord
import javax.inject.Inject

class FindParticipantByParticipantNinUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) {
    suspend fun findByParticipantByNin(participantNin: String): ParticipantBase? {
        return participantRepository.findByParticipantNin(participantNin)
            ?: draftParticipantRepository.findByParticipantNin(participantNin)
    }

    suspend fun findDeletedParticipantByNin(participantNin: String): RoomDeletedParticipantModel?{
        return deletedSyncRecordRepository.findByParticipantNin(participantNin)
    }
}