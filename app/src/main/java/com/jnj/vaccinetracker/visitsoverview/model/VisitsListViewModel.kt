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
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class VisitsListViewModel @Inject constructor(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private  val draftParticipantRepository: DraftParticipantRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
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

            val draftScheduledVisitsEncounter = draftVisitEncounterRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
            val draftVisitParticipantIds = draftScheduledVisitsEncounter.map { it.participantUuid }.toSet()
            // Remove scheduled visits with the same participant ID as in draftVisitEncounter
            val filteredScheduledVisits = scheduledVisits.filter { scheduledVisit ->
                !draftVisitParticipantIds.contains(scheduledVisit.participantUuid)
            }
            val draftScheduledVisits = draftVisitRepository.findVisitsAfterDate(getTodayMidnight())
            val convertedDraftVisits = draftScheduledVisits.map { draftVisit ->
                convertDraftVisitToVisitOffline(draftVisit)
            }
            val filteredDraftScheduledVisits = convertedDraftVisits + filteredScheduledVisits
            visitDTOs.value = createVisitDTOList(filteredDraftScheduledVisits)
            isLoading.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getHistoricalVisitsData() {
        isLoading.value = true
        viewModelScope.launch {

            val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

            if (currentLocationUuid.isNullOrEmpty()) {
                Log.e(
                    "VisitsListViewModel",
                    "Logged-in user's site UUID is null or empty. Aborting visit fetch."
                )
                isLoading.value = false
                return@launch
            } else {

                val historicalVisits =
                    visitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                        .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
                        .filter { visit -> isParticipantFromLocation(visit, currentLocationUuid) }
//
//                val historicalVisitsByLocation = visitRepository.findVisitsByLocationUuid(currentLocationUuid)
//                    .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
//





                val allHistoricalVisits = visitRepository.findAllVisits()
                if (allHistoricalVisits.isEmpty()) {
                    Log.w("VisitsListViewModel", "No historical visits found.")
                } else {
                    Log.w("VisitsListViewModel", "Historical visits: ${allHistoricalVisits.size}")
                    val occurred = allHistoricalVisits.filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
                    if (occurred.isEmpty()) {
                        Log.w("VisitsListViewModel", "No occurred visits found.")
                    } else {
                        Log.w("VisitsListViewModel", "Occurred visits: ${occurred.size}")
                        //val yesOrNo = isParticipantFromLocation( currentLocationUuid)
                    }





                }



                val draftVisits = draftVisitRepository.findAllVisits()

                if(draftVisits.isEmpty()) {
                    Log.w("VisitsListViewModel", "No draft visits found.")
                } else {
                    Log.w("VisitsListViewModel", "Draft visits: ${draftVisits.size}")
                }

                Log.d("VisitsListViewModel", "Filtered visits retrieved: ${historicalVisits.size}")

                val filteredVisits = historicalVisits.mapNotNull { visit ->
                    val draftParticipant = draftParticipantRepository.findByParticipantUuid(visit.participantUuid)

                    if (draftParticipant != null) {
                        val registrationDate = draftParticipant.registrationDate
                        val visitDate = visit.startDatetime

                        val removedVisitTime = removeTimeFromDateTime(visitDate)
                        val removedRegistrationTime = removeTimeFromDateTime(registrationDate)

                        val visitDateWithoutTime = LocalDate.parse(removedVisitTime)
                        val registrationDateWithoutTime = LocalDate.parse(removedRegistrationTime)

                        if (visitDateWithoutTime >= registrationDateWithoutTime) {
                            Log.d(
                                "VisitsListViewModel",
                                "Included visit: participantUuid=${visit.participantUuid}, visitDate=$visitDateWithoutTime, registrationDate=$registrationDateWithoutTime"
                            )
                            visit
                        } else {
                            Log.d(
                                "VisitsListViewModel",
                                "Excluded visit (visitDate < registrationDate): participantUuid=${visit.participantUuid}, visitDate=$visitDateWithoutTime, registrationDate=$registrationDateWithoutTime"
                            )
                            null
                        }
                    } else {
                        Log.w(
                            "VisitsListViewModel",
                            "Participant not found for UUID: ${visit.participantUuid}"
                        )
                        null
                    }
                }
                Log.w("VisitsListViewModel", "Filtered visits: ${filteredVisits.size}")

                val draftHistoricalVisits = draftVisitRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                val convertedDraftVisits = draftHistoricalVisits.map { convertDraftVisitToVisitOffline(it) }

                if(draftHistoricalVisits.isEmpty()) {
                    Log.w("VisitsListViewModel", "No draft visits found before the specified date.")
                } else {
                    Log.w("VisitsListViewModel", "Draft visits: ${draftHistoricalVisits.size}")
                }

                val draftHistoricalVisitsEncounter = draftVisitEncounterRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))

                if(draftHistoricalVisitsEncounter.isEmpty()) {
                    Log.w("VisitsListViewModel", "No draft visit encounters found before the specified date.")
                } else {
                    Log.w("VisitsListViewModel", "Draft visit encounters: ${draftHistoricalVisitsEncounter.size}")
                }

                val convertedDraftVisitsEncounter = draftHistoricalVisitsEncounter.map { convertDraftVisitEncounterToVisitOffline(it) }

                val filteredDraftVisitsEncounter = convertedDraftVisitsEncounter.filter {
                    it.participantUuid !in convertedDraftVisits.map { visit -> visit.participantUuid }
                }

                val combinedVisits = convertedDraftVisits + filteredDraftVisitsEncounter + filteredVisits

                logInfo("Combined ${combinedVisits.size} visits into VisitDTOs")
                visitDTOs.value = createVisitDTOList(combinedVisits)
                Log.w("VisitsListViewModel", "Filtered visits: ${combinedVisits.size}")

                isLoading.value = false
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

    private fun removeTimeFromDateTime(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(date)
        return dateStr
    }

    private suspend fun isParticipantFromLocation(visit: Visit, locationUuid: String): Boolean {
        val participant = draftParticipantRepository.findByParticipantUuid(visit.participantUuid)
        if (participant == null) {
            Log.w("VisitsListViewModel", "Participant not found for UUID: ${visit.participantUuid}")
            return false
        }  else {
            Log.d("VisitsListViewModel", "Participant found: ${participant.participantUuid}, Location UUID: ${participant.locationUuid}")
        }
        return participant.locationUuid == locationUuid
    }

}