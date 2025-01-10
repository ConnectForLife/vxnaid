package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.idi.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.domain.entities.ParticipantBase
import javax.inject.Inject

class FindParticipantByParticipantIdUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val participantRepository: ParticipantRepository,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) {
    suspend fun findByParticipantId(participantId: String): ParticipantBase? {
        return participantRepository.findByParticipantId(participantId)
            ?: draftParticipantRepository.findByParticipantId(participantId)
    }

    suspend fun findDeletedParticipantbyId(participantId: String): RoomDeletedParticipantModel?{
        return deletedSyncRecordRepository.findByParticipantId(participantId)
    }
}