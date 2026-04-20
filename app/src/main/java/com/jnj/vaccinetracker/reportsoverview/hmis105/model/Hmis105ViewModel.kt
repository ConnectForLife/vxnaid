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
        // HMIS 105 Report Vaccine Concept Names (without " Date" suffix)
        // Observation keys are stored as "<ConceptName> Date" in the database
        // CL27 (MR2) is intentionally EXCLUDED here; rendered after SECOND YEAR OF LIFE heading
        private val HMIS105_VACCINES = mapOf(
            "BCG Vxnaid"                          to "CL01. BCG",
            "Hep B BD Vxnaid"                     to "CL02. Hep B BD",
            "PAB for Td Vxnaid"                   to "CL03. PAB for Td",
            "Polio 0 Vxnaid"                      to "CL04. Polio 0",
            "Polio 1 Vxnaid"                      to "CL05. Polio 1",
            "Polio 2 Vxnaid"                      to "CL06. Polio 2",
            "Polio 3 Vxnaid"                      to "CL07. Polio 3",
            "IPV 1 Vxnaid"                        to "CL08. IPV 1",
            "IPV 2 Vxnaid"                        to "CL09. IPV 2",
            "DPT-HepB-Hib 1 Vxnaid"              to "CL10. DPT-HepB-Hib 1",
            "DPT-HepB-Hib 2 Vxnaid"              to "CL11. DPT-HepB-Hib 2",
            "DPT-HepB-Hib 3 Vxnaid"              to "CL12. DPT-HepB-Hib 3",
            "PCV 1 Vxnaid"                        to "CL13. PCV 1",
            "PCV 2 Vxnaid"                        to "CL14. PCV 2",
            "PCV 3 Vxnaid"                        to "CL15. PCV 3",
            "Rota 1 Vxnaid"                       to "CL16. Rota 1",
            "Rota 2 Vxnaid"                       to "CL17. Rota 2",
            "Rota 3 Vxnaid"                       to "CL18. Rota 3",
            "Yellow Fever Vxnaid"                 to "CL22. Yellow Fever",
            "Measles Rubella 1 (MR1) Vxnaid"     to "CL23. Measles Rubella 1 (MR1)"
            // NOTE: "Measles Rubella 2 (MR2) Vxnaid" is NOT here (handled separately as CL27)
        )

        // Observation key suffixes and concept names (without " Date" suffix)
        private const val DATE_OBS_SUFFIX       = " Date"
        private const val YELLOW_FEVER_NAME     = "Yellow Fever Vxnaid"
        private const val MR1_CONCEPT_NAME      = "Measles Rubella 1 (MR1) Vxnaid"
        private const val MR2_CONCEPT_NAME      = "Measles Rubella 2 (MR2) Vxnaid"
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
                val end   = endDate?.toDate()   ?: addDaysToDate(getTodayMidnight(), 1)

                Log.d("Hmis105ViewModel", "Loading report data from $start to $end")

                val data = withContext(dispatchers.io) {
                    try {
                        // Only confirmed visits at the current facility, within the date range.
                        // Mirrors SQL: o.obs_datetime >= :startDate AND o.obs_datetime < :endDate
                        // and pa.value IN (location uuids for :location)
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

                        // Cache all participants once up front to avoid repeated lookups
                        val participantsMap = buildParticipantsMap(allVisits)

                        val reportData = mutableListOf<Hmis105ReportDTO>()

                        // --- CL01–CL23: individual vaccines (excludes MR2) ---
                        reportData.addAll(createHmis105ReportDTOList(allVisits, participantsMap))

                        // --- CL24: Fully immunized by 1 year ---
                        reportData.add(createFullyImmunized1Year(allVisits, participantsMap))

                        // --- CL25: LLINs ---
                        reportData.add(createLLINSReport(allVisits, participantsMap))

                        // --- Section heading (formatting row, no counts) ---
                        reportData.add(Hmis105ReportDTO(doses = "SECOND YEAR OF LIFE"))

                        // --- CL27: MR2 (after heading, matches SQL ordering position 5) ---
                        reportData.add(createMR2Report(allVisits, participantsMap))

                        // --- CL28: Fully immunized by 2 years ---
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

    // -------------------------------------------------------------------------
    // Participant cache — built once and passed to all report builders
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Age helpers
    // FIX: Age is always calculated relative to the VISIT date (obs_datetime),
    //      NOT DateTime.now(). This matches TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime).
    // -------------------------------------------------------------------------

    /**
     * Returns age in whole months at the given reference date.
     * Matches: TIMESTAMPDIFF(MONTH, p.birthdate, referenceDate)
     */
    private fun calculateAgeInMonthsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        val birth = birthDate.toDateTime()
        return (referenceDate.yearInt - birth.yearInt) * 12 +
                (referenceDate.month0 - birth.month0)
    }

    /**
     * Returns age in whole years at the given reference date.
     * Matches: TIMESTAMPDIFF(YEAR, p.birthdate, referenceDate)
     */
    private fun calculateAgeInYearsAt(birthDate: BirthDate, referenceDate: DateTime): Int {
        return calculateAgeInMonthsAt(birthDate, referenceDate) / 12
    }

    /**
     * Maps to the SQL age buckets used in CL01–CL23 and CL27:
     *   0–11 months  → GROUP_AGE_FIRST  (Under 1)
     *  12–59 months  → GROUP_AGE_SECOND (1–4 years)
     *   5–14 years   → GROUP_AGE_THIRD  (5–14 years)
     * Age is evaluated at visitDate, matching o.obs_datetime in the SQL.
     */
    private fun calculateChildAgeGroupAt(birthDate: BirthDate, visitDate: DateTime): String {
        val ageInMonths = calculateAgeInMonthsAt(birthDate, visitDate)
        val ageInYears  = calculateAgeInYearsAt(birthDate, visitDate)
        return when {
            ageInMonths in 0..11   -> Constants.GROUP_AGE_FIRST
            ageInMonths in 12..59  -> Constants.GROUP_AGE_SECOND
            ageInYears  in 5..14   -> Constants.GROUP_AGE_THIRD
            else                   -> Constants.GROUP_AGE_FOURTH
        }
    }

    // -------------------------------------------------------------------------
    // Observation key helpers
    // The SQL identifies vaccines by concept UUID. Observation keys in the
    // local DB are stored as "<ConceptName> Date", so we match on concept name.
    // -------------------------------------------------------------------------

    /**
     * Returns true if the visit contains an observation matching the given concept name.
     * Observation keys are stored as "<ConceptName> Date" in the database.
     * This checks for the full key including the " Date" suffix.
     */
    private fun visitHasConceptByName(visit: Visit, conceptName: String): Boolean {
        val fullKey = "$conceptName$DATE_OBS_SUFFIX"
        return visit.observations.containsKey(fullKey)
    }

    // -------------------------------------------------------------------------
    // CL01–CL23: Individual vaccines
    // FIX: Age group now calculated at visit.startDatetime, not DateTime.now()
    // FIX: MR2 excluded from this list (handled separately as CL27)
    // -------------------------------------------------------------------------

    private suspend fun createHmis105ReportDTOList(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): List<Hmis105ReportDTO> {
        val reportRowsMap = mutableMapOf<String, Hmis105ReportDTO>()

        val vaccinesConfig = configurationManager.getSubstancesConfig()
            .filter { it.category == Constants.VACCINES_CATEGORY_NAME }

        Log.d("Hmis105ViewModel", "Available vaccine concept names: ${vaccinesConfig.map { it.conceptName }}")

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid] ?: continue
            // FIX: use visit date for age, not today
            val visitDateTime = DateTime(visit.startDatetime.time)

            for ((key, _) in visit.observations) {
                // Keys are stored as "<ConceptName> Date"
                val extractedConceptName = if (key.endsWith(DATE_OBS_SUFFIX)) {
                    key.dropLast(DATE_OBS_SUFFIX.length)
                } else continue

                val matchedVaccine = vaccinesConfig.find { it.conceptName == extractedConceptName }
                    ?: continue

                val hmisLabel = HMIS105_VACCINES[extractedConceptName] ?: continue

                // FIX: age group calculated at vaccination date, not today
                val ageGroup      = calculateChildAgeGroupAt(participant.birthDate, visitDateTime)
                val visitLocation = visit.visitLocation

                updateReportRow(reportRowsMap, hmisLabel, ageGroup, visitLocation)
                Log.d("Hmis105ViewModel", "Mapped $extractedConceptName → $hmisLabel | age group: $ageGroup")
            }
        }

        Log.d("Hmis105ViewModel", "Final report rows: ${reportRowsMap.keys}")
        // Sort matches SQL ORDER BY position 0, then Doses alphabetically
        return reportRowsMap.values.toList().sortedBy { it.doses }
    }

    // -------------------------------------------------------------------------
    // CL24: Fully Immunized by 1 Year
    // Requirement : received BOTH Yellow Fever AND MR1 at this facility
    // Age window  : 8–12 months at time of MR1 vaccination (obs_datetime)
    // FIX: age calculated at visit date; both vaccine checks use concept names;
    //      only children who are 8–12 months at MR1 vaccination time are counted
    // -------------------------------------------------------------------------

    private suspend fun createFullyImmunized1Year(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        // Collect participants who received Yellow Fever (any visit in range)
        val yellowFeverRecipients = visits
            .filter { visitHasConceptByName(it, YELLOW_FEVER_NAME) }
            .map { it.participantUuid }
            .toSet()

        // Collect participants who received MR1 (any visit in range)
        val mr1Recipients = visits
            .filter { visitHasConceptByName(it, MR1_CONCEPT_NAME) }
            .map { it.participantUuid }
            .toSet()

        Log.d("Hmis105ViewModel", "CL24 Yellow Fever recipients: ${yellowFeverRecipients.size}, MR1 recipients: ${mr1Recipients.size}")

        // Intersection: must have received BOTH
        val bothVaccinesUuids = yellowFeverRecipients.intersect(mr1Recipients)
        Log.d("Hmis105ViewModel", "CL24 received both vaccines: ${bothVaccinesUuids.size}")

        var under1Static   = 0
        var under1Outreach = 0

        for (participantUuid in bothVaccinesUuids) {
            val participant = participantsMap[participantUuid] ?: continue

            // Find the MR1 visit for this participant to get the vaccination date
            // (mirrors SQL: age checked at o.obs_datetime of the MR1 observation)
            val mr1Visit = visits.firstOrNull { visit ->
                visit.participantUuid == participantUuid &&
                        visitHasConceptByName(visit, MR1_CONCEPT_NAME)
            } ?: continue

            val visitDateTime  = DateTime(mr1Visit.startDatetime.time)
            val ageAtMR1       = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)

            // FIX: age window 8–12 months at vaccination time, matching SQL
            if (ageAtMR1 !in 8..12) {
                Log.d("Hmis105ViewModel", "CL24 skipping $participantUuid — age at MR1 was $ageAtMR1 months")
                continue
            }

            // Delivery mode from the MR1 visit
            when (mr1Visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC                                         -> under1Static++
                Constants.VISIT_PLACE_OUTREACH, Constants.VISIT_PLACE_SCHOOL        -> under1Outreach++
                else                                                                  -> under1Static++ // default static
            }
        }

        Log.d("Hmis105ViewModel", "CL24 Final — Static: $under1Static, Outreach: $under1Outreach")
        return Hmis105ReportDTO(
            doses        = "CL24. Fully immunized by 1 year",
            under1Static  = under1Static,
            under1Outreach = under1Outreach,
            total         = under1Static + under1Outreach
        )
    }

    // -------------------------------------------------------------------------
    // CL25: LLINs
    // Children under 1 year (0–11 months at visit date) who received LLINs
    // FIX: age calculated at visit date, not today
    // -------------------------------------------------------------------------

    private suspend fun createLLINSReport(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var under1Static   = 0
        var under1Outreach = 0

        for (visit in visits) {
            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            // FIX: age at visit date, matching TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)
            if (ageInMonths !in 0..11) continue

            // Check for LLINs observation with value "yes"
            // Matches SQL: c.uuid = UUID_LLINS AND LOWER(TRIM(o.value_text)) = 'yes'
            val hasLLINs = visit.observations.any { (key, obs) ->
                (key.contains("LLIN") || key.contains("Long-Lasting")) &&
                        obs.value.trim().equals("yes", ignoreCase = true)
            }
            if (!hasLLINs) continue

            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC                                  -> under1Static++
                Constants.VISIT_PLACE_OUTREACH, Constants.VISIT_PLACE_SCHOOL -> under1Outreach++
                // No else/default: SQL does not count if visitLocation is unknown
            }
        }

        return Hmis105ReportDTO(
            doses          = "CL25. No. received LLINs",
            under1Static   = under1Static,
            under1Outreach = under1Outreach,
            total          = under1Static + under1Outreach
        )
    }

    // -------------------------------------------------------------------------
    // CL27: Measles Rubella 2 (MR2)
    // Age buckets: 12–59 months (1-4 years) OR 5–14 years at vaccination date
    // FIX: age calculated at visit date, not today
    // FIX: rendered AFTER the SECOND YEAR OF LIFE heading, matching SQL ordering
    // -------------------------------------------------------------------------

    private suspend fun createMR2Report(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var age1to4Static   = 0
        var age1to4Outreach = 0
        var age5to14Static   = 0
        var age5to14Outreach = 0

        for (visit in visits) {
            if (!visitHasConceptByName(visit, MR2_CONCEPT_NAME)) continue

            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            // FIX: age group at vaccination date
            val ageGroup = calculateChildAgeGroupAt(participant.birthDate, visitDateTime)

            when {
                ageGroup == Constants.GROUP_AGE_SECOND &&
                        visit.visitLocation == Constants.VISIT_PLACE_STATIC                          -> age1to4Static++
                ageGroup == Constants.GROUP_AGE_SECOND &&
                        (visit.visitLocation == Constants.VISIT_PLACE_OUTREACH ||
                                visit.visitLocation == Constants.VISIT_PLACE_SCHOOL)                        -> age1to4Outreach++
                ageGroup == Constants.GROUP_AGE_THIRD &&
                        visit.visitLocation == Constants.VISIT_PLACE_STATIC                          -> age5to14Static++
                ageGroup == Constants.GROUP_AGE_THIRD &&
                        (visit.visitLocation == Constants.VISIT_PLACE_OUTREACH ||
                                visit.visitLocation == Constants.VISIT_PLACE_SCHOOL)                        -> age5to14Outreach++
            }
        }

        val total = age1to4Static + age1to4Outreach + age5to14Static + age5to14Outreach
        Log.d("Hmis105ViewModel", "CL27 MR2 — 1-4 Static: $age1to4Static, 1-4 Outreach: $age1to4Outreach, 5-14 Static: $age5to14Static, 5-14 Outreach: $age5to14Outreach")
        return Hmis105ReportDTO(
            doses          = "CL27. Measles Rubella 2 (MR2)",
            age1to4Static   = age1to4Static,
            age1to4Outreach = age1to4Outreach,
            age5to14Static  = age5to14Static,
            age5to14Outreach = age5to14Outreach,
            total           = total
        )
    }

    // -------------------------------------------------------------------------
    // CL28: Fully Immunized by 2 Years
    // Requirement : received MR2 (same base as CL27)
    // Age window  : 17–24 months at time of MR2 vaccination
    // FIX: age calculated at vaccination date; always <= CL27 (narrower window)
    // -------------------------------------------------------------------------

    private suspend fun createFullyImmunized2Years(
        visits: List<Visit>,
        participantsMap: Map<String, ParticipantBase?>
    ): Hmis105ReportDTO {

        var age1to4Static   = 0
        var age1to4Outreach = 0

        for (visit in visits) {
            if (!visitHasConceptByName(visit, MR2_CONCEPT_NAME)) continue

            val participant   = participantsMap[visit.participantUuid] ?: continue
            val visitDateTime = DateTime(visit.startDatetime.time)

            // FIX: age at vaccination date, matching TIMESTAMPDIFF(MONTH, p.birthdate, o.obs_datetime)
            val ageInMonths = calculateAgeInMonthsAt(participant.birthDate, visitDateTime)

            // Age window 17–24 months, matching SQL
            if (ageInMonths !in 17..24) {
                Log.d("Hmis105ViewModel", "CL28 skipping ${visit.participantUuid} — age at MR2 was $ageInMonths months")
                continue
            }

            when (visit.visitLocation) {
                Constants.VISIT_PLACE_STATIC                                         -> age1to4Static++
                Constants.VISIT_PLACE_OUTREACH, Constants.VISIT_PLACE_SCHOOL        -> age1to4Outreach++
                else                                                                  -> age1to4Static++
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

    // -------------------------------------------------------------------------
    // updateReportRow — unchanged logic, just called with corrected age groups
    // -------------------------------------------------------------------------

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
            ageGroup == Constants.GROUP_AGE_FIRST  && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(under1Outreach = currentRow.under1Outreach + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age1to4Static = currentRow.age1to4Static + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age1to4Outreach = currentRow.age1to4Outreach + 1)
            ageGroup == Constants.GROUP_AGE_THIRD  && mode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age5to14Static = currentRow.age5to14Static + 1)
            ageGroup == Constants.GROUP_AGE_THIRD  && (mode == Constants.VISIT_PLACE_OUTREACH || mode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age5to14Outreach = currentRow.age5to14Outreach + 1)
            else -> currentRow
        }

        val total = updatedRow.under1Static + updatedRow.under1Outreach +
                updatedRow.age1to4Static + updatedRow.age1to4Outreach +
                updatedRow.age5to14Static + updatedRow.age5to14Outreach

        reportRows[vaccine] = updatedRow.copy(total = total)
    }

    // -------------------------------------------------------------------------
    // Location filter — unchanged
    // -------------------------------------------------------------------------

    private suspend fun participantFromCurrentLocation(visit: Visit, locationUuid: String?): Boolean {
        val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
        return participant?.locationUuid == locationUuid
    }

    // -------------------------------------------------------------------------
    // Screen / state management — unchanged
    // -------------------------------------------------------------------------

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
            try { selectedStartDate.value = DateTime.parse(startDateStr).local }
            catch (e: Exception) { Log.e("Hmis105ViewModel", "Failed to parse start date: $startDateStr", e) }
        }
        if (endDateStr != null) {
            try { selectedEndDate.value = DateTime.parse(endDateStr).local }
            catch (e: Exception) { Log.e("Hmis105ViewModel", "Failed to parse end date: $endDateStr", e) }
        }
    }
}