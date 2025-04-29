package com.jnj.vaccinetracker.visitsoverview.model

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
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
                .filter { it.visitStatus == Constants.VISIT_STATUS_SCHEDULED }
            visitDTOs.value = createVisitDTOList(scheduledVisits)
            isLoading.value = false
        }
    }

    fun getHistoricalVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            try {
                // Retrieve all visits
                val allVisits = visitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                Log.d("VisitsListViewModel", "Total visits retrieved: ${allVisits.size}")

                // Filter visits based on visitDate >= registrationDate
                val filteredVisits = allVisits.filter { visit ->
                    val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                    if (participant != null) {
                        val registrationDate = participant.registrationDate
                        val visitDate = visit.startDatetime

                        Log.d("VisitsListViewModel", "Visit UUID: ${visit.visitUuid}, Visit Date: $visitDate, Registration Date: $registrationDate")

                        // Include visits where visitDate >= registrationDate
                        visitDate >= registrationDate
                    } else {
                        Log.w("VisitsListViewModel", "Participant not found for UUID: ${visit.participantUuid}")
                        false
                    }
                }
                Log.d("VisitsListViewModel", "Number of visits meeting criteria: ${filteredVisits.size}")
                // Convert filtered visits into DTOs
                visitDTOs.value = createVisitDTOList(filteredVisits)
            } catch (e: Exception) {
                Log.e("VisitsListViewModel", "Error fetching filtered visits data", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun getMissedVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            val missedVisits = visitRepository.findVisitsBeforeDate(getTodayMidnight())
                .filter { it.visitStatus == Constants.VISIT_STATUS_SCHEDULED }
            visitDTOs.value = createVisitDTOList(missedVisits)
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