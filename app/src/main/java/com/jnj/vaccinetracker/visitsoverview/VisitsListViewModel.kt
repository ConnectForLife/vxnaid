package com.jnj.vaccinetracker.visitsoverview

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.model.VisitDataDTO
import kotlinx.coroutines.launch
import javax.inject.Inject

class VisitsListViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val visitDTOs = mutableLiveData<List<VisitDataDTO>>()
    val isLoading = mutableLiveData<Boolean>()

    fun getScheduledVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            val scheduledVisits = visitRepository.findVisitsAfterDate(getTodayMidnight())
                .filter { it.attributes[Constants.ATTRIBUTE_VISIT_STATUS] == Constants.VISIT_STATUS_SCHEDULED }
            visitDTOs.value = createVisitDTOList(scheduledVisits)
            isLoading.value = false
        }
    }

    fun getHistoricalVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            val scheduledVisits = visitRepository.findVisitsBeforeDate(getTodayMidnight())
                .filter { it.attributes[Constants.ATTRIBUTE_VISIT_STATUS] == Constants.VISIT_STATUS_OCCURRED }
            visitDTOs.value = createVisitDTOList(scheduledVisits)
            isLoading.value = false
        }
    }

    fun getMissedVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            val scheduledVisits = visitRepository.findVisitsBeforeDate(getTodayMidnight())
                .filter { it.attributes[Constants.ATTRIBUTE_VISIT_STATUS] == Constants.VISIT_STATUS_SCHEDULED }
            visitDTOs.value = createVisitDTOList(scheduledVisits)
            isLoading.value = false
        }
    }

    private suspend fun createVisitDTOList(visits: List<Visit>): List<VisitDataDTO> {
        val visitDataDTOList: MutableList<VisitDataDTO> = mutableListOf()
        val participantsMap = mutableMapOf<String, ParticipantBase?>()

        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        visits.forEach { visit ->
            val participant  = participantsMap[visit.participantUuid]
            if (participant != null) {
                val visitDataDTO = VisitDataDTO(
                    visitUuid = visit.visitUuid,
                    startDatetime = visit.startDatetime,
                    attributes = visit.attributes,
                    observations = visit.observations,
                    visitType = visit.visitType,
                    participant = participant
                )
                visitDataDTOList.add(visitDataDTO)
            }
        }

        return visitDataDTOList
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}