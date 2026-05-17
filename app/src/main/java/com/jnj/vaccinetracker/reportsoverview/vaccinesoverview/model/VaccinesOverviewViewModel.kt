package com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.model

import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.addDaysToDate
import com.jnj.vaccinetracker.common.data.database.typealiases.getTodayMidnight
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.dto.VaccineObservationDTO
import com.soywiz.klock.DateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class VaccinesOverviewViewModel @Inject constructor(
    userRepository: UserRepository,
    private val configurationManager: ConfigurationManager,
    private val visitRepository: VisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val vaccineDTOs = mutableLiveData<List<VaccineObservationDTO>>()
    val attachedClinics = MutableLiveData<List<String>>(emptyList())
    val parentSiteName = MutableLiveData<String?>(null)
    val substancesConfig = mutableLiveData<SubstancesConfig>(emptyList())
    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    private var screens = listOf<Screen>()
    val isLoading = mutableLiveData<Boolean>()
    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    fun loadAttachedClinics() {
        viewModelScope.launch {
            try {
                val allSites = configurationManager.getSites()
                parentSiteName.value = allSites.find { it.uuid == currentLocationUuid }?.name
                val clinicNames = allSites
                    .filter { it.parentLocationUuid == currentLocationUuid }
                    .map { it.name }
                attachedClinics.value = clinicNames
            } catch (e: Exception) {
                Log.e("VaccinesOverviewVM", "Failed to load attached clinics", e)
                attachedClinics.value = emptyList()
            }
        }
    }

    init {
        initScreens()
    }

    suspend fun getSubstancesConfig() : SubstancesConfig {
        return configurationManager.getSubstancesConfig()
    }

    fun getVaccinesData() {
        isLoading.value = true
        viewModelScope.launch {
            val result = withContext(dispatchers.io) {
                val config = configurationManager.getSubstancesConfig()
                val conceptDateNames = config
                    .filter { it.category == Constants.VACCINES_CATEGORY_NAME }
                    .map { it.conceptName } + ChildHealthPlusService.values().map { it.conceptName }

                val syncedDeferred = async {
                    visitRepository.findAllVisitsByAttributeTypeAndValue(
                        Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED
                    )
                }
                val draftDeferred = async {
                    draftVisitEncounterRepository
                        .findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                        .map { convertDraftVisitEncounterToVisit(it) }
                }
                val allVisits = syncedDeferred.await() + draftDeferred.await()

                val participantUuids = allVisits.mapTo(mutableSetOf()) { it.participantUuid }
                val participantsMap = participantUuids
                    .map { uuid -> async { uuid to findParticipantByParticipantUuidUseCase.findByParticipantUuid(uuid) } }
                    .awaitAll()
                    .toMap()

                val dtos = mutableListOf<VaccineObservationDTO>()
                val seenVisitObservations = mutableSetOf<Pair<String, String>>()
                for (visit in allVisits) {
                    val participant = participantsMap[visit.participantUuid] ?: continue
                    if (participant.locationUuid != currentLocationUuid) continue
                    for ((key, observation) in visit.observations) {
                        if (!seenVisitObservations.add(visit.visitUuid to key)) continue
                        val conceptName = conceptDateNames.find { key == "$it ${Constants.DATE_STR}" } ?: continue
                        dtos.add(VaccineObservationDTO(
                            vaccineName    = conceptName,
                            administerDate = observation.value,
                            visitLocation  = visit.visitLocation ?: Constants.ALL_STRING,
                            ageGroup       = calculateChildAgeGroup(participant.birthDate),
                            attachedClinic = visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC]
                        ))
                    }
                }
                dtos
            }
            vaccineDTOs.value = result
            isLoading.value = false
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

    private fun initScreens() {
        screens = createScreens()
        setInitialScreen()
    }

    private fun createScreens(): List<Screen> {
        return mutableListOf(Screen.VACCINES_OVERVIEW)
    }

    private fun setInitialScreen() {
        if (currentScreen.get() == null) {
            val screen = screens.firstOrNull()
            currentScreen.set(screen)
        }
    }

    enum class Screen(@StringRes val title: Int) {
        VACCINES_OVERVIEW(R.string.vaccines_overview_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}
