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
    import java.time.LocalDateTime
    import java.time.ZoneId
    import java.time.temporal.ChronoUnit
    import kotlin.math.abs

    class SubstancesDataUtil {

        companion object {

            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun getSubstancesDataForCurrentVisit(
                participantBirthDate: String,
                participantVisits: List<VisitDetail>,
                configurationManager: ConfigurationManager
            ): List<SubstanceDataModel> {
                val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
                var childAgeInWeeks = DateUtil.getFullWeeksBetweenDateAndToday(participantBirthDate)
                val substancesConfig = configurationManager.getSubstancesConfig()
                val substanceDataModelList = mutableListOf<SubstanceDataModel>()
                substancesConfig.forEach { substance ->
                    val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                    val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                    childAgeInWeeks = if (substance.conceptName == Constants.HEP_B_BD_VACCINE_CONCEPT_NAME
                        || substance.conceptName == Constants.POLIO_0_VACCINE_CONCEPT_NAME) {
                        DateUtil.getRoundedUpWeeksBetweenDateAndToday(participantBirthDate)
                    } else {
                        DateUtil.getFullWeeksBetweenDateAndToday(participantBirthDate)
                    }
                    if (childAgeInWeeks in minWeekNumber..maxWeekNumber &&
                        !isSubstanceAlreadyApplied(participantVisits, substance.conceptName)
                    ) {
                        substanceDataModelList.add(
                            getSingleSubstanceData(
                                substance,
                                substancesGroupConfig,
                                participantVisits,
                                substancesConfig,
                                childAgeInWeeks
                            )
                        )
                    }
                }

                val resultListWithoutDuplicates = substanceDataModelList.distinctBy { it.conceptName }
                var filteredResultList = applyVaccinesCatchUpSchedule(
                    resultListWithoutDuplicates,
                    childAgeInWeeks,
                    participantVisits,
                    substancesGroupConfig,
                    substancesConfig
                ).toMutableList()

                /*
                Additional catchup schedule condition.
                If child is 6+ weeks and has never been vaccinated do now show any vaccines
                from 'higher' visit types than At Birth and 6 weeks regardless of the child's age
                 */
                val hasEverBeenVaccinated = isAnySubstanceApplied(participantVisits)
                if (childAgeInWeeks >= 6 && !hasEverBeenVaccinated) {
                    filteredResultList = filteredResultList.filter { substance ->
                        substance.visitType == Constants.AT_BIRTH_VISIT_TYPE
                                || substance.visitType == Constants.SIX_WEEKS_VISIT_TYPE }
                        .toMutableList()
                }

                return filteredResultList.filter { it.conceptName != "" }
            }

            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun getSubstancesDataForVisitWithGivenDate(
                participantBirthDate: String,
                visitDate: String,
                participantVisits: List<VisitDetail>,
                configurationManager: ConfigurationManager
            ): List<SubstanceDataModel> {
                val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
                val childAgeInWeeks = DateUtil.getFullWeeksBetweenDateAndToday(participantBirthDate)
                val weeksNumberBetweenBirthdateAndVisit = DateUtil.getFullWeeksBetweenDates(participantBirthDate, visitDate)
                val substancesConfig = configurationManager.getSubstancesConfig()
                val substanceDataModelList = mutableListOf<SubstanceDataModel>()
                substancesConfig.forEach { substance ->
                    val minWeekNumber = substance.weeksAfterBirth - substance.weeksAfterBirthLowWindow
                    val maxWeekNumber = substance.weeksAfterBirth + substance.weeksAfterBirthUpWindow
                    if (weeksNumberBetweenBirthdateAndVisit in minWeekNumber..maxWeekNumber &&
                        !isSubstanceAlreadyApplied(participantVisits, substance.conceptName)
                    ) {
                        substanceDataModelList.add(
                            getSingleSubstanceData(
                                substance,
                                substancesGroupConfig,
                                participantVisits,
                                substancesConfig,
                                childAgeInWeeks
                            )
                        )
                    }
                }

                val resultListWithoutDuplicates = substanceDataModelList.distinctBy { it.conceptName }
                val filteredResultList = applyVaccinesCatchUpSchedule(
                    resultListWithoutDuplicates,
                    weeksNumberBetweenBirthdateAndVisit,
                    participantVisits,
                    substancesGroupConfig,
                    substancesConfig
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

                val childAgeInWeeks = DateUtil.getFullWeeksBetweenDateAndToday(participantBirthDate)
                val hasEverBeenVaccinated = isAnySubstanceApplied(participantVisits)

                // If child is < 6 weeks, system should display 'At Birth' visit
                if (childAgeInWeeks < 6) {
                    return visitTypesOrdered[0] // At Birth
                }

                // If child is 6+ weeks and never been vaccinated, system should display '6 weeks' visit
                if (!hasEverBeenVaccinated) {
                    return visitTypesOrdered[1] // 6 weeks
                }

                // If the child has been vaccinated, determine the next visit based on the last visit
                val lastVisit = participantVisits
                    .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
                    .maxByOrNull { it.visitDate }

                return if (lastVisit != null) {
                    val lastVisitType = getVisitTypeFromLastVisit(lastVisit, allSubstancesConfig)
                    val lastVisitIndex = visitTypesOrdered.indexOf(lastVisitType)
                    if (lastVisitIndex < visitTypesOrdered.size - 1) {
                        visitTypesOrdered[lastVisitIndex + 1]
                    } else {
                        "No visit scheduled"
                    }
                } else {
                    findVisitTypeByChildAge(childAgeInWeeks, allSubstancesConfig)
                }
            }

            private fun findVisitTypeByChildAge(childAgeInWeeks: Int, substancesConfig: SubstancesConfig): String {
                val substanceClosestToChildAge = substancesConfig.minByOrNull {
                    abs(it.weeksAfterBirth - childAgeInWeeks)
                }

                return substanceClosestToChildAge?.visitType ?: ""
            }

            @RequiresApi(Build.VERSION_CODES.O)
            suspend fun getVisitTypeForVisitWithGivenDate(
                participantBirthDate: String,
                visitDate: String,
                participantVisits: List<VisitDetail>,
                configurationManager: ConfigurationManager
            ): String {
                val allSubstancesConfig = configurationManager.getSubstancesConfig()
                val visitTypesOrdered = getVisitTypesInOrder(allSubstancesConfig)

                // Find the most recent visit that has occurred
                val lastVisit = participantVisits
                    .filter { it.visitStatus == Constants.VISIT_STATUS_OCCURRED }
                    .maxByOrNull { it.visitDate }

                return if (lastVisit != null) {
                    val lastVisitType = getVisitTypeFromLastVisit(lastVisit, allSubstancesConfig)
                    val lastVisitIndex = visitTypesOrdered.indexOf(lastVisitType)
                    if (lastVisitIndex < visitTypesOrdered.size - 1) {
                        visitTypesOrdered[lastVisitIndex + 1]
                    } else {
                        "No visit scheduled"
                    }
                } else {
                    // If there is no last visit, determine the visit type based on the visit date
                    val weeksNumberBetweenBirthdateAndVisit = DateUtil.getFullWeeksBetweenDates(participantBirthDate, visitDate)
                    findVisitTypeByChildAge(weeksNumberBetweenBirthdateAndVisit, allSubstancesConfig)
                }
            }

            private fun getVisitTypeFromLastVisit(
                lastVisit: VisitDetail,
                allSubstancesConfig: List<Substance>
            ): String {
                val substanceNames = extractSubstanceNamesFromObservations(lastVisit)
                return substanceNames.mapNotNull { substanceName ->
                    allSubstancesConfig.find { it.conceptName == substanceName }?.visitType
                }.maxByOrNull { visitType ->
                    getVisitTypesInOrder(allSubstancesConfig).indexOf(visitType)
                } ?: ""
            }

            fun getVisitTypesInOrder(substances: List<Substance>): List<String> {
                return substances
                    .groupBy { it.weeksAfterBirth }
                    .toSortedMap()
                    .map { it.value.first().visitType }
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
                minimumWeeksNumberAfterPreviousDose: Int?,
                isPreviousDoseNotValid: Boolean
            ): Boolean {

                if (minimumWeeksNumberAfterPreviousDose == null || isPreviousDoseNotValid) {
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
                substancesGroupConfig: SubstancesGroupConfig,
                substancesConfig: SubstancesConfig
            ): List<SubstanceDataModel> {
                return substances.filter { substance ->
                    val previousDoseConceptName = findPreviousDoseName(substance, substancesGroupConfig)
                    val isPreviousDoseNotValid = isVaccineNotValidInTermsOfAge(previousDoseConceptName, substancesConfig, childAgeInWeeks)
                    (substance.maximumAgeInWeeks == null || childAgeInWeeks <= substance.maximumAgeInWeeks)
                            && isTimeIntervalMaintained(previousDoseConceptName, participantVisits,
                        substance.minimumWeeksNumberAfterPreviousDose, isPreviousDoseNotValid)
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
                            otherSubstance.options,
                            otherSubstance.isRequired
                        )
                    )
                }

                return otherSubstancesDataModelList
            }

            private fun getSingleSubstanceData(
                substance: Substance,
                substancesGroupConfig: SubstancesGroupConfig,
                participantVisits: List<VisitDetail>?,
                substancesConfig: SubstancesConfig,
                childAgeInWeeks: Int
            ): SubstanceDataModel {
                val group =
                    substancesGroupConfig.find { substanceGroup -> substanceGroup.substanceName == substance.group }
                val earlierDoses =
                    group?.options?.takeWhile { it != substance.conceptName }?.toMutableList()
                        ?: mutableListOf()

                val iterator = earlierDoses.iterator()
                while (iterator.hasNext()) {
                    val earlierDose = iterator.next()
                    val substanceObject = substancesConfig.find { it.conceptName == earlierDose }
                    if (substanceObject != null) {
                        if (substanceObject.maximumAgeInWeeks != null && childAgeInWeeks > substanceObject.maximumAgeInWeeks) {
                            iterator.remove()
                        }
                    }
                }

                if (group?.options?.contains(substance.conceptName) == true) {
                    earlierDoses.add(substance.conceptName)
                }
                var substanceToBeAdministered = substance.conceptName
                for (item in earlierDoses) {
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
                    substanceToBeAdministeredObject?.routeOfAdministration ?: "",
                    substanceToBeAdministeredObject?.group ?: "",
                    substanceToBeAdministeredObject?.maximumAgeInWeeks,
                    substanceToBeAdministeredObject?.minimumWeeksNumberAfterPreviousDose,
                    substanceToBeAdministeredObject?.visitType
                )
            }

            private fun isSubstanceAlreadyApplied(
                visits: List<VisitDetail>,
                substanceName: String
            ): Boolean {
                return visits.any { visit -> substanceName + " ${Constants.DATE_STR}" in visit.observations }
            }

            private fun isAnySubstanceApplied(visits: List<VisitDetail>): Boolean {
                return visits.any { visit ->
                    visit.observations.keys.any { key ->
                        key.endsWith(Constants.VXNAID_DATE_SUFFIX)
                    }
                }
            }

            private fun findPreviousDoseName(
                substance: SubstanceDataModel,
                substancesGroupConfig: SubstancesGroupConfig
            ): String? {
                val group = substancesGroupConfig.find { it.substanceName == substance.group }
                val index = group?.options?.indexOf(substance.conceptName) ?: return null

                val previousVaccineConceptName = if (index > 0) {
                    group.options[index - 1]
                } else {
                    null
                }

                return previousVaccineConceptName
            }

            private fun isVaccineNotValidInTermsOfAge(vaccineConceptName: String?,
                                                   substancesConfig: SubstancesConfig,
                                                   childAgeInWeeks: Int): Boolean {
                if (vaccineConceptName == null) {
                    return true
                }

                val vaccineObject = substancesConfig.find { it.conceptName == vaccineConceptName }

                return if (vaccineObject?.maximumAgeInWeeks != null) {
                    childAgeInWeeks > vaccineObject.maximumAgeInWeeks
                } else {
                    false
                }
            }
        }
    }
