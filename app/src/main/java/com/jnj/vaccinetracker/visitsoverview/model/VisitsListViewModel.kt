package com.jnj.vaccinetracker.visitsoverview.model
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.util.DateUtil
import com.soywiz.klock.DateFormat
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
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val visitDTOs = mutableLiveData<List<VisitDataDTO>>()
    val attachedClinics = MutableLiveData<List<String>>(emptyList())
    val parentSiteName = MutableLiveData<String?>(null)
    val isLoading = mutableLiveData<Boolean>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    fun loadAttachedClinics() {
        viewModelScope.launch {
            try {
                val allSites = configurationManager.getSites()
                parentSiteName.value = allSites.find { it.uuid == currentLocationUuid }?.name
                val clinicNames = allSites
                    .filter { it.parentLocationUuid == currentLocationUuid }
                    .map { it.name }
                attachedClinics.value = clinicNames
            } catch (e: Exception) {
                Log.e("VisitsListViewModel", "Failed to load attached clinics", e)
                attachedClinics.value = emptyList()
            }
        }
    }

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
            val uniqueScheduledVisits = scheduledVisits
                .groupBy { it.participantUuid }
                .map { (_, visits) ->
                    visits
                        .sortedWith(compareByDescending<Visit> { it.startDatetime.time }.thenByDescending { it.visitType != null })
                        .first()
                }

            val draftVisits = draftVisitRepository.findVisitsAfterDate(tomorrowMidnight)
            val convertedDraftVisits = draftVisits.map { convertDraftVisitToVisitOffline(it) }

            val draftVisitParticipantIds = convertedDraftVisits.map { it.participantUuid }.toSet()
            // Also exclude visits that were administered offline (DraftVisitEncounter exists) but not yet synced
            val administeredVisitUuids = draftVisitEncounterRepository
                .findVisitsBeforeDate(tomorrowMidnight)
                .map { it.visitUuid }.toSet()
            val filteredScheduledVisits = uniqueScheduledVisits.filter { scheduledVisit ->
                !draftVisitParticipantIds.contains(scheduledVisit.participantUuid) &&
                !administeredVisitUuids.contains(scheduledVisit.visitUuid)
            }

            val combinedScheduledVisits = convertedDraftVisits + filteredScheduledVisits
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
        val visitDataDTOList = mutableListOf<VisitDataDTO>()
        val participantsMap = findParticipantByParticipantUuidUseCase
            .findByParticipantUuids(visits.map { it.participantUuid }.toSet())
            .groupBy { it.participantUuid }

        val chpServiceKeyToDisplay = ChildHealthPlusService.values()
            .associate { it.serviceKey to it.displayName }

        val chpVisits = mutableListOf<Visit>()
        val regularVisits = mutableListOf<Visit>()
        for (visit in visits) {
            val serviceKey = visit.attributes[Constants.ATTRIBUTE_VISIT_TYPE_VXNAID]
            if (serviceKey != null && serviceKey in chpServiceKeyToDisplay) {
                chpVisits.add(visit)
            } else {
                regularVisits.add(visit)
            }
        }

        // Regular visits: one row per visit, unchanged behaviour.
        for (visit in regularVisits) {
            val participant = participantsMap[visit.participantUuid]?.getOrNull(0) ?: continue
            visitDataDTOList.add(VisitDataDTO(
                visitUuid      = visit.visitUuid,
                startDatetime  = visit.startDatetime,
                attributes     = visit.attributes,
                observations   = visit.observations,
                visitType      = visit.visitType,
                participant    = participant
            ))
        }

        // CHP visits: group by participant + date so that multiple services recorded
        // on the same day appear as a single row with all service names combined.
        chpVisits
            .groupBy { visit ->
                val dateStr = DateUtil.convertDateToString(
                    visit.startDatetime, DateFormat.FORMAT_DATE.toString()
                )
                "${visit.participantUuid}_$dateStr"
            }
            .forEach { (_, group) ->
                val first = group.first()
                val participant = participantsMap[first.participantUuid]?.getOrNull(0) ?: return@forEach

                val combinedServices = group
                    .mapNotNull { chpServiceKeyToDisplay[it.attributes[Constants.ATTRIBUTE_VISIT_TYPE_VXNAID]] }
                    .distinct()
                    .joinToString(", ")

                val mergedAttributes = first.attributes.toMutableMap()
                    .also { it[Constants.ATTRIBUTE_VISIT_TYPE_VXNAID] = combinedServices }

                val mergedObservations = group.fold(mutableMapOf<String, ObservationValue>()) { acc, v ->
                    acc.putAll(v.observations)
                    acc
                }

                visitDataDTOList.add(VisitDataDTO(
                    visitUuid     = first.visitUuid,
                    startDatetime = first.startDatetime,
                    attributes    = mergedAttributes,
                    observations  = mergedObservations,
                    visitType     = first.visitType,
                    participant   = participant
                ))
            }

        visitDataDTOList.sortByDescending { it.startDatetime.time }
        Log.d("VisitsListViewModel", "Visit count: ${visitDataDTOList.size}, Unique participant count: ${participantsMap.size}")
        return visitDataDTOList
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}