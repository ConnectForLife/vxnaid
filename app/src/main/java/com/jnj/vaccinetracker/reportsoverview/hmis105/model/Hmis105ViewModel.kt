package com.jnj.vaccinetracker.reportsoverview.hmis105.model

import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
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
import javax.inject.Inject

class Hmis105ViewModel @Inject constructor(
    userRepository: UserRepository,
    private val configurationManager: ConfigurationManager,
    private val visitRepository: VisitRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val reportDTOs = mutableLiveData<List<Hmis105ReportDTO>>(emptyList())
    val isLoading = mutableLiveData<Boolean>(false)
    val currentScreen = mutableLiveData<Screen>()
    val selectedStartDate = MutableLiveData<DateTime?>(null)
    val selectedEndDate = MutableLiveData<DateTime?>(null)
    var navigationDirection = NavigationDirection.NONE
    
    private var screens = listOf<Screen>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    companion object {
        // HMIS 105 Report Vaccine Concept Names mapping
        private val HMIS105_VACCINES = mapOf(
            "BCG Vxnaid" to "CL01. BCG",
            "Hep B BD Vxnaid" to "CL02. Hep B BD",
            "PAB for Td Vxnaid" to "CL03. PAB for Td",
            "Polio 0 Vxnaid" to "CL04. Polio 0",
            "Polio 1 Vxnaid" to "CL05. Polio 1",
            "Polio 2 Vxnaid" to "CL06. Polio 2",
            "Polio 3 Vxnaid" to "CL07. Polio 3",
            "IPV 1 Vxnaid" to "CL08. IPV 1",
            "IPV 2 Vxnaid" to "CL09. IPV 2",
            "DPT-HepB-Hib 1 Vxnaid" to "CL10. DPT-HepB-Hib 1",
            "DPT-HepB-Hib 2 Vxnaid" to "CL11. DPT-HepB-Hib 2",
            "DPT-HepB-Hib 3 Vxnaid" to "CL12. DPT-HepB-Hib 3",
            "PCV 1 Vxnaid" to "CL13. PCV 1",
            "PCV 2 Vxnaid" to "CL14. PCV 2",
            "PCV 3 Vxnaid" to "CL15. PCV 3",
            "Rota 1 Vxnaid" to "CL16. Rota 1",
            "Rota 2 Vxnaid" to "CL17. Rota 2",
            "Rota 3 Vxnaid" to "CL18. Rota 3",
            "Yellow Fever Vxnaid" to "CL22. Yellow Fever",
            "Measles Rubella 1 Vxnaid" to "CL23. Measles Rubella 1 (MR1)",
            "Measles Rubella 2 Vxnaid" to "CL27. Measles Rubella 2 (MR2)"
        )
    }

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
                        // Only count confirmed visits (exclude draft visits to match SQL behavior)
                        val allVisits = visitRepository
                            .findAllVisitsByAttributeTypeAndValue(Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED)
                            .filter { visit ->
                                visit.startDatetime.time in start.time..end.time &&
                                participantFromCurrentLocation(visit, currentLocationUuid)
                            }

                        Log.d("Hmis105ViewModel", "Total visits found: ${allVisits.size}")
                        
                        val reportData = mutableListOf<Hmis105ReportDTO>()
                        
                        // Add individual vaccine doses
                        reportData.addAll(createHmis105ReportDTOList(allVisits))

                        // Add fully immunized by 1 year
                        reportData.add(createFullyImmunized1Year(allVisits))

                        // Add LLINs
                        reportData.add(createLLINSReport(allVisits))

                        // Add section heading
                        reportData.add(Hmis105ReportDTO(doses = "SECOND YEAR OF LIFE"))

                        // Add fully immunized by 2 years
                        reportData.add(createFullyImmunized2Years(allVisits))
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

    private suspend fun createFullyImmunized1Year(visits: List<Visit>): Hmis105ReportDTO {
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

    private suspend fun createFullyImmunized2Years(visits: List<Visit>): Hmis105ReportDTO {
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
                
                // Check if 17-24 months old (per SQL specification)
                if (ageInMonths in 17..24) {
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

    private suspend fun createLLINSReport(visits: List<Visit>): Hmis105ReportDTO {
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
                    // Check if received LLINs: look for observation key/concept for LLIN (UUID: 6de53ec6-bf3f-41fe-bf2e-e61447a6557a)
                    // with value "yes"
                    val hasLLINs = visit.observations.any { (key, obs) ->
                        (key.contains("6de53ec6-bf3f-41fe-bf2e-e61447a6557a") ||
                         key.contains("LLIN") ||
                         key.contains("Long-Lasting Insecticidal Net")) &&
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

    private suspend fun createHmis105ReportDTOList(visits: List<Visit>): List<Hmis105ReportDTO> {
        val reportRowsMap = mutableMapOf<String, Hmis105ReportDTO>()
        val participantsMap = mutableMapOf<String, ParticipantBase?>()

        // Cache participants
        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        val vaccinesConfig = configurationManager.getSubstancesConfig()
            .filter { it.category == Constants.VACCINES_CATEGORY_NAME }

        Log.d("Hmis105ViewModel", "Available vaccine concept names: ${vaccinesConfig.map { it.conceptName }}")

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                for ((key, _) in visit.observations) {
                    // Extract vaccine name from observation key by removing " Date" suffix
                    val extractedVaccineName = if (key.endsWith(" ${Constants.DATE_STR}")) {
                        key.split(" ${Constants.DATE_STR}")[0]
                    } else {
                        null
                    }

                    if (extractedVaccineName != null) {
                        Log.d("Hmis105ViewModel", "Found observation key: $key -> vaccine name: $extractedVaccineName")

                        // Match with config to validate it exists
                        val matchedVaccine = vaccinesConfig.find { it.conceptName == extractedVaccineName }
                        if (matchedVaccine != null) {
                            // Get HMIS label from mapping
                            val hmisLabel = HMIS105_VACCINES[extractedVaccineName]
                            if (hmisLabel != null) {
                                val ageGroup = calculateChildAgeGroup(participant.birthDate)
                                val visitLocation = visit.visitLocation

                                updateReportRow(reportRowsMap, hmisLabel, ageGroup, visitLocation)
                                Log.d("Hmis105ViewModel", "Mapped $extractedVaccineName to HMIS label: $hmisLabel")
                            } else {
                                Log.w("Hmis105ViewModel", "No HMIS mapping for vaccine: $extractedVaccineName")
                            }
                        } else {
                            Log.w("Hmis105ViewModel", "No vaccine config match for: $extractedVaccineName")
                        }
                    }
                }
            }
        }

        Log.d("Hmis105ViewModel", "Final report rows: ${reportRowsMap.keys}")
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

    override fun saveInstanceState(outState: Bundle) {
        selectedStartDate.value?.let { 
            outState.putString("selectedStartDate", it.toString())
        }
        selectedEndDate.value?.let { 
            outState.putString("selectedEndDate", it.toString())
        }
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        val startDateStr = savedInstanceState.getString("selectedStartDate")
        val endDateStr = savedInstanceState.getString("selectedEndDate")
        
        if (startDateStr != null) {
            try {
                selectedStartDate.value = DateTime.parse(startDateStr).local
            } catch (e: Exception) {
                Log.e("Hmis105ViewModel", "Failed to parse start date: $startDateStr", e)
            }
        }
        if (endDateStr != null) {
            try {
                selectedEndDate.value = DateTime.parse(endDateStr).local
            } catch (e: Exception) {
                Log.e("Hmis105ViewModel", "Failed to parse end date: $endDateStr", e)
            }
        }
    }
}

