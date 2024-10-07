package com.jnj.vaccinetracker.common.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.Substance
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

class SubstancesDataUtil {

    companion object {

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getSubstancesDataForCurrentVisit(
            participantBirthDate: String,
            participantVisits: List<VisitDetail>,
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                if (childAgeInWeeks in minWeekNumber..maxWeekNumber &&
                    !isSubstanceAlreadyApplied(participantVisits, substance.conceptName)
                ) {
                    substanceDataModelList.add(
                        getSingleSubstanceData(
                            substance,
                            substancesGroupConfig,
                            participantVisits,
                            substancesConfig
                        )
                    )
                }
            }

            val resultListWithoutDuplicates = substanceDataModelList.distinctBy { it.conceptName }
            val filteredResultList = applyVaccinesCatchUpSchedule(
                resultListWithoutDuplicates,
                childAgeInWeeks,
                participantVisits,
                substancesGroupConfig
            ).toMutableList()

            return filteredResultList.filter { it.conceptName != "" }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getVisitTypeForCurrentVisit(
            participantBirthDate: String,
            participantVisits: List<VisitDetail>,
            configurationManager: ConfigurationManager
        ): String {
            val allSubstancesConfig = configurationManager.getSubstancesConfig()
            val visitTypesOrdered = getVisitTypesInOrder(allSubstancesConfig)
            val suggestedSubstancesForChild = getSubstancesDataForCurrentVisit(participantBirthDate, participantVisits, configurationManager)

            return when {
                // Case 1: If there are suggested substances for the child
                suggestedSubstancesForChild.isNotEmpty() -> {
                    val visitTypesInSuggestedSubstances = getVisitTypesFromSubstances(suggestedSubstancesForChild)
                    getBestVisitType(visitTypesInSuggestedSubstances, visitTypesOrdered)
                }
                // Case 2: If no suggested substances, check for the last visit
                else -> getLastVisitType(participantVisits, allSubstancesConfig, visitTypesOrdered) ?: ""
            }
        }

        private fun getBestVisitType(visitTypesInSuggestedSubstances: List<String>, visitTypesOrdered: List<String>): String {
            return when {
                visitTypesInSuggestedSubstances.size == 1 -> visitTypesInSuggestedSubstances[0]
                visitTypesInSuggestedSubstances.size > 1 -> visitTypesInSuggestedSubstances.maxByOrNull { visitTypesOrdered.indexOf(it) } ?: ""
                else -> ""
            }
        }

        private fun getLastVisitType(
            participantVisits: List<VisitDetail>,
            allSubstancesConfig: List<Substance>,
            visitTypesOrdered: List<String>
        ): String? {
            val lastVisit = participantVisits.filter{it.visitStatus == Constants.VISIT_STATUS_OCCURRED}.maxByOrNull { it.visitDate }


            lastVisit?.let {
                val substanceNames = extractSubstanceNamesFromObservations(it)
                val visitTypesFromLastVisit = getVisitTypesFromSubstanceNames(substanceNames, allSubstancesConfig)
                val lastVisitType = visitTypesFromLastVisit.maxByOrNull { visitTypesOrdered.indexOf(it) }
                val nextVisitIndex = lastVisitType?.let { visitTypesOrdered.indexOf(it) + 1 }

                return nextVisitIndex?.takeIf { it < visitTypesOrdered.size }?.let { visitTypesOrdered[it] }
            }

            return null
        }

        private fun extractSubstanceNamesFromObservations(visit: VisitDetail): List<String> {
            return visit.observations.mapNotNull { (key, _) ->
                if (key.endsWith(" ${Constants.DATE_STR}")) key.split(" ${Constants.DATE_STR}")[0] else null
            }
        }

        private fun getVisitTypesFromSubstanceNames(
            substanceNames: List<String>,
            allSubstancesConfig: List<Substance>
        ): List<String> {
            return substanceNames.mapNotNull { substanceName ->
                allSubstancesConfig.find { it.conceptName == substanceName }?.visitType
            }
        }

        private fun getVisitTypesInOrder(substances: List<Substance>): List<String> {
            return substances
                .groupBy { it.weeksAfterBirth }
                .toSortedMap()
                .map { it.value.first().visitType }
        }

        private fun getVisitTypesFromSubstances(substances: List<SubstanceDataModel>): List<String> {
            return substances.map { it.visitType.toString() }
        }


        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getSubstancesDataForVisitType(
            visitType: String,
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val filteredSubstancesByVisitType = configurationManager.getSubstancesConfig()
                .filter { substance -> substance.visitType == visitType }
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()

            filteredSubstancesByVisitType.forEach { substance ->
                substanceDataModelList.add(
                    SubstanceDataModel(
                        substance.conceptName,
                        substance.label,
                        substance.category,
                        substance.routeOfAdministration,
                        substance.group,
                        substance.maximumAgeInWeeks,
                        substance.minimumWeeksNumberAfterPreviousDose,
                        substance.visitType
                    )
                )
            }

            return substanceDataModelList.filter { it.conceptName != "" }.distinctBy { it.conceptName }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun isTimeIntervalMaintained(
            previousDoseConceptName: String?,
            participantVisits: List<VisitDetail>,
            minimumWeeksNumberAfterPreviousDose: Int?
        ): Boolean {

            if (previousDoseConceptName == null || minimumWeeksNumberAfterPreviousDose == null) {
                return true
            }

            val vaccineDates = participantVisits.flatMap { visitDetail ->
                visitDetail.observations.filter { (key, _) ->
                    key == previousDoseConceptName + " ${Constants.DATE_STR}"
                }.mapNotNull {
                    DateUtil.convertStringToDate(it.value.value, DateFormat.FORMAT_DATE.toString())
                }
            }

            val mostRecentVaccineDate = vaccineDates.maxOrNull() ?: return false
            val weeksSinceVaccine = ChronoUnit.WEEKS.between(
                mostRecentVaccineDate.toInstant().atZone(ZoneId.of(Constants.UTC_TIME_ZONE_NAME))
                    .toLocalDateTime(),
                LocalDateTime.now()
            )

            return weeksSinceVaccine >= minimumWeeksNumberAfterPreviousDose
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyVaccinesCatchUpSchedule(
            substances: List<SubstanceDataModel>,
            childAgeInWeeks: Int,
            participantVisits: List<VisitDetail>,
            substancesGroupConfig: SubstancesGroupConfig
        ): List<SubstanceDataModel> {
            return substances.filter { substance ->
                val previousDoseConceptName = findPreviousDoseName(substance, substancesGroupConfig)
                (substance.maximumAgeInWeeks == null || childAgeInWeeks <= substance.maximumAgeInWeeks) &&
                        isTimeIntervalMaintained(
                            previousDoseConceptName,
                            participantVisits,
                            substance.minimumWeeksNumberAfterPreviousDose
                        )
            }
        }

        suspend fun getAllSubstances(
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                substanceDataModelList.add(
                    SubstanceDataModel(
                        substance.conceptName,
                        substance.label,
                        substance.category,
                        substance.routeOfAdministration,
                        substance.group,
                        substance.maximumAgeInWeeks,
                        substance.minimumWeeksNumberAfterPreviousDose,
                        substance.visitType
                    )
                )
            }

            return substanceDataModelList
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getOtherSubstancesDataForCurrentVisit(
            participantBirthDate: String,
            configurationManager: ConfigurationManager
        ): List<OtherSubstanceDataModel> {
            val otherSubstancesConfig = configurationManager.getOtherSubstancesConfig()
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val otherSubstancesDataModelList = mutableListOf<OtherSubstanceDataModel>()
            otherSubstancesConfig.forEach { otherSubstance ->
                val minWeekNumber =
                    otherSubstance.weeksAfterBirth - otherSubstance.weeksAfterBirthLowWindow
                val maxWeekNumber =
                    otherSubstance.weeksAfterBirth + otherSubstance.weeksAfterBirthUpWindow
                if (childAgeInWeeks in minWeekNumber..maxWeekNumber) {
                    otherSubstancesDataModelList.add(
                        OtherSubstanceDataModel(
                            otherSubstance.conceptName,
                            otherSubstance.label,
                            otherSubstance.category,
                            otherSubstance.inputType,
                            otherSubstance.visitType,
                            otherSubstance.options
                        )
                    )
                }
            }

            return otherSubstancesDataModelList
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getOtherSubstancesDataForVisitType(
            visitType: String,
            configurationManager: ConfigurationManager
        ): List<OtherSubstanceDataModel> {
            val otherSubstancesDataModelList = mutableListOf<OtherSubstanceDataModel>()
            val filteredOtherSubstancesByVisitType =
                configurationManager.getOtherSubstancesConfig().filter { it.visitType == visitType }
                    .distinct()

            filteredOtherSubstancesByVisitType.forEach { otherSubstance ->
                otherSubstancesDataModelList.add(
                    OtherSubstanceDataModel(
                        otherSubstance.conceptName,
                        otherSubstance.label,
                        otherSubstance.category,
                        otherSubstance.inputType,
                        otherSubstance.visitType,
                        otherSubstance.options
                    )
                )
            }

            return otherSubstancesDataModelList
        }

        private fun getSingleSubstanceData(
            substance: Substance,
            substancesGroupConfig: SubstancesGroupConfig,
            participantVisits: List<VisitDetail>?,
            substancesConfig: SubstancesConfig
        ): SubstanceDataModel {
            val group =
                substancesGroupConfig.find { substanceGroup -> substanceGroup.substanceName == substance.group }
            val earlierDoses =
                group?.options?.takeWhile { it != substance.conceptName }?.toMutableList()
                    ?: mutableListOf()
            if (group?.options?.contains(substance.conceptName) == true) {
                earlierDoses.add(substance.conceptName)
            }
            var substanceToBeAdministered = substance.conceptName
            for (item in earlierDoses) {
                if (participantVisits != null && isSubstanceAlreadyApplied(
                        participantVisits,
                        item
                    )
                ) {
                    substanceToBeAdministered = ""
                } else {
                    substanceToBeAdministered = item
                    break
                }
            }

            val substanceToBeAdministeredObject =
                substancesConfig.find { it.conceptName == substanceToBeAdministered }

            return SubstanceDataModel(
                substanceToBeAdministeredObject?.conceptName ?: "",
                substanceToBeAdministeredObject?.label ?: "",
                substanceToBeAdministeredObject?.category ?: "",
                substanceToBeAdministeredObject?.routeOfAdministration ?: "",
                substanceToBeAdministeredObject?.group ?: "",
                substanceToBeAdministeredObject?.maximumAgeInWeeks,
                substanceToBeAdministeredObject?.minimumWeeksNumberAfterPreviousDose,
                substanceToBeAdministeredObject?.visitType
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getWeeksBetweenDateAndToday(dateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern(DateFormat.FORMAT_DATE.toString())
            val startDate = LocalDate.parse(dateString, formatter)
            val endDate = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return ceil(daysBetween / 7).toInt()
        }

        private fun isSubstanceAlreadyApplied(
            visits: List<VisitDetail>,
            substanceName: String
        ): Boolean {
            return visits.any { visit -> substanceName + " ${Constants.DATE_STR}" in visit.observations }
        }

        private fun findPreviousDoseName(
            substance: SubstanceDataModel,
            substancesGroupConfig: SubstancesGroupConfig
        ): String? {
            val group = substancesGroupConfig.find { it.substanceName == substance.group }
            val index = group?.options?.indexOf(substance.conceptName)

            val previousVaccineConceptName = if (index!! > 0) {
                group.options[index - 1]
            } else {
                null
            }

            return previousVaccineConceptName
        }
    }
}