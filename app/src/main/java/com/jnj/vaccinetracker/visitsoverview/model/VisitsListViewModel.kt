package com.jnj.vaccinetracker.visitsoverview.model
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class VisitsListViewModel @Inject constructor(
    userRepository: UserRepository,
    private val visitRepository: VisitRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val visitDTOs = mutableLiveData<List<VisitDataDTO>>()
    val isLoading = mutableLiveData<Boolean>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    fun getScheduledVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            if (currentLocationUuid.isNullOrEmpty()) {
                Log.e("VisitsListViewModel", "Logged-in user's site UUID is null or empty. Aborting visit fetch.")
                isLoading.value = false
                return@launch
            }
            val todayMidnight = getTodayMidnight()
            val tomorrowMidnight = addDaysToDate(todayMidnight, 1)

            val scheduledVisits = visitRepository.getScheduledVisits(todayMidnight, Constants.VISIT_STATUS_SCHEDULED, currentLocationUuid)

            val draftScheduledVisitsEncounter = draftVisitEncounterRepository.findVisitsAfterDate(tomorrowMidnight)
            val convertedDraftVisitsEncounter = draftScheduledVisitsEncounter.map { draftVisit ->
                convertDraftVisitEncounterToVisitOffline(draftVisit) }

            val draftScheduledVisits = draftVisitRepository.findVisitsAfterDate(tomorrowMidnight)
            val convertedDraftVisits = draftScheduledVisits.map { draftVisit ->
                convertDraftVisitToVisitOffline(draftVisit) }

            val filteredScheduleDraftVisitsEncounter = convertedDraftVisitsEncounter.filter { draftVisitEncounter ->
                draftVisitEncounter.participantUuid !in convertedDraftVisits.map { it.participantUuid } }

            // Remove scheduled visits with the same participant ID as in draftVisitEncounter
            val draftVisitParticipantIds = draftScheduledVisitsEncounter.map { it.participantUuid }.toSet()
            val filteredScheduledVisits = scheduledVisits.filter { scheduledVisit ->
                !draftVisitParticipantIds.contains(scheduledVisit.participantUuid) }

            val combinedScheduledVisits = convertedDraftVisits + filteredScheduleDraftVisitsEncounter + filteredScheduledVisits
            visitDTOs.value = createVisitDTOList(combinedScheduledVisits)
            isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getHistoricalVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            if (currentLocationUuid.isNullOrEmpty()) {
                Log.e("VisitsListViewModel", "Logged-in user's site UUID is null or empty. Aborting visit fetch.")
                isLoading.value = false
                return@launch
            }

            val todayMidnight = getTodayMidnight()
            val tomorrowMidnight = addDaysToDate(todayMidnight, 1)

            val historicalVisits = visitRepository.getVisitHistory(tomorrowMidnight, Constants.VISIT_STATUS_OCCURRED, currentLocationUuid)

            val draftHistoricalVisitsEncounter = draftVisitEncounterRepository.findVisitsBeforeDate(tomorrowMidnight)
            val convertedDraftVisitsEncounter = draftHistoricalVisitsEncounter.map { draftVisitEncounter ->
                convertDraftVisitEncounterToVisitOffline(draftVisitEncounter)}

            val combinedHistoricalVisits = convertedDraftVisitsEncounter + historicalVisits
            val filteredHistoricalVisits = combinedHistoricalVisits.filter { it.visitLocation != Constants.EMPTY_STRING_VALUE }
            Log.d("VisitsListViewModel", "Draft visits encounter ${convertedDraftVisitsEncounter.size}, Historical visits ${historicalVisits.size}, Combined visits: ${combinedHistoricalVisits.size} with filtered visits: ${filteredHistoricalVisits.size}")

            visitDTOs.value = createVisitDTOList(filteredHistoricalVisits)
            isLoading.value = false
        }
    }

    fun getMissedVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            if (currentLocationUuid.isNullOrEmpty()) {
                Log.e("VisitsListViewModel", "Logged-in user's site UUID is null or empty. Aborting visit fetch.")
                isLoading.value = false
                return@launch
            }
            val todayMidnight = getTodayMidnight()

            val missedVisits = visitRepository.getMissedVisits(todayMidnight, Constants.VISIT_STATUS_SCHEDULED, currentLocationUuid )
            val draftVisits = draftVisitRepository.findVisitsBeforeDate(todayMidnight)
            val convertedDraftVisits = draftVisits.map { draftVisit -> convertDraftVisitToVisitOffline(draftVisit) }

            val combinedMissedVisits =  missedVisits + convertedDraftVisits

            val draftScheduledVisits = draftVisitRepository.findVisitsAfterDate(todayMidnight)
            val filteredMissedVisits = combinedMissedVisits.filter { missedVisit ->
                missedVisit.participantUuid !in draftScheduledVisits.map { it.participantUuid }
            }

            visitDTOs.value = createVisitDTOList(filteredMissedVisits)
            isLoading.value = false
        }
    }

    private fun convertDraftVisitToVisitOffline(draftVisit: DraftVisit): Visit {
        return Visit(
            visitUuid = draftVisit.visitUuid,
            startDatetime = draftVisit.startDatetime,
            visitType = draftVisit.visitType,
            participantUuid = draftVisit.participantUuid,
            attributes = draftVisit.attributes,
            observations = emptyMap(),
            dateModified = Date(System.currentTimeMillis())
        )
    }

    private fun convertDraftVisitEncounterToVisitOffline(draftVisitEncounter: DraftVisitEncounter): Visit {
        return Visit(
            visitUuid = draftVisitEncounter.visitUuid,
            startDatetime = draftVisitEncounter.startDatetime,
            visitType = draftVisitEncounter.visitType,
            participantUuid = draftVisitEncounter.participantUuid,
            attributes = draftVisitEncounter.attributes,
            observations = draftVisitEncounter.observations.mapValues { entry ->
                ObservationValue(entry.value, draftVisitEncounter.startDatetime)
            },
            dateModified = Date(System.currentTimeMillis()))
    }

    private suspend fun createVisitDTOList(visits: List<Visit>): List<VisitDataDTO> {
        val visitDataDTOList: MutableList<VisitDataDTO> = mutableListOf()
        val participantsMap =
            findParticipantByParticipantUuidUseCase.findByParticipantUuids(visits.map { it.participantUuid }
                .toSet()).groupBy { it.participantUuid }

        visits.forEach { visit ->
            val participant = participantsMap[visit.participantUuid]?.getOrNull(0)
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
        Log.d("VisitsListViewModel", "Visit count: ${visitDataDTOList.size}, Unique participant count: ${participantsMap.size}")
        return visitDataDTOList
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}

    private suspend fun participantFromCurrentLocation(visit: Visit, locationUuid: String): Boolean {
        val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
        return if (participant == null) {
            Log.w("VisitsListViewModel", "Participant not found for UUID: ${visit.participantUuid}")
            false
        } else {
            Log.d("VisitsListViewModel", "Participant found: ${participant.participantUuid}, Location UUID: ${participant.locationUuid} vs Current Location UUID: $locationUuid")
            participant.locationUuid == locationUuid
        }
    }
}