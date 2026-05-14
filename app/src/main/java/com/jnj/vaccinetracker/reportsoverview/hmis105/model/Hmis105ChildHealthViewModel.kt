package com.jnj.vaccinetracker.reportsoverview.hmis105.model

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
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.ParticipantBase
import com.jnj.vaccinetracker.common.domain.entities.Visit
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ChildHealthObservationDTO
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

class Hmis105ChildHealthViewModel @Inject constructor(
    userRepository: UserRepository,
    private val visitRepository: VisitRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {

    val observationDTOs = mutableLiveData<List<Hmis105ChildHealthObservationDTO>>(emptyList())
    val isLoading = mutableLiveData<Boolean>(false)
    val currentScreen = mutableLiveData<Screen>()
    val selectedStartDate = MutableLiveData<DateTime?>(null)
    val selectedEndDate = MutableLiveData<DateTime?>(null)
    var navigationDirection = NavigationDirection.NONE

    private val currentLocationUuid = userRepository.getDeviceNameSiteUuid()

    companion object {
        private const val KEY_VITAMIN_A = "Vitamin A Vxnaid Date"
        private const val KEY_DEWORMING = "Deworming Vxnaid Date"

        private const val DOSE_CH01 = "CH01 Vitamin A (Dose 1)"
        private const val DOSE_CH02 = "CH02 Vitamin A (Dose 2)"
        private const val DOSE_CH03 = "CH03 Dewormed (Dose 1)"
        private const val DOSE_CH04 = "CH04 Dewormed (Dose 2)"

        private val ALL_LOCATIONS = listOf(
            Constants.VISIT_PLACE_STATIC,
            Constants.VISIT_PLACE_OUTREACH,
            Constants.VISIT_PLACE_SCHOOL
        )
    }

    init {
        val screens = listOf(Screen.HMIS105_CHILD_HEALTH)
        if (currentScreen.get() == null) currentScreen.set(screens.firstOrNull())
    }

    fun getChildHealthData() {
        isLoading.value = true
        viewModelScope.launch {
            val result = withContext(dispatchers.io) {
                try {
                    val syncedVisits = visitRepository.findAllVisitsByAttributeTypeAndValue(
                        Constants.ATTRIBUTE_VISIT_STATUS, Constants.VISIT_STATUS_OCCURRED
                    )
                    val draftVisits = draftVisitEncounterRepository
                        .findVisitsBeforeDate(addDaysToDate(getTodayMidnight(), 1))
                        .map { convertDraftVisitEncounterToVisit(it) }

                    val allVisits = syncedVisits + draftVisits

                    val participantsMap = mutableMapOf<String, ParticipantBase?>()
                    for (visit in allVisits) {
                        if (!participantsMap.containsKey(visit.participantUuid)) {
                            participantsMap[visit.participantUuid] =
                                findParticipantByParticipantUuidUseCase.findByParticipantUuid(visit.participantUuid)
                        }
                    }

                    val dtos = mutableListOf<Hmis105ChildHealthObservationDTO>()
                    val nowDateTime = DateTime.now()

                    for (visit in allVisits) {
                        val participant = participantsMap[visit.participantUuid] ?: continue
                        if (participant.locationUuid != currentLocationUuid) continue

                        val ageInMonths = calculateAgeInMonths(participant.birthDate, nowDateTime)
                        val ageInYears = ageInMonths / 12
                        val visitLocation = visit.visitLocation.takeIf { it in ALL_LOCATIONS }
                            ?: Constants.VISIT_PLACE_STATIC

                        for ((key, obsValue) in visit.observations) {
                            val dose = when {
                                key == KEY_VITAMIN_A && ageInMonths in 0..11  -> DOSE_CH01
                                key == KEY_VITAMIN_A && ageInMonths in 12..59 -> DOSE_CH02
                                key == KEY_DEWORMING && ageInMonths in 12..59 -> DOSE_CH03
                                key == KEY_DEWORMING && ageInYears  in 5..14  -> DOSE_CH04
                                else -> continue
                            }
                            val ageGroup = when {
                                ageInMonths in 0..11  -> Constants.GROUP_AGE_FIRST
                                ageInMonths in 12..59 -> Constants.GROUP_AGE_SECOND
                                else                  -> Constants.GROUP_AGE_THIRD
                            }
                            val dto = Hmis105ChildHealthObservationDTO(
                                dose          = dose,
                                administerDate = obsValue.value,
                                visitLocation  = visitLocation,
                                ageGroup       = ageGroup,
                                gender         = participant.gender
                            )
                            if (!dtos.contains(dto)) dtos.add(dto)
                        }
                    }
                    dtos
                } catch (ex: Exception) {
                    Log.e("Hmis105ChildHealthVM", "Error loading data", ex)
                    emptyList()
                }
            }
            observationDTOs.value = result
            isLoading.value = false
        }
    }

    private fun convertDraftVisitEncounterToVisit(draft: DraftVisitEncounter): Visit {
        return Visit(
            visitUuid       = draft.visitUuid,
            startDatetime   = draft.startDatetime,
            visitType       = draft.visitType,
            participantUuid = draft.participantUuid,
            attributes      = draft.attributes,
            observations    = draft.observations.mapValues { (_, v) ->
                ObservationValue(v, draft.startDatetime)
            },
            dateModified    = Date(System.currentTimeMillis())
        )
    }

    private fun calculateAgeInMonths(birthDate: BirthDate, referenceDate: DateTime): Int {
        val birth = birthDate.toDateTime()
        return (referenceDate.yearInt - birth.yearInt) * 12 +
                (referenceDate.month0 - birth.month0)
    }

    enum class Screen(@StringRes val label: Int) {
        HMIS105_CHILD_HEALTH(R.string.hmis105_child_health_report_title)
    }

    override fun saveInstanceState(outState: Bundle) {
        selectedStartDate.value?.let { outState.putString("selectedStartDate", it.toString()) }
        selectedEndDate.value?.let   { outState.putString("selectedEndDate",   it.toString()) }
    }

    override fun restoreInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.getString("selectedStartDate")?.let {
            try { selectedStartDate.value = DateTime.parse(it).local } catch (_: Exception) {}
        }
        savedInstanceState.getString("selectedEndDate")?.let {
            try { selectedEndDate.value = DateTime.parse(it).local } catch (_: Exception) {}
        }
    }
}
