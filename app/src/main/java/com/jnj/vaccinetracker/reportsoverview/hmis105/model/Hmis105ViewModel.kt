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

        // -----------------------------------------------------------------------
        // Observation keys — these are the FULL concept names as stored in the
        // local database (concept_name.name column).
        // They include "(MR1)"/"(MR2)" and the word "Date" as part of the name.
        // Source: concept_name table confirmed from database dump.
        // -----------------------------------------------------------------------

        // Individual vaccines mapped to HMIS 105 report labels.
        // MR2 is intentionally EXCLUDED here — it is rendered separately as CL27
        // after the SECOND YEAR OF LIFE heading, matching SQL ordering position 5.
        private val HMIS105_VACCINES = mapOf(
            "BCG Vxnaid Date"                      to "CL01. BCG",
            "Hep B BD Vxnaid Date"                 to "CL02. Hep B BD",
            "PAB for Td Vxnaid Date"               to "CL03. PAB for Td",
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

        // Exact observation keys for vaccines used in special computed rows.
        // Must match concept_name.name in the database exactly.
        private const val KEY_MR1          = "Measles Rubella 1 (MR1) Vxnaid Date"
        private const val KEY_MR2          = "Measles Rubella 2 (MR2) Vxnaid Date"
        private const val KEY_YELLOW_FEVER = "Yellow Fever Vxnaid Date"

        // Concept UUIDs from the SQL query — used for LLINs obs lookup only
        private const val UUID_LLINS = "6de53ec6-bf3f-41fe-bf2e-e61447a6557a"
    }

    init {
        initScreens()
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    fun getHmisMalaria105Data(startDate: DateTime?, endDate: DateTime?) {
        Log.d("Hmis105ViewModel", "getHmisMalaria105Data called")
        isLoading.value = true
        viewModelScope.launch {
            try {
                val start = startDate?.toDate() ?: getTodayMidnight()
                val end   = endDate?.toDate()   ?: addDaysToDate(getTodayMidnight(), 1)

                Log.d("Hmis105ViewModel", "Loading report data from $start to $end")

                val data = withContext(dispatchers.io) {
                    try {
                        // Only confirmed visits at the current facility within the date range.
                        // Mirrors SQL: o.obs_datetime >= :startDate AND < :endDate
                        //              AND pa.value IN (location uuids for :location)
                        val allVisits = visitRepository
                            .findAllVisitsByAttributeTypeAndValue(
                                Constants.ATTRIBUTE_VISIT_STATUS,
                                Constants.VISIT_STATUS_OCCURRED
                            )
                            .filter { visit ->
                                visit.startDatetime.time in start.time until end.time &&
                                        participantFromCurrentLocation(visit, currentLocationUuid)
                            }

                        Log.d("Hmis105ViewModel", "Total visits found: ${allVisits.size}")

                        // Log all unique observation keys — useful for debugging
                        // concept name mismatches between app and database
                        val allKeys = allVisits.flatMap { it.observations.keys }.toSet()
                        Log.d("Hmis105ViewModel", "All obs keys in filtered visits: $allKeys")

                        // Build participant cache once — shared across all report sections
                        val participantsMap = buildParticipantsMap(allVisits)

                        val reportData = mutableListOf<Hmis105ReportDTO>()

                        // CL01–CL23: individual vaccines (MR2 excluded from this list)
                        reportData.addAll(createHmis105ReportDTOList(allVisits, participantsMap))

                        // CL24: Fully immunized by 1 year (MR1 + Yellow Fever, age 8–12 months)
                        reportData.add(createFullyImmunized1Year(allVisits, participantsMap))

                        // CL25: LLINs (under 1 year, value = "yes")
                        reportData.add(createLLINSReport(allVisits, participantsMap))

                        // Section heading — formatting row only, no counts
                        reportData.add(Hmis105ReportDTO(doses = "SECOND YEAR OF LIFE"))

                        // CL27: MR2 — rendered after heading, matches SQL ordering position 5
                        reportData.add(createMR2Report(allVisits, participantsMap))

                        // CL28: Fully immunized by 2 years (MR2, age 17–24 months at vaccination)
                        reportData.add(createFullyImmunized2Years(allVisits, participantsMap))

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

    // =========================================================================
    // Participant cache — built once, passed to all report section builders
    // =========================================================================

    private suspend fun buildParticipantsMap(visits: List<Visit>): Map<String, ParticipantBase?> {
        val map = mutableMapOf<String, ParticipantBase?>()
        for (visit in visits) {
            if (!map.containsKey(visit.participantUuid)) {
                map[visit.participantUuid] =
                    findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
            }
        }
        return map
    }

    // =========================================================================
    // Age helpers
    //
    // CRITICAL: All age calculations use the VISIT DATE (obs_datetime), never
    // DateTime.now(). This matches the SQL behaviour:
    //   TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime)
    //   TIMESTAMPDIFF(YEAR,  p.birthdate, o.obs_datetime)
    // Using today's date would place children in the wrong age buckets for
    // historical visits, producing counts that don't match the SQL report.
    // =========================================================================

    /**
     * Age in whole months at [referenceDate].
     * Matches SQL: TIMESTAMPDIFF(MONTH, p.birthdate, referenceDate)
     */
    private fun calculateAgeInMonthsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        val birth = birthDate.toDateTime()
        return (referenceDate.yearInt - birth.yearInt) * 12 +
                (referenceDate.month0 - birth.month0)
    }

    /**
     * Age in whole years at [referenceDate].
     * Matches SQL: TIMESTAMPDIFF(YEAR, p.birthdate, referenceDate)
     */
    private fun calculateAgeInYearsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        return calculateAgeInMonthsAt(birthDate, referenceDate) / 12
    }

    /**
     * Maps to the SQL age buckets used across CL01–CL23 and CL27.
     * Age is evaluated at [visitDate] (obs_datetime), never today.
     *
     *   0–11  months → GROUP_AGE_FIRST  (Under 1 year)
     *  12–59  months → GROUP_AGE_SECOND (1–4 years)
     *   5–14  years  → GROUP_AGE_THIRD  (5–14 years)
     */
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

    // =========================================================================
    // Observation key helper
    //
    // Observation keys are the FULL concept name as stored in concept_name.name.
    // Example: "Measles Rubella 1 (MR1) Vxnaid Date"
    // No suffix is appended in code — the key IS the complete name including "Date".
    // =========================================================================

    private fun visitHasObs(visit: Visit, conceptName: String): Boolean {
        return visit.observations.containsKey(conceptName)
    }

    // =========================================================================
    // CL01–CL23: Individual vaccines
    //
    // - Observation key looked up directly in HMIS105_VACCINES map
    // - MR2 excluded (handled separately as CL27)
    // - Age group evaluated at visit date, not today
    // =========================================================================

    private suspend fun createHmis105ReportDTOList(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): List<Hmis105ReportDTO> {

        val reportRowsMap = mutableMapOf<String, Hmis105ReportDTO>()

        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            for ((key, _) in visit.observations) {
                // Direct map lookup — key is the full concept name
                val hmisLabel = HMIS105_VACCINES[key] ?: continue

                val ageGroup      = calculateChildAgeGroupAt(participant.birthDate, visitDateTime)
                val visitLocation = visit.visitLocation

                updateReportRow(reportRowsMap, hmisLabel, ageGroup, visitLocation)
                Log.d("Hmis105ViewModel", "Mapped [$key] → [$hmisLabel] | age: $ageGroup | loc: $visitLocation")
            }
        }

        Log.d("Hmis105ViewModel", "CL01–CL23 rows generated: ${reportRowsMap.keys}")
        return reportRowsMap.values.toList().sortedBy { it.doses }
    }

    // =========================================================================
    // CL24: Fully Immunized by 1 Year
    //
    // SQL logic:
    //   - Driven from MR1 obs at this facility in the date range
    //   - Child must be 8–12 months at time of MR1 vaccination (obs_datetime)
    //   - Child must also have received Yellow Fever at this facility in range
    //   - Delivery mode taken from the MR1 visit
    //   - Result always <= MIN(CL22 Yellow Fever count, CL23 MR1 count)
    // =========================================================================

    private suspend fun createFullyImmunized1Year(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        // Distinct participants who received Yellow Fever in the date range at this facility
        val yellowFeverRecipients = visits
            .filter { visitHasObs(it, KEY_YELLOW_FEVER) }
            .map { it.participantUuid }
            .toSet()

        // Distinct participants who received MR1 in the date range at this facility
        val mr1Recipients = visits
            .filter { visitHasObs(it, KEY_MR1) }
            .map { it.participantUuid }
            .toSet()

        Log.d("Hmis105ViewModel", "CL24 Yellow Fever recipients: ${yellowFeverRecipients.size}")
        Log.d("Hmis105ViewModel", "CL24 MR1 recipients: ${mr1Recipients.size}")

        // Must have received BOTH vaccines (intersection)
        val bothVaccinesUuids = yellowFeverRecipients.intersect(mr1Recipients)
        Log.d("Hmis105ViewModel", "CL24 received both YF + MR1: ${bothVaccinesUuids.size}")

        var under1Static   = 0
        var under1Outreach = 0

        for (participantUuid in bothVaccinesUuids) {
            val participant = participantsMap[participantUuid] ?: continue

            // Age is checked at the MR1 visit date (obs_datetime), not today
            val mr1Visit = visits.firstOrNull { visit ->
                visit.participantUuid == participantUuid &&
                        visitHasObs(visit, KEY_MR1)
            } ?: continue

            val visitDateTime = DateTime(mr1Visit.startDatetime.time)
            val ageAtMR1      = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)

            // Age window 8–12 months at vaccination time — matches SQL
            if (ageAtMR1 !in 8..12) {
                Log.d("Hmis105ViewModel", "CL24 skip $participantUuid — age at MR1 was $ageAtMR1 months")
                continue
            }

            // Delivery mode from the MR1 visit
            when (mr1Visit.visitLocation) {
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

    // =========================================================================
    // CL25: LLINs
    //
    // SQL logic:
    //   - Concept UUID: 6de53ec6-bf3f-41fe-bf2e-e61447a6557a
    //   - LOWER(TRIM(o.value_text)) = 'yes'
    //   - Age 0–11 months at observation date (obs_datetime)
    //   - Only Static / Outreach / School delivery modes counted
    // =========================================================================

    private suspend fun createLLINSReport(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var under1Static   = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            // Age at visit date — matches TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)
            if (ageInMonths !in 0..11) continue

            // Check for LLINs observation with value "yes"
            // Key may be stored by UUID or display name containing "LLIN"
            val hasLLINs = visit.observations.any { (key, obs) ->
                (key.contains(UUID_LLINS, ignoreCase = true) ||
                        key.contains("LLIN", ignoreCase = true)) &&
                        obs.value.trim().equals("yes", ignoreCase = true)
            }
            if (!hasLLINs) continue

            // SQL only counts known delivery modes — no default fallback for LLINs
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

    // =========================================================================
    // CL27: Measles Rubella 2 (MR2)
    //
    // SQL logic:
    //   - All MR2 recipients at this facility in the date range
    //   - Age buckets at vaccination date:
    //       12–59 months → 1-4 years columns
    //       5–14 years   → 5-14 years columns
    //   - Under-1 columns are always 0
    //   - Rendered AFTER SECOND YEAR OF LIFE heading (SQL ordering position 5)
    // =========================================================================

    private suspend fun createMR2Report(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var age1to4Static    = 0
        var age1to4Outreach  = 0
        var age5to14Static   = 0
        var age5to14Outreach = 0

        for (visit in visits) {
            if (!visitHasObs(visit, KEY_MR2)) continue

            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            val ageGroup      = calculateChildAgeGroupAt(participant.birthDate, visitDateTime)
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

    // =========================================================================
    // CL28: Fully Immunized by 2 Years
    //
    // SQL logic:
    //   - Same base population as CL27 (MR2 recipients at this facility in range)
    //   - Age window: 17–24 months at time of MR2 vaccination (obs_datetime)
    //   - Result always <= CL27 (same group, narrower age window)
    //   - Under-1 and 5-14 columns are always 0
    // =========================================================================

    private suspend fun createFullyImmunized2Years(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var age1to4Static   = 0
        var age1to4Outreach = 0

        for (visit in visits) {
            if (!visitHasObs(visit, KEY_MR2)) continue

            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            // Age at MR2 vaccination date — matches TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)

            // Age window 17–24 months — matches SQL
            if (ageInMonths !in 17..24) {
                Log.d("Hmis105ViewModel", "CL28 skip ${visit.participantUuid} — age at MR2 was $ageInMonths months")
                continue
            }

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

    // =========================================================================
    // updateReportRow — applies increment to the correct column
    // =========================================================================

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

    // =========================================================================
    // Location filter
    // =========================================================================

    private suspend fun participantFromCurrentLocation(
        visit: Visit,
        locationUuid: String?
    ): Boolean {
        val participant = findParticipantByParticipantUuidUseCase
            .findByParticipantUuid(visit.participantUuid)
        return participant?.locationUuid == locationUuid
    }

    // =========================================================================
    // Screen / state management
    // =========================================================================

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.HMIS105_REPORT)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            currentScreen.set(screens.firstOrNull())
        }
    }

    enum class Screen(@StringRes val label: Int) {
        HMIS105_REPORT(R.string.hmis105_report_title)
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
