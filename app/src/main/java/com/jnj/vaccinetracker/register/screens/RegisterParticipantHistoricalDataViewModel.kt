package com.jnj.vaccinetracker.register.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.RegisterParticipant
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.helpers.uuid
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.model.HistoricalData
import com.jnj.vaccinetracker.register.model.SubstancesData
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalDate
import java.util.Date
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantHistoricalDataViewModel @Inject constructor(
   override val dispatchers: AppCoroutineDispatchers,
   private val visitManager: VisitManager,
   private val sessionExpiryObserver: SessionExpiryObserver,
   private val createVisitUseCase: CreateVisitUseCase,
   private val userRepository: UserRepository,
   private val syncSettingsRepository: SyncSettingsRepository
) : ViewModelBase() {

   val registerVaccinesSuccessEvents = eventFlow<ParticipantSummaryUiModel>()
   val loading = mutableLiveBoolean()
   val errorMessage = mutableLiveData<String>()
   private val participantArg = stateFlow<RegisterParticipant?>(null)
   private val participantSummaryArg = stateFlow<ParticipantSummaryUiModel?>(null)
   val registerParticipant = mutableLiveData<RegisterParticipant>()
   val participant = mutableLiveData<ParticipantSummaryUiModel>()
   private val dosingVisit = mutableLiveData<VisitDetail>()
   val isEdit = mutableLiveData<Boolean>(false)
   val groupedVisitsByType = mutableLiveData<Map<String, List<VisitDetail>>>()
   val visitTypesData = mutableLiveData<MutableMap<String, HistoricalData>>(mutableMapOf())
   private val _historicalVisitDates = MutableLiveData<List<DateTime>>(emptyList())
   val historicalVisitDates: LiveData<List<DateTime>> get() = _historicalVisitDates
   private val _disabledDates = MutableLiveData<MutableSet<Long>>().apply {
      value = mutableSetOf()
   }
   private val disabledDates: LiveData<MutableSet<Long>> = _disabledDates
   private val allDisabledDates = mutableSetOf<Long>()
   fun getAllDisabledDates(): Set<Long> = allDisabledDates
   private val _actionLiveData = MutableLiveData<String>()
   val actionLiveData: LiveData<String> = _actionLiveData

   private val atBirthVisitDate = mutableLiveData<DateTime?>()

   fun isDateValidForVisitType(visitType: String, selectedDate: DateTime): Boolean {
      if (visitType == "At Birth") return true
      val birthDate = atBirthVisitDate.value ?: return true
      return selectedDate >= birthDate
   }

   fun setAtBirthVisitDate(date: DateTime) {
      atBirthVisitDate.value = date
   }

   private val disabledDatesByVisitType: MutableMap<String, Set<Long>> = mutableMapOf()

   fun getDisabledDatesForVisitType(visitType: String): Set<Long> {
      return if (visitType == "At Birth") {
         emptySet()
      } else {
         disabledDatesByVisitType[visitType] ?: emptySet()
      }
   }


   fun setDisabledDatesForVisitType(visitType: String, dates: Set<Long>) {
      if (visitType != "At Birth") {
         disabledDatesByVisitType[visitType] = dates
      }
   }

   fun addDisabledDate(date: Long) {
      allDisabledDates.add(date)
   }

   fun setAllDisabledDates(dates: Set<Long>) {
      allDisabledDates.clear()
      allDisabledDates.addAll(dates)
   }


   init {
      participantSummaryArg
         .filterNotNull()
         .distinctUntilChanged()
         .onEach {
            participant.value = it
            isEdit.value = true
            loadOnEdit()
         }
         .launchIn(scope)

      participantArg
         .filterNotNull()
         .distinctUntilChanged()
         .onEach {
            registerParticipant.value = it
            isEdit.value = false
         }
         .launchIn(scope)
   }

   suspend fun loadOnEdit() {
      loading.set(true)
      try {
         val visits = visitManager.getVisitsForParticipant(participantUuid = participant.value!!.participantUuid)

         val filteredVisits = visits.filter { visit ->
            visit.visitStatus == Constants.VISIT_STATUS_OCCURRED && visit.visitType == Constants.VISIT_TYPE_DOSING
         }

         groupedVisitsByType.value = filteredVisits.groupBy { visit ->
            visit.attributes.getOrElse(Constants.ATTRIBUTE_VISIT_TYPE_VXNAID) { "UNKNOWN" }.toString()
         }

         loading.set(false)
      } catch (ex: Throwable) {
         yield()
         ex.rethrowIfFatal()
         loading.set(false)
         logError("Failed to get visits for participant: ", ex)
      }
   }

   // Extension function to convert a Long timestamp to midnight time
   private fun Long.toMidnight(): Long {
      val cal = java.util.Calendar.getInstance()
      cal.timeInMillis = this
      cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
      cal.set(java.util.Calendar.MINUTE, 0)
      cal.set(java.util.Calendar.SECOND, 0)
      cal.set(java.util.Calendar.MILLISECOND, 0)
      return cal.timeInMillis
   }


   private fun buildHistoricalVisitObject(
      participant: ParticipantSummaryUiModel,
      visitType: String,
      visitDate: Date
   ): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      return CreateVisit(
         participantUuid = participant.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = visitDate,
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitType,
         )
      )
   }

   @RequiresApi(Build.VERSION_CODES.O)
   suspend fun createHistoricalVisit(
      participant: ParticipantSummaryUiModel,
      visitType: String,
      visitUuid: String,
      visitDate: Date
   ) {
      createVisitUseCase.createVisitWithGivenUuid(buildHistoricalVisitObject(
         participant, visitType, visitDate), visitUuid)
   }

   private suspend fun doRegisterVisit(visitType: String, historicalData: HistoricalData) {
      val participant = participant.value ?: run {
         logError("No participant available.")
         return
      }

      val visits = visitManager.getVisitsForParticipant(participant.participantUuid)
      onVisitsLoaded(visits)

      val dosingVisit = dosingVisit.value ?: run {
         logError("No dosing visit available.")
         return
      }

      val substanceObservations = historicalData.value[Constants.SUBSTANCES_AND_DATES_STR]?.substanceValueMap?.mapValues {
         mapOf(
            Constants.DATE_STR to it.value,
            Constants.MANUFACTURER_NAME_STR to "",
            Constants.BARCODE_STR to ""
         )
      }?.toMutableMap() ?: mutableMapOf()

      val otherSubstancesAndValues = historicalData.value[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]?.substanceValueMap ?: mutableMapOf()
      val visitUuid = uuid()
      val visitDate = historicalData.visitDate ?: Date()
      createHistoricalVisit(participant, visitType, visitUuid, visitDate)

      loading.set(true)

      scope.launch {
         try {
            visitManager.registerDosingVisit(
               encounterDatetime = visitDate,
               visitUuid = visitUuid,
               participantUuid = participant.participantUuid,
               dosingNumber = dosingVisit.dosingNumber ?: 0,
               substanceObservations = substanceObservations,
               otherSubstanceObservations = otherSubstancesAndValues,
               visitTypeVxnaid = visitType
            )
         } catch (ex: OperatorUuidNotAvailableException) {
            sessionExpiryObserver.notifySessionExpired()
         } catch (throwable: Throwable) {
            throwable.rethrowIfFatal()
            logError("Failed to register dosing visit: ", throwable)
         } finally {
            loading.set(false)
         }
      }
   }

   private fun onVisitsLoaded(visits: List<VisitDetail>) {
      dosingVisit.value = visits.firstOrNull()
   }

   suspend fun submitVaccineRegistration() {
      val visitData = visitTypesData.value ?: run {
         logError("No visit types data available.")
         return
      }

      visitData.forEach { (visitType, historicalData) ->
         doRegisterVisit(visitType, historicalData)
      }

      participant.value?.let {
         registerVaccinesSuccessEvents.tryEmit(it)
      } ?: logError("No participant to emit success event.")
   }

   fun setArguments(participant: RegisterParticipant?, participantSummary: ParticipantSummaryUiModel?) {
      participantArg.value = participant
      participantSummaryArg.value = participantSummary
   }

   fun addVisitTypeData(
      visitTypeName: String,
      substancesAndDates: MutableMap<String, String>?,
      otherSubstancesAndValues: MutableMap<String, String>,
      visitDate: DateTime?
   ) {
      val currentData = visitTypesData.value ?: mutableMapOf()
      val visitTypeEntry = currentData.getOrPut(visitTypeName) { HistoricalData(null, emptyMap()) }
      val substances = SubstancesData(substancesAndDates?.toMap() ?: emptyMap())
      val otherSubstances = SubstancesData(otherSubstancesAndValues.toMap())
      val updatedVisitTypeEntry = visitTypeEntry.copy(
         visitDate = findVisitDate(visitDate, substances.substanceValueMap),
         value = mapOf(
            Constants.SUBSTANCES_AND_DATES_STR to substances,
            Constants.OTHER_SUBSTANCES_AND_VALUES_STR to otherSubstances
         )
      )
      currentData[visitTypeName] = updatedVisitTypeEntry


      visitTypesData.postValue(currentData)
   }

   fun getParticipantBirthDate(): String {
      return registerParticipant.value?.birthDate?.birthDateToString() ?: participant.value!!.birthDateText
   }

   private fun findVisitDate(visitDate: DateTime?, vaccines: Map<String, String>): Date {
      if (visitDate != null) {
         return visitDate.toDate()
      }

      if (vaccines.isNotEmpty()) {
         val parsedDates = vaccines.values.mapNotNull { dateString ->
            DateUtil.convertStringToDate(dateString, "yyyy-MM-dd")
         }
         return parsedDates.minOrNull()!!
      }

      return Date()
   }

   fun setHistoricalVisitDate(date: DateTime) {
        _historicalVisitDates.value = _historicalVisitDates.value?.plus(date) ?: listOf(date)
        Log.d("HistoricalVisit", "Set historical visit date: $date")

   }
}
