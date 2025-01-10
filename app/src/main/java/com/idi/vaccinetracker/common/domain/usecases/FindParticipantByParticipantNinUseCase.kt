package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.data.database.models.RoomDeletedParticipantModel
import com.idi.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.domain.entities.ParticipantBase
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