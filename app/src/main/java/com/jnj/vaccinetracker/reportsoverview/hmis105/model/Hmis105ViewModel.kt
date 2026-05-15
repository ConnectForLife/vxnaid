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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class Hmis105ViewModel @Inject constructor(
    userRepository: UserRepository,
    private val visitRepository: VisitRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val reportDTOs = mutableLiveData<List<Hmis105ReportDTO>>(emptyList())
    val isLoading = mutableLiveData<Boolean>(false)
    val currentScreen = mutableLiveData<Screen>()
    val selectedStartDate = MutableLiveData<DateTime?>(null)
    val selectedEndDate = MutableLiveData<DateTime?>(null)
    val attachedClinics = MutableLiveData<List<String>>(emptyList())
    val parentSiteName = MutableLiveData<String?>(null)
    var navigationDirection = NavigationDirection.NONE

    private var screens = listOf<Screen>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    companion object {
        const val PARENT_CLINIC_FILTER = "__PARENT__"

        private val HMIS105_VACCINES = mapOf(
            "BCG Vxnaid Date"                      to "CL01. BCG",
            "Hep B BD Vxnaid Date"                 to "CL02. Hep B BD",
            "Polio 0 Vxnaid Date"                  to "CL04. Polio 0",
            "Polio 1 Vxnaid Date"                  to "CL05. Polio 1",
            "Polio 2 Vxnaid Date"                  to "CL06. Polio 2",
            "Polio 3 Vxnaid Date"                  to "CL07. Polio 3",
            "IPV 1 Vxnaid Date"                    to "CL08. IPV 1",
            "IPV 2 Vxnaid Date"                    to "CL09. IPV 2",
            "DPT-HepB-Hib 1 Vxnaid Date"          to "CL10. DPT-HepB-Hib 1",
            "DPT-HepB-Hib 2 Vxnaid Date"          to "CL11. DPT-HepB-Hib 2",
            "DPT-HepB-Hib 3 Vxnaid Date"          to "CL12. DPT-HepB-Hib 3",
            "PCV 1 Vxnaid Date"                    to "CL13. PCV 1",
            "PCV 2 Vxnaid Date"                    to "CL14. PCV 2",
            "PCV 3 Vxnaid Date"                    to "CL15. PCV 3",
            "Rota 1 Vxnaid Date"                   to "CL16. Rota 1",
            "Rota 2 Vxnaid Date"                   to "CL17. Rota 2",
            "Rota 3 Vxnaid Date"                   to "CL18. Rota 3",
            "Yellow Fever Vxnaid Date"             to "CL22. Yellow Fever",
            "Measles Rubella 1 (MR1) Vxnaid Date" to "CL23. Measles Rubella 1 (MR1)"
        )

        private const val KEY_MR1          = "Measles Rubella 1 (MR1) Vxnaid Date"
        private const val KEY_MR2          = "Measles Rubella 2 (MR2) Vxnaid Date"
        private const val KEY_YELLOW_FEVER = "Yellow Fever Vxnaid Date"
        private const val UUID_LLINS = "6de53ec6-bf3f-41fe-bf2e-e61447a6557a"
        private const val UUID_PAB = "b8ca722b-9731-4e50-8081-ac9131230718"
        private val REQUIRED_VACCINES_FIRST_YEAR: Set<String> = HMIS105_VACCINES.keys
        private val REQUIRED_VACCINES_SECOND_YEAR: Set<String> = HMIS105_VACCINES.keys + KEY_MR2
    }

    init {
        initScreens()
    }

    fun loadAttachedClinics() {
        viewModelScope.launch {
            try {
                val allSites = configurationManager.getSites()
                parentSiteName.value = allSites.find { it.uuid == currentLocationUuid }?.name
                attachedClinics.value = allSites
                    .filter { it.parentLocationUuid == currentLocationUuid }
                    .map { it.name }
            } catch (e: Exception) {
                Log.e("Hmis105ViewModel", "Failed to load attached clinics", e)
                attachedClinics.value = emptyList()
            }
        }
    }

    fun getHMIS105Data(startDate: DateTime?, endDate: DateTime?, selectedClinic: String? = null) {
        Log.d("Hmis105ViewModel", "getHMIS105Data called")
        isLoading.value = true
        viewModelScope.launch {
            try {
                val start = startDate?.toDate() ?: getTodayMidnight()
                val end   = endDate?.toDate()   ?: addDaysToDate(getTodayMidnight(), 1)

                Log.d("Hmis105ViewModel", "Loading report data from $start to $end")

                val data = withContext(dispatchers.io) {
                    try {
                        val occurredVisits = visitRepository
                            .findAllVisitsByAttributeTypeAndValue(
                                Constants.ATTRIBUTE_VISIT_STATUS,
                                Constants.VISIT_STATUS_OCCURRED
                            )
                        val participantUuids = occurredVisits.mapTo(mutableSetOf()) { it.participantUuid }
                        val participantsMap = participantUuids
                            .map { uuid -> async { uuid to findParticipantByParticipantUuidUseCase.findByParticipantUuid(uuid) } }
                            .awaitAll()
                            .toMap()
                        val candidateVisits = occurredVisits.filter { visit ->
                            participantsMap[visit.participantUuid]?.locationUuid == currentLocationUuid
                        }.filter { visit ->
                            when (selectedClinic) {
                                null -> true
                                PARENT_CLINIC_FILTER -> visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC].isNullOrBlank()
                                else -> visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC]
                                    ?.equals(selectedClinic, ignoreCase = true) == true
                            }
                        }
                        val allVisits = candidateVisits.filter { visit ->
                            visit.observations.values.any { obsValue ->
                                obsValue.dateTime.time in start.time until end.time
                            }
                        }

                        Log.d("Hmis105ViewModel", "Total visits found: ${allVisits.size}")

                        val allKeys = allVisits.flatMap { it.observations.keys }.toSet()
                        Log.d("Hmis105ViewModel", "All obs keys in filtered visits: $allKeys")

                        val allObsKeysByParticipant: Map<String, Set<String>> = candidateVisits
                            .groupBy { it.participantUuid }
                            .mapValues { (_, visits) -> visits.flatMap { it.observations.keys }.toSet() }

                        val reportData = mutableListOf<Hmis105ReportDTO>()

                        reportData.addAll(createHmis105ReportDTOList(allVisits, participantsMap, start, end))
                        reportData.add(createPABReport(allVisits, participantsMap, start, end))
                        reportData.add(createFullyImmunized1Year(allVisits, allObsKeysByParticipant, participantsMap, start, end))
                        reportData.add(createLLINSReport(allVisits, participantsMap, start, end))
                        reportData.add(Hmis105ReportDTO(doses = "SECOND YEAR OF LIFE"))
                        reportData.add(createMR2Report(allVisits, participantsMap, start, end))
                        reportData.add(createFullyImmunized2Years(allVisits, allObsKeysByParticipant, participantsMap, start, end))

                        reportData.sortWith { a, b ->
                            val aOrder = getSortOrder(a.doses)
                            val bOrder = getSortOrder(b.doses)
                            when {
                                aOrder.first != bOrder.first -> aOrder.first.compareTo(bOrder.first)
                                else -> aOrder.second.compareTo(bOrder.second)
                            }
                        }
                        
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

    private fun getSortOrder(doses: String): Pair<Int, Int> {
        return when {
            doses == "SECOND YEAR OF LIFE" -> Pair(3, 0)
            else -> {
                val clMatch = Regex("CL(\\d+)").find(doses)
                val clNumber = clMatch?.groupValues?.get(1)?.toIntOrNull()
                when {
                    clNumber != null && clNumber in 1..23 -> Pair(0, clNumber)
                    clNumber == 24 -> Pair(1, 24)
                    clNumber == 25 -> Pair(2, 25)
                    clNumber == 27 -> Pair(4, 27)
                    clNumber == 28 -> Pair(5, 28)
                    else -> Pair(6, 999)
                }
            }
        }
    }

    private fun calculateAgeInMonthsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        val birth = birthDate.toDateTime()
        return (referenceDate.yearInt - birth.yearInt) * 12 +
                (referenceDate.month0 - birth.month0)
    }

    private fun calculateAgeInYearsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        return calculateAgeInMonthsAt(birthDate, referenceDate) / 12
    }

    private fun calculateChildAgeGroupAt(birthDate: BirthDate, visitDate: DateTime): String {
        val ageInMonths = calculateAgeInMonthsAt(birthDate, visitDate)
        val ageInYears  = calculateAgeInYearsAt(birthDate, visitDate)
        return when {
            ageInMonths in 0..11  -> Constants.GROUP_AGE_FIRST
            ageInMonths in 12..59 -> Constants.GROUP_AGE_SECOND
            ageInYears  in 5..14  -> Constants.GROUP_AGE_THIRD
            else                  -> Constants.GROUP_AGE_FOURTH
        }
    }

    private fun createHmis105ReportDTOList(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): List<Hmis105ReportDTO> {

        val reportRowsMap = HMIS105_VACCINES.values
            .associateWithTo(mutableMapOf()) { label -> Hmis105ReportDTO(doses = label) }
        val nowDateTime = DateTime.now()
        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue

            for ((key, obsValue) in visit.observations) {
                if (obsValue.dateTime.time !in startDate.time until endDate.time) {
                    continue
                }

                val hmisLabel = HMIS105_VACCINES[key] ?: continue
                val ageGroup      = calculateChildAgeGroupAt(participant.birthDate, nowDateTime)
                val visitLocation = visit.visitLocation

                updateReportRow(reportRowsMap, hmisLabel, ageGroup, visitLocation)
                Log.d("Hmis105ViewModel", "Mapped [$key] → [$hmisLabel] | age: $ageGroup | loc: $visitLocation")
            }
        }
        return reportRowsMap.values.toList().sortedBy { it.doses }
    }
    private fun createFullyImmunized1Year(
        periodVisits: List<Visit>,
        allObsKeysByParticipant: Map<String, Set<String>>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): Hmis105ReportDTO {

        var under1Static   = 0
        var under1Outreach = 0
        val counted = mutableSetOf<String>()

        for (visit in periodVisits) {
            val participantUuid = visit.participantUuid
            if (participantUuid in counted) continue

            val mr1Obs = visit.observations[KEY_MR1]?.takeIf {
                it.dateTime.time in startDate.time until endDate.time
            } ?: continue

            val participant = participantsMap[participantUuid] ?: continue

            val ageAtMR1 = calculateAgeInMonthsAt(participant.birthDate, DateTime(mr1Obs.dateTime.time))
            if (ageAtMR1 !in 8..12) {
                Log.d("Hmis105ViewModel", "CL24 skip $participantUuid — age at MR1 was $ageAtMR1 months")
                continue
            }

            val receivedKeys = allObsKeysByParticipant[participantUuid] ?: emptySet()
            val missingVaccines = REQUIRED_VACCINES_FIRST_YEAR - receivedKeys
            if (missingVaccines.isNotEmpty()) {
                Log.d("Hmis105ViewModel", "CL24 skip $participantUuid — missing: $missingVaccines")
                continue
            }

            counted.add(participantUuid)
            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC   -> under1Static++
                Constants.VISIT_PLACE_OUTREACH,
                Constants.VISIT_PLACE_SCHOOL   -> under1Outreach++
                else                           -> under1Static++
            }
        }

        Log.d("Hmis105ViewModel", "CL24 Final — Static: $under1Static, Outreach: $under1Outreach")
        return Hmis105ReportDTO(
            doses          = "CL24. Fully immunized by 1 year",
            under1Static   = under1Static,
            under1Outreach = under1Outreach,
            total          = under1Static + under1Outreach
        )
    }

    private fun createLLINSReport(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): Hmis105ReportDTO {

        var under1Static   = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue
            val llinsEntry = visit.observations.entries.firstOrNull { (key, obs) ->
                (key.contains(UUID_LLINS, ignoreCase = true) ||
                        key.contains("LLIN", ignoreCase = true)) &&
                        obs.value.trim().equals("yes", ignoreCase = true) &&
                        obs.dateTime.time in startDate.time until endDate.time
            } ?: continue

            val obsDateTime = DateTime(llinsEntry.value.dateTime.time)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, obsDateTime)
            if (ageInMonths !in 0..11) continue

            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC   -> under1Static++
                Constants.VISIT_PLACE_OUTREACH,
                Constants.VISIT_PLACE_SCHOOL   -> under1Outreach++
            }
        }

        Log.d("Hmis105ViewModel", "CL25 LLINs — Static: $under1Static, Outreach: $under1Outreach")
        return Hmis105ReportDTO(
            doses          = "CL25. No. received LLINs",
            under1Static   = under1Static,
            under1Outreach = under1Outreach,
            total          = under1Static + under1Outreach
        )
    }

    private fun createPABReport(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): Hmis105ReportDTO {

        var under1Static   = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue

            val pabEntry = visit.observations.entries.firstOrNull { (key, obs) ->
                (key.contains(UUID_PAB, ignoreCase = true) ||
                        key.contains("PAB", ignoreCase = true)) &&
                        obs.value.trim().isNotEmpty() &&
                        obs.dateTime.time in startDate.time until endDate.time
            } ?: continue

            val obsDateTime = DateTime(pabEntry.value.dateTime.time)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, obsDateTime)
            if (ageInMonths !in 0..11) continue

            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC   -> under1Static++
                Constants.VISIT_PLACE_OUTREACH,
                Constants.VISIT_PLACE_SCHOOL   -> under1Outreach++
            }
        }

        Log.d("Hmis105ViewModel", "CL03 PAB for Td — Static: $under1Static, Outreach: $under1Outreach")
        return Hmis105ReportDTO(
            doses          = "CL03. PAB for Td",
            under1Static   = under1Static,
            under1Outreach = under1Outreach,
            total          = under1Static + under1Outreach
        )
    }

    private fun createMR2Report(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): Hmis105ReportDTO {

        var age1to4Static    = 0
        var age1to4Outreach  = 0
        var age5to14Static   = 0
        var age5to14Outreach = 0

        for (visit in visits) {
            val mr2Obs = visit.observations[KEY_MR2]?.takeIf {
                it.dateTime.time in startDate.time until endDate.time
            } ?: continue

            val participant   = participantsMap[visit.participantUuid] ?: continue
            val mr2DateTime = DateTime(mr2Obs.dateTime.time)
            val ageGroup      = calculateChildAgeGroupAt(participant.birthDate, mr2DateTime)
            val visitLocation = visit.visitLocation

            when {
                ageGroup == Constants.GROUP_AGE_SECOND &&
                        visitLocation == Constants.VISIT_PLACE_STATIC                          -> age1to4Static++

                ageGroup == Constants.GROUP_AGE_SECOND &&
                        (visitLocation == Constants.VISIT_PLACE_OUTREACH ||
                                visitLocation == Constants.VISIT_PLACE_SCHOOL)                        -> age1to4Outreach++

                ageGroup == Constants.GROUP_AGE_THIRD &&
                        visitLocation == Constants.VISIT_PLACE_STATIC                          -> age5to14Static++

                ageGroup == Constants.GROUP_AGE_THIRD &&
                        (visitLocation == Constants.VISIT_PLACE_OUTREACH ||
                                visitLocation == Constants.VISIT_PLACE_SCHOOL)                        -> age5to14Outreach++
            }
        }

        val total = age1to4Static + age1to4Outreach + age5to14Static + age5to14Outreach
        Log.d("Hmis105ViewModel", "CL27 MR2 — 1-4 Static: $age1to4Static, 1-4 Outreach: $age1to4Outreach, " +
                "5-14 Static: $age5to14Static, 5-14 Outreach: $age5to14Outreach, Total: $total")

        return Hmis105ReportDTO(
            doses            = "CL27. Measles Rubella 2 (MR2)",
            age1to4Static    = age1to4Static,
            age1to4Outreach  = age1to4Outreach,
            age5to14Static   = age5to14Static,
            age5to14Outreach = age5to14Outreach,
            total            = total
        )
    }

    private fun createFullyImmunized2Years(
        periodVisits: List<Visit>,
        allObsKeysByParticipant: Map<String, Set<String>>,
        participantsMap: Map<String, ParticipantBase?>,
        startDate: Date,
        endDate: Date
    ): Hmis105ReportDTO {

        var age1to4Static   = 0
        var age1to4Outreach = 0
        val counted = mutableSetOf<String>()

        for (visit in periodVisits) {
            val participantUuid = visit.participantUuid
            if (participantUuid in counted) continue

            // The visit must include MR2 administered within the reporting period.
            val mr2Obs = visit.observations[KEY_MR2]?.takeIf {
                it.dateTime.time in startDate.time until endDate.time
            } ?: continue

            val participant = participantsMap[participantUuid] ?: continue

            val ageAtMR2 = calculateAgeInMonthsAt(participant.birthDate, DateTime(mr2Obs.dateTime.time))
            if (ageAtMR2 !in 17..24) {
                Log.d("Hmis105ViewModel", "CL28 skip $participantUuid — age at MR2 was $ageAtMR2 months")
                continue
            }

            val receivedKeys = allObsKeysByParticipant[participantUuid] ?: emptySet()
            val missingVaccines = REQUIRED_VACCINES_SECOND_YEAR - receivedKeys
            if (missingVaccines.isNotEmpty()) {
                Log.d("Hmis105ViewModel", "CL28 skip $participantUuid — missing: $missingVaccines")
                continue
            }

            counted.add(participantUuid)
            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC   -> age1to4Static++
                Constants.VISIT_PLACE_OUTREACH,
                Constants.VISIT_PLACE_SCHOOL   -> age1to4Outreach++
                else                           -> age1to4Static++
            }
        }

        Log.d("Hmis105ViewModel", "CL28 Final — Static: $age1to4Static, Outreach: $age1to4Outreach")
        return Hmis105ReportDTO(
            doses           = "CL28. Fully immunized by 2 years",
            age1to4Static   = age1to4Static,
            age1to4Outreach = age1to4Outreach,
            total           = age1to4Static + age1to4Outreach
        )
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
            ageGroup == Constants.GROUP_AGE_FIRST  && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(under1Static = currentRow.under1Static + 1)

            ageGroup == Constants.GROUP_AGE_FIRST  &&
                    (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(under1Outreach = currentRow.under1Outreach + 1)

            ageGroup == Constants.GROUP_AGE_SECOND && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age1to4Static = currentRow.age1to4Static + 1)

            ageGroup == Constants.GROUP_AGE_SECOND &&
                    (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age1to4Outreach = currentRow.age1to4Outreach + 1)

            ageGroup == Constants.GROUP_AGE_THIRD  && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age5to14Static = currentRow.age5to14Static + 1)

            ageGroup == Constants.GROUP_AGE_THIRD  &&
                    (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age5to14Outreach = currentRow.age5to14Outreach + 1)

            else -> currentRow
        }

        val total = updatedRow.under1Static   + updatedRow.under1Outreach  +
                updatedRow.age1to4Static  + updatedRow.age1to4Outreach +
                updatedRow.age5to14Static + updatedRow.age5to14Outreach

        reportRows[vaccine] = updatedRow.copy(total = total)
    }


    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.HMIS105_VACCINES_REPORT)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            currentScreen.set(screens.firstOrNull())
        }
    }

    enum class Screen(@StringRes val label: Int) {
        HMIS105_VACCINES_REPORT(R.string.hmis105_report_title)
    }

    override fun saveInstanceState(outState: Bundle) {
        selectedStartDate.value?.let { outState.putString("selectedStartDate", it.toString()) }
        selectedEndDate.value?.let   { outState.putString("selectedEndDate",   it.toString()) }
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        val startDateStr = savedInstanceState.getString("selectedStartDate")
        val endDateStr   = savedInstanceState.getString("selectedEndDate")

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
