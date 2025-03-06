package com.jnj.vaccinetracker.visitsoverview.model

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.Participant
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import kotlinx.coroutines.launch
import javax.inject.Inject

class VisitsListViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val participantRepository: ParticipantRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val visitDTOs = mutableLiveData<List<VisitDataDTO>>()
    private val participantDataDTOs = mutableLiveData<List<ParticipantDataDTO>>()
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
            val historicalVisits = visitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
            visitDTOs.value = createVisitDTOList(historicalVisits)
            isLoading.value = false
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

    fun fetchAllRegisteredChildren() {
        isLoading.value = true
        viewModelScope.launch {
            val registeredChildren = participantRepository.findAllByPhone(phone = null)
                .filter { it.participantStatus == Constants.VISIT_STATUS_OCCURRED }
            participantDataDTOs.value = createParticipantDTOList(registeredChildren)
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

    private suspend fun createParticipantDTOList(participants: List<Participant>): List<ParticipantDataDTO> {
        val participantDTOList: MutableList<ParticipantDataDTO> = mutableListOf()

        participants.forEach { participant ->
            val participantDTO = ParticipantDataDTO(
                participantUuid = participant.participantUuid,
                firstName = participant.childFirstName.toString(),
                lastName = participant.childLastName.toString(),
                gender = participant.gender.toString(),
                birthDate = participant.dateModified,
                homeLocation = participant.address?.let {
                    // Concatenate fields to form a full address string
                    listOfNotNull(
                        it.address1,
                        it.address2,
                        it.cityVillage,
                        it.stateProvince,
                        it.country
                    )
                        .joinToString(", ")
                },
                phone = participant.childNumber,
                attributes = participant.attributes
            )
            participantDTOList.add(participantDTO)
        }
        return participantDTOList
    }


    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}