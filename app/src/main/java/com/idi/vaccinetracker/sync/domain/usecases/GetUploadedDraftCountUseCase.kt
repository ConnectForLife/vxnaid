package com.idi.vaccinetracker.sync.domain.usecases

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantBiometricsTemplateRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantImageRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.idi.vaccinetracker.common.data.database.repositories.base.UpdatableDraftRepository
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.domain.entities.SyncEntityType
import javax.inject.Inject


class GetUploadedDraftCountUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftParticipantImageRepository: DraftParticipantImageRepository,
    private val draftParticipantBiometricsTemplateRepository: DraftParticipantBiometricsTemplateRepository,
) {

    private fun SyncEntityType.repo(): UpdatableDraftRepository<*> = when (this) {
        SyncEntityType.PARTICIPANT -> draftParticipantRepository
        SyncEntityType.IMAGE -> draftParticipantImageRepository
        SyncEntityType.BIOMETRICS_TEMPLATE -> draftParticipantBiometricsTemplateRepository
        SyncEntityType.VISIT -> draftVisitRepository
    }

    suspend fun getCount(syncEntityType: SyncEntityType): Long {
        return syncEntityType.repo().countByDraftState(DraftState.UPLOADED)
    }
}