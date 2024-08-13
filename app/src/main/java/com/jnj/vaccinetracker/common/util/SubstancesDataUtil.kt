package com.jnj.vaccinetracker.common.util

import android.os.Build
import androidx.annotation.RequiresApi
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.domain.entities.Substance
import com.jnj.vaccinetracker.common.domain.entities.SubstancesGroupConfig
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import java.time.LocalDate
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
                if (childAgeInWeeks in minWeekNumber..maxWeekNumber) {
                    substanceDataModelList.add(
                        getSingleSubstanceData(
                            substance,
                            substancesGroupConfig,
                            participantVisits
                        )
                    )
                }
            }

            return substanceDataModelList.filter { it.conceptName != ""}
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
                            substance.routeOfAdministration
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
                            otherSubstance.options
                        )
                    )
                }
            }

            return otherSubstancesDataModelList
        }

        private fun getSingleSubstanceData(
            substance: Substance,
            substancesGroupConfig: SubstancesGroupConfig,
            participantVisits: List<VisitDetail>
        ): SubstanceDataModel {
            val group =
                substancesGroupConfig.find { substanceGroup -> substanceGroup.substanceName == substance.group }
            val earlierElements = group?.options?.takeWhile { it != substance.conceptName }?.toMutableList() ?: mutableListOf()
            if (group?.options?.contains(substance.conceptName) == true) {
                earlierElements.add(substance.conceptName)
            }
            var substanceToBeAdministered = substance.conceptName
            for (item in earlierElements) {
                if (isSubstanceAlreadyApplied(participantVisits, item)) {
                    substanceToBeAdministered = ""
                } else {
                    substanceToBeAdministered = item
                    break
                }
            }

            return SubstanceDataModel(
                substanceToBeAdministered,
                substance.label,
                substance.category,
                substance.routeOfAdministration
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getWeeksBetweenDateAndToday(dateString: String): Int {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDate = LocalDate.parse(dateString, formatter)
            val endDate = LocalDate.now()
            val daysBetween = ChronoUnit.DAYS.between(startDate, endDate).toDouble()

            return ceil(daysBetween / 7).toInt()
        }

        private fun isSubstanceAlreadyApplied(
            visits: List<VisitDetail>,
            substanceName: String
        ): Boolean {
            return visits.any { visit -> substanceName in visit.observations }
        }
    }
}