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
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.dto.VaccineObservationDTO
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
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
            val occurredVisits = visitRepository.findAllVisitsByAttributeTypeAndValue(Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED).filter { participantFromCurrentLocation(it, currentLocationUuid) }

            val draftVisitEncounters = draftVisitEncounterRepository.findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
            val draftVisitEncountersAsVisits = draftVisitEncounters.map { convertDraftVisitEncounterToVisit(it) }

            val allVisits = occurredVisits + draftVisitEncountersAsVisits
            vaccineDTOs.value = createVaccineObservationDTOList(allVisits)
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

    private suspend fun createVaccineObservationDTOList(visits: List<Visit>): List<VaccineObservationDTO> {
        val vaccineObservationDTOList: MutableList<VaccineObservationDTO> = mutableListOf()
        val participantsMap = mutableMapOf<String, ParticipantBase?>()

        visits.forEach { visit ->
            if (!participantsMap.containsKey(visit.participantUuid)) {
                val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                participantsMap[visit.participantUuid] = participant
            }
        }

        val vaccineConceptDateNames = substancesConfig.value!!
            .filter { it.category == Constants.VACCINES_CATEGORY_NAME }
            .map { it.conceptName }

        for (visit in visits) {
            val participant = participantsMap[visit.participantUuid]
            if (participant != null) {
                for ((key, observation) in visit.observations) {
                    val vaccineConceptName = vaccineConceptDateNames.find { key == "$it ${Constants.DATE_STR}" }
                    if (vaccineConceptName != null) {
                        val ageGroup = calculateChildAgeGroup(participant.birthDate)
                        val visitLocation = visit.visitLocation ?: Constants.ALL_STRING
                        val attachedClinic = visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC]
                        val dto = VaccineObservationDTO(vaccineConceptName, observation.value, visitLocation, ageGroup, attachedClinic)
                        if (!vaccineObservationDTOList.contains(dto)) { // Avoid duplicates
                            vaccineObservationDTOList.add(dto)
                        }
                    }
                }
            }
        }

        return vaccineObservationDTOList
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

    private suspend fun participantFromCurrentLocation(visit: Visit, locationUuid: String?): Boolean {
        val participant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
        return if (participant == null) {
            Log.w("VaccinesiewModel", "Participant not found")
            false
        } else {
            Log.d("VaccinesViewModel", "Participant found")
            participant.locationUuid == locationUuid
        }
    }

    enum class Screen(@StringRes val title: Int) {
        VACCINES_OVERVIEW(R.string.vaccines_overview_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}
