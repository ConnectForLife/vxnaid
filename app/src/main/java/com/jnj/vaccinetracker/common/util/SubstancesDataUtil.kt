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

        private const val HEP_B_BD_VACCINE_CONCEPT_NAME = "Hep B BD Vxnaid"
        private const val BCG_VACCINE_CONCEPT_NAME = "BCG Vxnaid"
        private const val POLIO_0_VACCINE_CONCEPT_NAME = "Polio 0 Vxnaid"
        private const val POLIO_1_VACCINE_CONCEPT_NAME = "Polio 1 Vxnaid"
        private const val POLIO_2_VACCINE_CONCEPT_NAME = "Polio 2 Vxnaid"
        private const val POLIO_3_VACCINE_CONCEPT_NAME = "Polio 3 Vxnaid"
        private const val ROTA_1_VACCINE_CONCEPT_NAME = "Rota 1 Vxnaid"
        private const val ROTA_2_VACCINE_CONCEPT_NAME = "Rota 2 Vxnaid"
        private const val ROTA_3_VACCINE_CONCEPT_NAME = "Rota 3 Vxnaid"
        private const val PCV_1_VACCINE_CONCEPT_NAME = "PCV 1 Vxnaid"
        private const val PCV_2_VACCINE_CONCEPT_NAME = "PCV 2 Vxnaid"
        private const val PCV_3_VACCINE_CONCEPT_NAME = "PCV 3 Vxnaid"
        private const val DPT_HEB_1_VACCINE_CONCEPT_NAME = "DPT-HepB-Hib 1 Vxnaid"
        private const val DPT_HEB_2_VACCINE_CONCEPT_NAME = "DPT-HepB-Hib 2 Vxnaid"
        private const val DPT_HEB_3_VACCINE_CONCEPT_NAME = "DPT-HepB-Hib 3 Vxnaid"
        private const val IPV_1_VACCINE_CONCEPT_NAME = "IPV 1 Vxnaid"
        private const val IPV_2_VACCINE_CONCEPT_NAME = "IPV 2 Vxnaid"
        private const val MR_1_VACCINE_CONCEPT_NAME = "Measles Rubella 1 (MR1) Vxnaid"
        private const val MR_2_VACCINE_CONCEPT_NAME = "Measles Rubella 2 (MR2) Vxnaid"
        private const val YELLOW_FEVER_VACCINE_CONCEPT_NAME = "Yellow Fever Vxnaid"
        private const val MALARIA_1_VACCINE_CONCEPT_NAME = "Malaria 1 Vxnaid"
        private const val MALARIA_2_VACCINE_CONCEPT_NAME = "Malaria 2 Vxnaid"
        private const val MALARIA_3_VACCINE_CONCEPT_NAME = "Malaria 3 Vxnaid"
        private const val MALARIA_4_VACCINE_CONCEPT_NAME = "Malaria 4 Vxnaid"

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
                            participantVisits,
                            substancesConfig
                        )
                    )
                }
            }

            val resultListWithoutDuplicates = substanceDataModelList.distinctBy { it.conceptName }
            val filteredResultList = applyVaccinesCatchUpSchedule(resultListWithoutDuplicates, childAgeInWeeks, participantVisits).toMutableList()
            handleHepBBDVaccine(filteredResultList, childAgeInWeeks, participantVisits, substancesConfig)
            handleBCGVaccine(filteredResultList, participantVisits, substancesConfig)

            return filteredResultList.filter { it.conceptName != "" }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getVisitTypeForCurrentVisit(
            participantBirthDate: String,
        ): String {
            val childAgeInWeeks = getWeeksBetweenDateAndToday(participantBirthDate)
            val visitType = getVisitTypeFromChildAgeInWeeks(childAgeInWeeks)
            return visitType
        }

        @RequiresApi(Build.VERSION_CODES.O)
        suspend fun getSubstancesDataForVisitType(
            visitType: String,
            configurationManager: ConfigurationManager
        ): List<SubstanceDataModel> {
            val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
            val visitTypeInWeeks = getWeeksBetweenDateAndTodayFromVisitType(visitType)
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substanceDataModelList = mutableListOf<SubstanceDataModel>()
            substancesConfig.forEach { substance ->
                val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                if (visitTypeInWeeks in minWeekNumber..maxWeekNumber) {
                    substanceDataModelList.add(
                        getSingleSubstanceData(
                            substance,
                            substancesGroupConfig,
                            null,
                            substancesConfig
                        )
                    )
                }
            }
            return substanceDataModelList.filter { it.conceptName != "" }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun isTimeIntervalMaintained(
            previousVaccineConceptName: String,
            participantVisits: List<VisitDetail>,
            weeksTimeInterval: Int
        ): Boolean {
            val vaccineDates = participantVisits.flatMap { visitDetail ->
                visitDetail.observations.filter { (key, _) ->
                    key == previousVaccineConceptName + " ${Constants.DATE_STR}"
                }.mapNotNull {
                    DateUtil.convertStringToDate(it.value.value, DateFormat.FORMAT_DATE.toString())
                }
            }

            val mostRecentVaccineDate = vaccineDates.maxOrNull() ?: return false
            val weeksSinceVaccine = ChronoUnit.WEEKS.between(
                mostRecentVaccineDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime(),
                LocalDateTime.now()
            )

            return weeksSinceVaccine >= weeksTimeInterval
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun applyVaccinesCatchUpSchedule(
            substances: List<SubstanceDataModel>,
            childAgeInWeeks: Int,
            participantVisits: List<VisitDetail>
        ): List<SubstanceDataModel> {
            return substances.filter { substance ->
                when (substance.conceptName) {
                    HEP_B_BD_VACCINE_CONCEPT_NAME -> {
                        childAgeInWeeks <= 6
                    }

                    ROTA_1_VACCINE_CONCEPT_NAME -> {
                        childAgeInWeeks <= 104
                    }

                    POLIO_2_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(POLIO_1_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    PCV_2_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(PCV_1_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    DPT_HEB_2_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(
                            DPT_HEB_1_VACCINE_CONCEPT_NAME,
                            participantVisits,
                            4
                        )
                    }

                    ROTA_2_VACCINE_CONCEPT_NAME -> {
                        childAgeInWeeks <= 208 && isTimeIntervalMaintained(
                            ROTA_1_VACCINE_CONCEPT_NAME,
                            participantVisits,
                            4
                        )
                    }

                    POLIO_3_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(POLIO_2_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    PCV_3_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(PCV_2_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    DPT_HEB_3_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(
                            DPT_HEB_2_VACCINE_CONCEPT_NAME,
                            participantVisits,
                            4
                        )
                    }

                    ROTA_3_VACCINE_CONCEPT_NAME -> {
                        childAgeInWeeks <= 208 && isTimeIntervalMaintained(
                            ROTA_2_VACCINE_CONCEPT_NAME,
                            participantVisits,
                            4
                        )
                    }

                    MR_1_VACCINE_CONCEPT_NAME, YELLOW_FEVER_VACCINE_CONCEPT_NAME -> {
                        childAgeInWeeks >= 36
                    }

                    MR_2_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(MR_1_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    MALARIA_2_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(MALARIA_1_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    MALARIA_3_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(MALARIA_2_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    MALARIA_4_VACCINE_CONCEPT_NAME -> {
                        isTimeIntervalMaintained(MALARIA_3_VACCINE_CONCEPT_NAME, participantVisits, 4)
                    }

                    else -> true
                }
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
                            otherSubstance.inputType,
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
            val otherSubstancesConfig = configurationManager.getOtherSubstancesConfig()
            val visitTypeInWeeks = getWeeksBetweenDateAndTodayFromVisitType(visitType)
            val otherSubstancesDataModelList = mutableListOf<OtherSubstanceDataModel>()
            otherSubstancesConfig.forEach { otherSubstance ->
                val minWeekNumber =
                    otherSubstance.weeksAfterBirth - otherSubstance.weeksAfterBirthLowWindow
                val maxWeekNumber =
                    otherSubstance.weeksAfterBirth + otherSubstance.weeksAfterBirthUpWindow
                if (visitTypeInWeeks in minWeekNumber..maxWeekNumber) {
                    otherSubstancesDataModelList.add(
                        OtherSubstanceDataModel(
                            otherSubstance.conceptName,
                            otherSubstance.label,
                            otherSubstance.category,
                            otherSubstance.inputType,
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
            participantVisits: List<VisitDetail>?,
            substancesConfig: SubstancesConfig
        ): SubstanceDataModel {
            val group =
                substancesGroupConfig.find { substanceGroup -> substanceGroup.substanceName == substance.group }
            val earlierElements =
                group?.options?.takeWhile { it != substance.conceptName }?.toMutableList()
                    ?: mutableListOf()
            if (group?.options?.contains(substance.conceptName) == true) {
                earlierElements.add(substance.conceptName)
            }
            var substanceToBeAdministered = substance.conceptName
            for (item in earlierElements) {
                if (participantVisits != null && isSubstanceAlreadyApplied(participantVisits, item)) {
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
                substanceToBeAdministeredObject?.routeOfAdministration ?: ""
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

        private fun getWeeksBetweenDateAndTodayFromVisitType(visitType: String): Int {
            return when (visitType) {
                Constants.VISIT_TYPE_AT_BIRTH -> 1
                Constants.VISIT_TYPE_SIX_WEEKS -> 6
                Constants.VISIT_TYPE_TEN_WEEKS -> 10
                Constants.VISIT_TYPE_FOURTEEN_WEEKS -> 14
                Constants.VISIT_TYPE_NINE_MONTHS -> 36
                Constants.VISIT_TYPE_EIGHTEEN_MONTHS -> 72
                Constants.VISIT_TYPE_TWO_YEARS -> 96
                else -> 1
            }
        }

        private fun getVisitTypeFromChildAgeInWeeks(ageInWeeks: Int): String {
            return when {
                ageInWeeks in 0..4 -> Constants.VISIT_TYPE_AT_BIRTH       // 0-4 weeks: Birth visit
                ageInWeeks in 5..8 -> Constants.VISIT_TYPE_SIX_WEEKS      // 5-8 weeks: Six weeks visit
                ageInWeeks in 9..12 -> Constants.VISIT_TYPE_TEN_WEEKS     // 9-12 weeks: Ten weeks visit
                ageInWeeks in 13..34 -> Constants.VISIT_TYPE_FOURTEEN_WEEKS // 13-34 weeks: Fourteen weeks visit
                ageInWeeks in 35..70 -> Constants.VISIT_TYPE_NINE_MONTHS  // 35-70 weeks: Nine months visit
                ageInWeeks in 71..94 -> Constants.VISIT_TYPE_EIGHTEEN_MONTHS // 71-94 weeks: Eighteen months visit
                ageInWeeks >= 95 -> Constants.VISIT_TYPE_TWO_YEARS        // 95+ weeks: Two years visit
                else -> Constants.VISIT_TYPE_AT_BIRTH                    // Default to Birth visit
            }
        }

        private fun isSubstanceAlreadyApplied(
            visits: List<VisitDetail>,
            substanceName: String
        ): Boolean {
            return visits.any { visit -> substanceName + " ${Constants.DATE_STR}" in visit.observations }
        }

        private fun handleHepBBDVaccine(
            substances: MutableList<SubstanceDataModel>,
            childAgeInWeeks: Int,
            participantVisits: List<VisitDetail>,
            substancesConfig: SubstancesConfig
        ) {
            val isHepBBDVaccineAlreadyApplied =
                participantVisits.any { visit -> HEP_B_BD_VACCINE_CONCEPT_NAME + " ${Constants.DATE_STR}" in visit.observations }
            if (!isHepBBDVaccineAlreadyApplied && substances.none { it.conceptName == HEP_B_BD_VACCINE_CONCEPT_NAME } && childAgeInWeeks <= 6) {
                val hepBBDVaccineObject =
                    substancesConfig.find { it.conceptName == HEP_B_BD_VACCINE_CONCEPT_NAME }
                substances.add(
                    SubstanceDataModel(
                        hepBBDVaccineObject?.conceptName ?: "",
                        hepBBDVaccineObject?.label ?: "",
                        hepBBDVaccineObject?.category ?: "",
                        hepBBDVaccineObject?.routeOfAdministration ?: ""
                    )
                )
            }
        }

        private fun handleBCGVaccine(
            substances: MutableList<SubstanceDataModel>,
            participantVisits: List<VisitDetail>,
            substancesConfig: SubstancesConfig
        ) {
            val isBCGVaccineAlreadyApplied =
                participantVisits.any { visit -> BCG_VACCINE_CONCEPT_NAME + " ${Constants.DATE_STR}" in visit.observations }
            val bcgVaccineObject =
                substancesConfig.find { it.conceptName == BCG_VACCINE_CONCEPT_NAME }
            if (!isBCGVaccineAlreadyApplied && substances.none { it.conceptName == BCG_VACCINE_CONCEPT_NAME }) {
                substances.add(
                    SubstanceDataModel(
                        bcgVaccineObject?.conceptName ?: "",
                        bcgVaccineObject?.label ?: "",
                        bcgVaccineObject?.category ?: "",
                        bcgVaccineObject?.routeOfAdministration ?: ""
                    )
                )
            }
        }
    }
}