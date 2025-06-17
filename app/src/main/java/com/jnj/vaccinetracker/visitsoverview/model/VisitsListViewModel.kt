package com.jnj.vaccinetracker.visitsoverview.model
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class VisitsListViewModel @Inject constructor(
    userRepository: UserRepository,
    private val visitRepository: VisitRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private  val draftParticipantRepository: DraftParticipantRepository,
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
            val scheduledVisits = visitRepository.findVisitsAfterDate(getTodayMidnight())
                .filter { it.visitStatus == Constants.VISIT_STATUS_SCHEDULED }

            val draftScheduledVisitsEncounter = draftVisitEncounterRepository.findVisitsAfterDate(addDaysToDate(getTodayMidnight(), 1))
            val convertedDraftVisitsEncounter = draftScheduledVisitsEncounter.map { draftVisit ->
                convertDraftVisitEncounterToVisitOffline(draftVisit) }

            val draftScheduledVisits = draftVisitRepository.findVisitsAfterDate(addDaysToDate(getTodayMidnight(), 1))
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
            val historicalVisits = visitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
            Log.d("VisitsListViewModel", "Total visits retrieved: ${historicalVisits.size}")

//            Filter historical visits by registration date
//            val filteredVisits = filterVisitsByRegistrationDate(historicalVisits)
//            Log.d("VisitsListViewModel", "Filtered visits: ${filteredVisits.size}")

            val draftHistoricalVisits = draftVisitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
            val convertedDraftVisits = draftHistoricalVisits.map { draftVisit ->
                convertDraftVisitToVisitOffline(draftVisit)
            }

            val draftHistoricalVisitsEncounter = draftVisitEncounterRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
            val convertedDraftVisitsEncounter = draftHistoricalVisitsEncounter.map { draftVisitEncounter ->
                convertDraftVisitEncounterToVisitOffline(draftVisitEncounter)
            }

            val filteredDraftVisitsEncounter = convertedDraftVisitsEncounter.filter { draftVisitEncounter ->
                draftVisitEncounter.participantUuid !in convertedDraftVisits.map { it.participantUuid }
            }

            val combinedVisits = convertedDraftVisits + filteredDraftVisitsEncounter + historicalVisits
            Log.d("VisitsListViewModel", "Draft visit ${convertedDraftVisits.size}, Draft visits encounter ${filteredDraftVisitsEncounter.size}, Historical visits ${historicalVisits.size}, Combined visits: ${combinedVisits.size}")

            visitDTOs.value = createVisitDTOList(combinedVisits)
            isLoading.value = false
        }
    }

    fun getMissedVisitsData() {
        isLoading.value = true
        viewModelScope.launch {
            val missedVisits = visitRepository.findVisitsBeforeDate(getTodayMidnight())
                .filter { it.visitStatus == Constants.VISIT_STATUS_SCHEDULED }

            val draftVisits = draftVisitRepository.findVisitsBeforeDate(getTodayMidnight())
            val convertedDraftVisits = draftVisits.map { draftVisit ->
                convertDraftVisitToVisitOffline(draftVisit)
            }

            val draftVisitsEncounter = draftVisitEncounterRepository.findVisitsBeforeDate(getTodayMidnight())
            val convertedDraftVisitsEncounter = draftVisitsEncounter.map { draftVisitEncounter ->
                convertDraftVisitEncounterToVisitOffline(draftVisitEncounter)
            }

            val combinedMissedVisits = convertedDraftVisits + convertedDraftVisitsEncounter + missedVisits

            val draftScheduledVisits = draftVisitRepository.findVisitsAfterDate(getTodayMidnight())
            val filteredMissedVisits = combinedMissedVisits.filter { missedVisit ->
                missedVisit.participantUuid !in draftScheduledVisits.map { it.participantUuid }
            }

            visitDTOs.value = createVisitDTOList(filteredMissedVisits)
            isLoading.value = false
        }
    }

    // Registration date filter function
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun filterVisitsByRegistrationDate(visits: List<Visit>): List<Visit> {
        return visits.mapNotNull { visit ->
            val draftParticipant = draftParticipantRepository.findByParticipantUuid(visit.participantUuid)
            if (draftParticipant != null) {
                val registrationDate = draftParticipant.registrationDate
                val visitDate = visit.startDatetime

                // remove time from date
                val removedVisitTime = removeTimeFromDateTime(visitDate)
                val removedRegistrationTime = removeTimeFromDateTime(registrationDate)

                val visitDateWithoutTime: LocalDate = LocalDate.parse(removedVisitTime)
                val registrationDateWithoutTime: LocalDate = LocalDate.parse(removedRegistrationTime)

                if (visitDateWithoutTime >= registrationDateWithoutTime) {
                    Log.d("VisitsListViewModel", "Included visit: participantUuid=${visit.participantUuid}, visitDate=$visitDateWithoutTime, registrationDate=$registrationDateWithoutTime")
                    visit
                } else {
                    Log.d("VisitsListViewModel", "Excluded visit (visitDate < registrationDate): participantUuid=${visit.participantUuid}, visitDate=$visitDateWithoutTime, registrationDate=$registrationDateWithoutTime")
                    null
                }
            } else {
                Log.d("VisitsListViewModel", "Participant not found for UUID: ${visit.participantUuid}")
                null
            }
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

    private fun removeTimeFromDateTime(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        return dateStr
    }

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