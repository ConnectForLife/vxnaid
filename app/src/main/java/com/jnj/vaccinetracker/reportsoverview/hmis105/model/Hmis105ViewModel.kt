package com.jnj.vaccinetracker.reportsoverview.hmis105.model

import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ReportDTO
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class Hmis105ViewModel @Inject constructor(
    userRepository: UserRepository,
    private val configurationManager: ConfigurationManager,
    private val visitRepository: VisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val reportDTOs = mutableLiveData<List<Hmis105ReportDTO>>(emptyList())
    val isLoading = mutableLiveData<Boolean>(false)
    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    
    private var screens = listOf<Screen>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    init {
        initScreens()
    }

    fun getHmisMalaria105Data(startDate: DateTime?, endDate: DateTime?) {
        Log.d("Hmis105ViewModel", "getHmisMalaria105Data called")
        isLoading.value = true
        viewModelScope.launch {
            try {
                val start = startDate?.toDate() ?: getTodayMidnight()
                val end = endDate?.toDate() ?: addDaysToDate(getTodayMidnight(), 1)
                
                Log.d("Hmis105ViewModel", "Loading report data from $start to $end")
                
                // Load data on IO dispatcher
                val data = withContext(dispatchers.io) {
                    try {
                        val occurredVisits = visitRepository
                            .findAllVisitsByAttributeTypeAndValue(Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED)
                            .filter { visit ->
                                visit.startDatetime.time in start.time..end.time &&
                                participantFromCurrentLocation(visit, currentLocationUuid)
                            }

                        val draftVisitEncounters = draftVisitEncounterRepository.findVisitsBeforeDate(end)
                            .filter { it.startDatetime.time >= start.time }
                        val draftVisitEncountersAsVisits = draftVisitEncounters.map { convertDraftVisitEncounterToVisit(it) }

                        val allVisits = occurredVisits + draftVisitEncountersAsVisits
                        Log.d("Hmis105ViewModel", "Total visits found: ${allVisits.size}")
                        
                        val reportData = mutableListOf<Hmis105ReportDTO>()
                        
                        // Add individual vaccine doses
                        reportData.addAll(createHmis105ReportDTOList(allVisits, start, end))
                        
                        // Add fully immunized by 1 year
                        reportData.add(createFullyImmunized1Year(allVisits, start, end))
                        
                        // Add LLINs
                        reportData.add(createLLINSReport(allVisits, start, end))
                        
                        // Add section heading
                        reportData.add(Hmis105ReportDTO(doses = "SECOND YEAR OF LIFE"))

                        // Add fully immunized by 2 years
                        reportData.add(createFullyImmunized2Years(allVisits, start, end))
                        
                        reportData
                    } catch (ex: Exception) {
                        Log.e("Hmis105ViewModel", "Repository error: ${ex.message}", ex)
                        emptyList()
                    }
                }

                Log.d("Hmis105ViewModel", "Report data loaded: ${data.size} rows")
                reportDTOs.value = data
                isLoading.value = false
            } catch (ex: Exception) {
                Log.e("Hmis105ViewModel", "Unexpected error loading report data", ex)
                reportDTOs.value = emptyList()
                isLoading.value = false
            }
        }
    }

    private suspend fun createFullyImmunized1Year(visits: List<Visit>, start: Date, end: Date): Hmis105ReportDTO {
        val participantsMap = mutableMapOf<String, ParticipantBase?>()
        
        // Cache participants
        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        var under1Static = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                val ageInMonths = calculateAgeInMonths(participant.birthDate)
                
                // Check if 8-12 months old
                if (ageInMonths in 8..12) {
                    // Check if has Yellow Fever AND MR1
                    val hasYellowFever = visit.observations.keys.any { 
                        it.contains("CL22") || it.contains("Yellow Fever")
                    }
                    val hasMR1 = visit.observations.keys.any { 
                        it.contains("CL23") || it.contains("Measles Rubella 1")
                    }
                    
                    if (hasYellowFever && hasMR1) {
                        val visitLocation = visit.visitLocation
                        if (visitLocation == Constants.VISIT_PLACE_STATIC) {
                            under1Static++
                        } else if (visitLocation == Constants.VISIT_PLACE_OUTREACH || visitLocation == Constants.VISIT_PLACE_SCHOOL) {
                            under1Outreach++
                        }
                    }
                }
            }
        }

        return Hmis105ReportDTO(
            doses = "CL24. Fully immunized by 1 year",
            under1Static = under1Static,
            under1Outreach = under1Outreach,
            total = under1Static + under1Outreach
        )
    }

    private suspend fun createFullyImmunized2Years(visits: List<Visit>, start: Date, end: Date): Hmis105ReportDTO {
        val participantsMap = mutableMapOf<String, ParticipantBase?>()
        
        // Cache participants
        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        var age1to4Static = 0
        var age1to4Outreach = 0

        // Track distinct people who received MR2
        val peopleMR2 = mutableSetOf<String>()

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                val ageInMonths = calculateAgeInMonths(participant.birthDate)
                
                // Check if 17-24 months old
                if (ageInMonths in 15..24) {
                    // Check if received MR2 - UUID: b3e01696-40ca-4b08-8448-d64bef8be88d
                    val hasMR2 = visit.observations.keys.any { obsKey ->
                        // Check both by concept UUID and common naming patterns
                        obsKey.contains("b3e01696-40ca-4b08-8448-d64bef8be88d") ||
                        obsKey.contains("MR2") ||
                        obsKey.contains("Measles Rubella 2") ||
                        obsKey.contains("CL27")
                    }
                    
                    if (hasMR2) {
                        peopleMR2.add(visit.participantUuid)
                        val visitLocation = visit.visitLocation
                        when {
                            visitLocation == Constants.VISIT_PLACE_STATIC -> age1to4Static++
                            visitLocation == Constants.VISIT_PLACE_OUTREACH || visitLocation == Constants.VISIT_PLACE_SCHOOL -> age1to4Outreach++
                        }
                    }
                }
            }
        }

        return Hmis105ReportDTO(
            doses = "CL28. Fully immunized by 2 years",
            age1to4Static = age1to4Static,
            age1to4Outreach = age1to4Outreach,
            total = age1to4Static + age1to4Outreach
        )
    }

    private suspend fun createLLINSReport(visits: List<Visit>, start: Date, end: Date): Hmis105ReportDTO {
        val participantsMap = mutableMapOf<String, ParticipantBase?>()
        
        // Cache participants
        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        var under1Static = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                val ageInMonths = calculateAgeInMonths(participant.birthDate)
                
                // Check if under 1 year
                if (ageInMonths < 12) {
                    // Check if received LLINs (has LLIN observation with value "yes")
                    val hasLLINs = visit.observations.values.any { obs ->
                        obs.value.equals("yes", ignoreCase = true)
                    }
                    
                    if (hasLLINs) {
                        val visitLocation = visit.visitLocation
                        if (visitLocation == Constants.VISIT_PLACE_STATIC) {
                            under1Static++
                        } else if (visitLocation == Constants.VISIT_PLACE_OUTREACH || visitLocation == Constants.VISIT_PLACE_SCHOOL) {
                            under1Outreach++
                        }
                    }
                }
            }
        }

        return Hmis105ReportDTO(
            doses = "CL25. No. received LLINs",
            under1Static = under1Static,
            under1Outreach = under1Outreach,
            total = under1Static + under1Outreach
        )
    }

    private fun calculateAgeInMonths(birthDate: BirthDate): Int {
        val currentDate = DateTime.now()
        val birthDateTime = birthDate.toDateTime()
        return (currentDate.yearInt - birthDateTime.yearInt) * 12 + (currentDate.month0 - birthDateTime.month0)
    }

    private suspend fun createHmis105ReportDTOList(visits: List<Visit>, start: Date, end: Date): List<Hmis105ReportDTO> {
        val reportRowsMap = mutableMapOf<String, Hmis105ReportDTO>()
        val participantsMap = mutableMapOf<String, ParticipantBase?>()

        // Cache participants
        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        val vaccineConceptNames = configurationManager.getSubstancesConfig()
            .filter { it.category == Constants.VACCINES_CATEGORY_NAME }
            .map { it.conceptName }

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                for ((key, _) in visit.observations) {
                    val vaccineConceptName = vaccineConceptNames.find { key == "$it ${Constants.DATE_STR}" }
                    if (vaccineConceptName != null) {
                        val ageGroup = calculateChildAgeGroup(participant.birthDate)
                        val visitLocation = visit.visitLocation

                        updateReportRow(reportRowsMap, vaccineConceptName, ageGroup, visitLocation)
                    }
                }
            }
        }

        return reportRowsMap.values.toList().sortedBy { it.doses }
    }

    private fun updateReportRow(
        reportRows: MutableMap<String, Hmis105ReportDTO>,
        vaccine: String,
        ageGroup: String,
        deliveryMode: String?
    ) {
        val currentRow = reportRows[vaccine] ?: Hmis105ReportDTO(doses = vaccine)
        val mode = deliveryMode ?: Constants.VISIT_PLACE_STATIC

        val updatedRow = when {
            ageGroup == Constants.GROUP_AGE_FIRST && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(under1Static = currentRow.under1Static + 1)
            ageGroup == Constants.GROUP_AGE_FIRST && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(under1Outreach = currentRow.under1Outreach + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age1to4Static = currentRow.age1to4Static + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age1to4Outreach = currentRow.age1to4Outreach + 1)
            ageGroup == Constants.GROUP_AGE_THIRD && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age5to14Static = currentRow.age5to14Static + 1)
            ageGroup == Constants.GROUP_AGE_THIRD && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age5to14Outreach = currentRow.age5to14Outreach + 1)
            else -> currentRow
        }

        val total = updatedRow.under1Static + updatedRow.under1Outreach + updatedRow.age1to4Static +
                   updatedRow.age1to4Outreach + updatedRow.age5to14Static + updatedRow.age5to14Outreach

        reportRows[vaccine] = updatedRow.copy(total = total)
    }

    private fun calculateChildAgeGroup(birthDate: BirthDate): String {
        val currentDate = DateTime.now()
        val birthDateTime = birthDate.toDateTime()

        val ageInMonths = (currentDate.yearInt - birthDateTime.yearInt) * 12 + (currentDate.month0 - birthDateTime.month0)
        val ageInYears = ageInMonths / 12

        return when {
            ageInMonths < 12 -> Constants.GROUP_AGE_FIRST
            ageInMonths < 60 -> Constants.GROUP_AGE_SECOND
            ageInYears <= 14 -> Constants.GROUP_AGE_THIRD
            else -> Constants.GROUP_AGE_FOURTH
        }
    }

    private fun convertDraftVisitEncounterToVisit(draftVisitEncounter: DraftVisitEncounter): Visit {
        return Visit(
            visitUuid = draftVisitEncounter.visitUuid,
            startDatetime = draftVisitEncounter.startDatetime,
            visitType = draftVisitEncounter.visitType,
            participantUuid = draftVisitEncounter.participantUuid,
            attributes = draftVisitEncounter.attributes,
            observations = draftVisitEncounter.observations.mapValues { entry ->
                ObservationValue(entry.value, draftVisitEncounter.startDatetime)
            },
            dateModified = Date(System.currentTimeMillis())
        )
    }

    private suspend fun participantFromCurrentLocation(visit: Visit, locationUuid: String?): Boolean {
        val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
        return if (participant == null) {
            false
        } else {
            participant.locationUuid == locationUuid
        }
    }

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.HMIS105_REPORT)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            val screen = screens.firstOrNull()
            currentScreen.set(screen)
        }
    }

    enum class Screen(@StringRes val label: Int) {
        HMIS105_REPORT(R.string.hmis105_report_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}

