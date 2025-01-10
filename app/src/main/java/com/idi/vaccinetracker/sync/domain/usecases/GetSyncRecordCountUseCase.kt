package com.idi.vaccinetracker.sync.domain.usecases

import com.idi.vaccinetracker.common.data.database.repositories.ParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantImageRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.VisitRepository
import com.idi.vaccinetracker.common.data.database.repositories.base.RepositoryBase
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject

class GetSyncRecordCountUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val visitRepository: VisitRepository,
    private val imageRepository: ParticipantImageRepository,
    private val biometricsTemplateRepository: ParticipantBiometricsTemplateRepository,
) {

    private fun SyncEntityType.repo(): RepositoryBase<*> = when (this) {
        SyncEntityType.PARTICIPANT -> participantRepository
        SyncEntityType.IMAGE -> imageRepository
        SyncEntityType.BIOMETRICS_TEMPLATE -> biometricsTemplateRepository
        SyncEntityType.VISIT -> visitRepository
    }

    suspend fun getCount(syncEntityType: SyncEntityType): Long {
        return syncEntityType.repo().count()
    }
}