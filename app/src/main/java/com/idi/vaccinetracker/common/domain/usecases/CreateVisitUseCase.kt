package com.idi.vaccinetracker.common.domain.usecases

import com.idi.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.idi.vaccinetracker.common.domain.entities.CreateVisit
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.domain.entities.DraftVisit
import com.idi.vaccinetracker.common.helpers.uuid
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateVisitUseCase @Inject constructor(
    private val draftVisitRepository: DraftVisitRepository,
) {
    private fun CreateVisit.toDomain(visitUuid: String) =
        DraftVisit(
            startDatetime,
            participantUuid,
            locationUuid = locationUuid,
            visitUuid = visitUuid,
            attributes = attributes,
            visitType = visitType,
            draftState = DraftState.initialState()
        )

    suspend fun createVisit(createVisit: CreateVisit): DraftVisit {
        val visitUuid = uuid()
        val draftVisit = createVisit.toDomain(visitUuid)
        draftVisitRepository.insert(draftVisit, orReplace = false)
        return draftVisit
    }
}