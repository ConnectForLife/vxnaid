package com.jnj.vaccinetracker.reportsoverview.hmis105.repository

import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ReportDTO
import com.soywiz.klock.DateTime
import java.util.Date
import javax.inject.Inject

class Hmis105Repository @Inject constructor(
    private val visitRepository: VisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    private val configurationManager: ConfigurationManager
) {

    companion object {
        // HMIS 105 Report Vaccine Concept Names (These should be mapped to actual concept UUIDs in your system)
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

    suspend fun getHmisMalaria105ReportData(
        startDate: Date,
        endDate: Date,
        locationUuid: String? = null
    ): List<Hmis105ReportDTO> {
        try {
            val vaccineConfig = configurationManager.getSubstancesConfig()
                .filter { it.category == Constants.VACCINES_CATEGORY_NAME }

            val visitDates = getVisitDatesInRange(startDate, endDate)
            val reportRows = mutableMapOf<String, Hmis105ReportDTO>()

            for (visit in visitDates) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                if (participant == null) continue

                val ageGroup = calculateAgeGroup(participant.birthDate, visit.startDatetime)
                val deliveryMode = visit.visitLocation ?: Constants.VISIT_PLACE_STATIC

                for ((key, observation) in visit.observations) {
                    val vaccineConceptName = vaccineConfig.find { key == "${it.conceptName} ${Constants.DATE_STR}" }
                    if (vaccineConceptName != null) {
                        val reportLabel = HMIS105_VACCINES[vaccineConceptName.conceptName] ?: vaccineConceptName.label
                        updateReportRow(reportRows, reportLabel, ageGroup, deliveryMode)
                    }
                }
            }

            return reportRows.values.toList()
                .sortedWith(compareBy<Hmis105ReportDTO> { it.doses }.thenBy { it.doses })
        } catch (ex: Exception) {
            return emptyList()
        }
    }

    private suspend fun getVisitDatesInRange(startDate: Date, endDate: Date): List<Visit> {
        val occurredVisits = visitRepository
            .findAllVisitsByAttributeTypeAndValue(Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED)
            .filter { visit ->
                visit.startDatetime.time in startDate.time..endDate.time
            }

        val draftVisitEncounters = draftVisitEncounterRepository.findVisitsBeforeDate(endDate)
            .filter { it.startDatetime.time >= startDate.time }
        val draftVisitEncountersAsVisits = draftVisitEncounters.map { convertDraftVisitEncounterToVisit(it) }

        return occurredVisits + draftVisitEncountersAsVisits
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

    private fun calculateAgeGroup(birthDate: BirthDate, visitDate: Date): String {
        val visitDateTime = DateTime.fromUnix(visitDate.time)
        val birthDateTime = birthDate.toDateTime()

        val ageInMonths = (visitDateTime.yearInt - birthDateTime.yearInt) * 12 + (visitDateTime.month0 - birthDateTime.month0)
        val ageInYears = ageInMonths / 12

        return when {
            ageInMonths < 12 -> Constants.GROUP_AGE_FIRST // "0-11 months"
            ageInMonths < 60 -> Constants.GROUP_AGE_SECOND // "12-59 months"
            ageInYears <= 14 -> Constants.GROUP_AGE_THIRD // "5-14 years"
            else -> Constants.GROUP_AGE_FOURTH // "14+ years"
        }
    }

    private fun updateReportRow(
        reportRows: MutableMap<String, Hmis105ReportDTO>,
        label: String,
        ageGroup: String,
        deliveryMode: String
    ) {
        val currentRow = reportRows[label] ?: Hmis105ReportDTO(doses = label)
        
        val updatedRow = when {
            ageGroup == Constants.GROUP_AGE_FIRST && deliveryMode == Constants.VISIT_PLACE_STATIC -> 
                currentRow.copy(under1Static = currentRow.under1Static + 1)
            ageGroup == Constants.GROUP_AGE_FIRST && (deliveryMode == Constants.VISIT_PLACE_OUTREACH || deliveryMode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(under1Outreach = currentRow.under1Outreach + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && deliveryMode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age1to4Static = currentRow.age1to4Static + 1)
            ageGroup == Constants.GROUP_AGE_SECOND && (deliveryMode == Constants.VISIT_PLACE_OUTREACH || deliveryMode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age1to4Outreach = currentRow.age1to4Outreach + 1)
            ageGroup == Constants.GROUP_AGE_THIRD && deliveryMode == Constants.VISIT_PLACE_STATIC ->
                currentRow.copy(age5to14Static = currentRow.age5to14Static + 1)
            ageGroup == Constants.GROUP_AGE_THIRD && (deliveryMode == Constants.VISIT_PLACE_OUTREACH || deliveryMode == Constants.VISIT_PLACE_SCHOOL) ->
                currentRow.copy(age5to14Outreach = currentRow.age5to14Outreach + 1)
            else -> currentRow
        }
        
        val total = updatedRow.under1Static + updatedRow.under1Outreach + updatedRow.age1to4Static +
                   updatedRow.age1to4Outreach + updatedRow.age5to14Static + updatedRow.age5to14Outreach
        
        reportRows[label] = updatedRow.copy(total = total)
    }
}

