package com.jnj.vaccinetracker.sync.domain.usecases.upload

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.models.api.request.UpdateVisitObservationDto
import com.jnj.vaccinetracker.common.data.models.api.request.VisitUpdateRequest
import com.jnj.vaccinetracker.common.data.models.api.response.AttributeDto
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.exceptions.WebCallException
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import javax.inject.Inject

class UploadDraftVisitEncounterUseCase @Inject constructor(
    private val api: VaccineTrackerSyncApiDataSource,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
) {

    private fun DraftVisitEncounter.toDto() = VisitUpdateRequest(
        visitUuid = visitUuid,
        startDatetime = startDatetime,
        locationUuid = locationUuid,
        attributes = attributes.map { AttributeDto(it.key, it.value) },
        observations = observations.filter { it.value.isNotEmpty() }.map { UpdateVisitObservationDto(it.key, it.value) },
    )

    private suspend fun updateDraftStates(uploadedDraftVisitEncounter: DraftVisitEncounter) {
        draftVisitEncounterRepository.updateDraftState(uploadedDraftVisitEncounter)
    }

    suspend fun upload(draftVisitEncounter: DraftVisitEncounter) {
        require(draftVisitEncounter.draftState.isPendingUpload()) { "VisitEncounter already uploaded!" }
        val request = draftVisitEncounter.toDto()
        try {
            api.updateVisit(request)
        } catch (ex: WebCallException) {
            if (ex.code == 404) {
                // visit was deleted on the backend — drop this pending encounter
                // rather than retrying forever
                logWarn("updateVisit 404 visit not found, dropping draft encounter ${draftVisitEncounter.visitUuid}")
                draftVisitEncounterRepository.deleteByVisitUuid(draftVisitEncounter.visitUuid)
                return
            }
            throw ex
        }
        val uploadedDraftVisitEncounter = draftVisitEncounter.copy(
            draftState = DraftState.UPLOADED)
        updateDraftStates(uploadedDraftVisitEncounter)
    }

}