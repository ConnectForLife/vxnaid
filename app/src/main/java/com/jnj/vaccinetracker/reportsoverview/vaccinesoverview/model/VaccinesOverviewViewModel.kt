package com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.model

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.VisitRepository
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.SubstancesConfig
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.dto.VaccineObservationDTO
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import javax.inject.Inject

class VaccinesOverviewViewModel @Inject constructor(
    private val configurationManager: ConfigurationManager,
    private val visitRepository: VisitRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val vaccineDTOs = mutableLiveData<List<VaccineObservationDTO>>()
    val substancesConfig = mutableLiveData<SubstancesConfig>(emptyList())
    val currentScreen = mutableLiveData<Screen>()
    var navigationDirection = NavigationDirection.NONE
    private var screens = listOf<Screen>()
    val isLoading = mutableLiveData<Boolean>()

    init {
        initScreens()
    }

    suspend fun getSubstancesConfig() : SubstancesConfig {
        return configurationManager.getSubstancesConfig()
    }

    fun getVaccinesData() {
        isLoading.value = true
        viewModelScope.launch {
            val occurredVisits = visitRepository.findAllVisitsByAttributeTypeAndValue(Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED)
            vaccineDTOs.value = createVaccineObservationDTOList(occurredVisits)
            isLoading.value = false
        }

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
            val participant  = participantsMap[visit.participantUuid]
            if (participant != null) {
                for ((key, observation) in visit.observations) {
                    val vaccineConceptName = vaccineConceptDateNames.find { key == "$it ${Constants.DATE_STR}" }
                    if (vaccineConceptName != null) {
                        val ageGroup = calculateChildAgeGroup(participant.birthDate)
                        vaccineObservationDTOList.add(VaccineObservationDTO(vaccineConceptName, observation.value, visit.visitLocation, ageGroup))
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

    enum class Screen(@StringRes val title: Int) {
        VACCINES_OVERVIEW(R.string.vaccines_overview_title)
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}