package com.jnj.vaccinetracker.visit

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.dateDayStart
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantImageUiModel.Companion.toUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for visit screen.
 *
 * @author maartenvangiel
 * @author druelens
 * @version 2
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitViewModel @Inject constructor(
    private val participantManager: ParticipantManager,
    private val visitManager: VisitManager,
    private val configurationManager: ConfigurationManager,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val createVisitUseCase: CreateVisitUseCase,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    ) : ViewModelBase() {

    /**
     * emits when event submission finished
     */
    val visitEvents = eventFlow<Boolean>()
    private val retryClickEvents = eventFlow<Unit>()
    private val participantArg = stateFlow<ParticipantSummaryUiModel?>(null)
    val loading = mutableLiveBoolean()
    val participant = mutableLiveData<ParticipantSummaryUiModel>()
    val participantImage = mutableLiveData<ParticipantImageUiModel>()
    val dosingVisit = mutableLiveData<VisitDetail>()
    val dosingVisitIsInsideTimeWindow = mutableLiveBoolean(true)
    val previousDosingVisits = mutableLiveData<List<VisitDetail>>()
    val errorMessage = mutableLiveData<String>()
    val upcomingVisit = mutableLiveData<UpcomingVisit?>()

    var suggestedSubstancesData = MutableLiveData(listOf<SubstanceDataModel>())
    var selectedSubstancesData = MutableLiveData(listOf<SubstanceDataModel>())
    var substancesDataAll = MutableLiveData(listOf<SubstanceDataModel>())
    var selectedSubstancesWithBarcodes = MutableLiveData<MutableMap<String, Map<String, String>>>(mutableMapOf())
    var selectedOtherSubstances = MutableLiveData<MutableMap<String, String>>()
    var otherSubstancesData =  MutableLiveData<List<OtherSubstanceDataModel>>(listOf())
    var suggestedOtherSubstancesData =  MutableLiveData<List<OtherSubstanceDataModel>>(listOf())
    var checkOtherSubstances =  MutableLiveData(false)
    var isAnyOtherSubstancesEmpty =  MutableLiveData(false)
    var visitsCounter = MutableLiveData(0)
    val patientVisits = MutableLiveData<List<VisitDetail>>(listOf())

    var isSuggesting =  MutableLiveData(true)
    var selectedVisitType =  MutableLiveData<String>()
    var suggestedVisitType =  MutableLiveData<String>()
    var visitTypes =  MutableLiveData<List<String>>()
    var missingSubstancesVisitDate = MutableLiveData<Date>(null)
    var contraindicationsRescheduleDate = MutableLiveData<DateTime>(null)
    var contraindicationsRescheduleReasonText = MutableLiveData<String>(null)

    val selectedVisitDate = MutableLiveData(Date())

    val selectedVisitDateDisplay = selectedVisitDate.map { date ->
        formatDateForDisplay(date)
    }

    val nextVisitPreviewText = selectedVisitDate.map { selectedDate ->
        calculateNextVisitPreview(selectedDate)
    }

    init {
        initState()
    }

    private suspend fun loadImage(participantSummary: ParticipantSummaryUiModel) {
        // If the picture is already loaded, don't need to load again
        if (participant.value == participantSummary && participantImage.get() != null) return
        // If we already have the picture (from match or registration), don't need to query it again
        if (participantSummary.participantPicture != null) {
            participantImage.set(participantSummary.participantPicture)
            return
        }

        try {
            val bytes = participantManager.getPersonImage(participantSummary.participantUuid)
            participantImage.value = bytes.toUiModel()
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            logWarn("Failed to get person image: ", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun load(participantSummary: ParticipantSummaryUiModel) {
        try {
            visitTypes.value = configurationManager.getSubstancesConfig().map { it.visitType }.distinct()

            patientVisits.value = visitManager.getVisitsForParticipant(participantSummary.participantUuid)
            visitsCounter.value = patientVisits.value?.count()

            val suggestedVisitTypeFromConfig = SubstancesDataUtil.getVisitTypeForCurrentVisit(
                participantSummary.birthDateText,
                patientVisits.value!!,
                configurationManager)
            suggestedVisitType.value = suggestedVisitTypeFromConfig
            selectedVisitType.value = suggestedVisitTypeFromConfig

            val suggestedSubstancesFromConfig = SubstancesDataUtil.getSubstancesDataForCurrentVisit(
                participantSummary.birthDateText,
                patientVisits.value!!,
                configurationManager
            )
            suggestedSubstancesData.value = suggestedSubstancesFromConfig
            selectedSubstancesData.value = suggestedSubstancesFromConfig

            substancesDataAll.value = SubstancesDataUtil.getAllSubstances(configurationManager)

            val otherSubstancesList = SubstancesDataUtil.getOtherSubstancesDataForVisitType(
                suggestedVisitTypeFromConfig,
                configurationManager
            )
            val filteredOtherSubstancesList = filterOtherSubstancesByLLIN(otherSubstancesList)

            otherSubstancesData.value = filteredOtherSubstancesList
            suggestedOtherSubstancesData.value = otherSubstancesData.value

            onVisitsLoaded(patientVisits.value!!)
        } catch (ex: Throwable) {
            yield()
            ex.rethrowIfFatal()
            errorMessage.set(resourcesWrapper.getString(R.string.general_label_error))
            logError("Failed to load visits for participant: ", ex)
        } finally {
            loading.set(false)
        }

        loadImage(participantSummary)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initState() {
        loading.set(true)
        errorMessage.set(null)
        participantArg.filterNotNull().onEach { participant ->
            this.participant.value = participant
        }.combine(retryClickEvents.asFlow()) { participant, _ ->
            loading.set(true)
            errorMessage.set(null)
            load(participant)
        }.launchIn(scope)

        retryClickEvents.tryEmit(Unit)
    }

    fun setArguments(participant: ParticipantSummaryUiModel) {
        this.participantArg.value = participant
    }

    fun onRetryClick() {
        retryClickEvents.tryEmit(Unit)
    }

    /**
     * Triggered when visits are loaded from manager.
     * Will set the previous visits for the dosing history and find the current open dosing visit.
     * Checks if the current time is in the dosing visit window for the current open dosing visit.
     *
     * @param visits    List of VisitDetail objects of the retrieved visits for this participant
     */
    private fun onVisitsLoaded(visits: List<VisitDetail>) {
        previousDosingVisits.set(visits.findPreviousDosingVisits())

        val foundDosingVisit = visits.findDosingVisit()
        dosingVisit.set(foundDosingVisit)

        foundDosingVisit?.let { visit ->
            val now = Calendar.getInstance().timeInMillis
            val startTime = visit.startDate.dateDayStart.time
            val endTime = visit.endDate.dateDayStart.time + 1.days
            val insideTimeWindow = now >= startTime && now <= endTime
            logInfo("insideTimeWindow: $insideTimeWindow")
            dosingVisitIsInsideTimeWindow.set(insideTimeWindow)
        }
    }


    /**
     * Find previously completed dosing visits, ordered by dosing number
     */
    private fun List<VisitDetail>.findPreviousDosingVisits(): List<VisitDetail> {
        val previousDosingVisits = mutableListOf<VisitDetail>()

        map { visit ->
            if (visit.visitType == Constants.VISIT_TYPE_DOSING && visit.visitStatus == Constants.VISIT_STATUS_OCCURRED)
                previousDosingVisits.add(visit)
        }

        return previousDosingVisits.sortedBy { it.dosingNumber }

    }

    /**
     * Submit a dosing visit encounter
     *
     * @param newVisitDate  date of next visit
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun submitDosingVisit(newVisitDate: Date? = null, visitPlace: String? = null,
                          referralObservations: Map<String, String> = emptyMap(), outreachName: String? = null,
                          attachedClinic: String? = null) {
        val participant = participant.get()
        val dosingVisit = dosingVisit.get()
        val visitsCounter = visitsCounter.value
        val selectedVisitType = selectedVisitType.value
        val substancesObservations = selectedSubstancesWithBarcodes.value ?: mapOf()
        val otherSubstancesObservations = selectedOtherSubstances.value ?: mapOf()

        if (participant == null || dosingVisit == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return
        }

        loading.set(true)

        scope.launch {
            try {
                visitManager.registerDosingVisit(
                    encounterDatetime = selectedVisitDate.value ?: Date(),
                    visitUuid = dosingVisit.uuid,
                    participantUuid = participant.participantUuid,
                    dosingNumber = visitsCounter ?: 0,
                    substanceObservations = substancesObservations.toMap(),
                    otherSubstanceObservations = otherSubstancesObservations.toMap(),
                    visitLocation = visitPlace,
                    visitTypeVxnaid = selectedVisitType,
                    referralObservations = referralObservations,
                    visitOutreachName = outreachName,
                    attachedClinic = attachedClinic,
                )

                if (newVisitDate != null) {
                    createNextVisit(participant, newVisitDate)
                }

                onVisitLogged()
                loading.set(false)
                visitEvents.tryEmit(true)
            } catch (ex: OperatorUuidNotAvailableException) {
                loading.set(false)
                sessionExpiryObserver.notifySessionExpired()
            } catch (throwable: Throwable) {
                yield()
                throwable.rethrowIfFatal()
                loading.set(false)
                logError("Failed to register dosing visit: ", throwable)
                visitEvents.tryEmit(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun validateDosingVisit(
        missingSubstancesListener: (List<String>) -> Unit,
    ): Boolean {
        val participant = participant.get()
        val dosingVisit = dosingVisit.get()
        val missingSubstances = getMissingSubstanceLabels()

        if (participant == null || dosingVisit == null) {
            logError("No participant or dosing visit in memory!")
            visitEvents.tryEmit(false)
            return false
        }

        if (missingSubstances.isNotEmpty()) {
            missingSubstancesListener(missingSubstances)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createNextVisit(participant: ParticipantSummaryUiModel, newVisitDate: Date) {
        createVisitUseCase.createVisit(buildVisitObject(participant, newVisitDate))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getNextVisitDate(participant: ParticipantSummaryUiModel): Date? {
        val weeksNumberAfterBirthForNextVisit = findWeeksNumberAfterBirthForNextVisit(participant.birthDateText)
        return weeksNumberAfterBirthForNextVisit?.let { calculateNextVisitDate(participant.birthDateText, it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun findWeeksNumberAfterBirthForNextVisit(participantBirthDate: String): Int? {
        val substancesConfig = configurationManager.getSubstancesConfig()
        val weeksAfterBirthSet = substancesConfig.map { it.weeksAfterBirth }.sorted().toSet()
        val childAgeInWeeks = DateUtil.getFullWeeksBetweenDateAndToday(participantBirthDate)

        return substancesConfig
            .filter {
                val minWeekNumber = it.weeksAfterBirth - it.weeksAfterBirthLowWindow
                val maxWeekNumber = it.weeksAfterBirth + it.weeksAfterBirthUpWindow
                childAgeInWeeks in minWeekNumber..maxWeekNumber
            }.firstNotNullOfOrNull {
                weeksAfterBirthSet.filter { week -> week > it.weeksAfterBirth }.minOrNull()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun buildVisitObject(participant: ParticipantSummaryUiModel, visitDate: Date): CreateVisit {
        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
        val locationUuid = getLocationUuid()
        val visitType = findVisitType(participant, visitDate)

        return CreateVisit(
            participantUuid = participant.participantUuid,
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = visitDate,
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitType,
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateNextVisitDate(birthDateText: String, weeksNumberAfterBirth: Int): Date {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val birthDate = LocalDate.parse(birthDateText, formatter)
        val nextVisitDate = birthDate.plusWeeks(weeksNumberAfterBirth.toLong())
        return Date.from(nextVisitDate.atStartOfDay(ZoneId.of(Constants.UTC_TIME_ZONE_NAME)).toInstant())
    }

    private suspend fun onVisitLogged() {
        val participantUuid = participant.value?.participantUuid
        logInfo("onVisitLogged: participantUuid=$participantUuid")
        upcomingVisit.value = if (participantUuid != null) {
            try {
                visitManager.getUpcomingVisit(participantUuid)
            } catch (ex: Exception) {
                yield()
                ex.rethrowIfFatal()
                logError("Failed to get upcoming visit", ex)
                null
            }
        } else {
            logWarn("error participantUuid not available to get upcoming visit")
            null
        }
    }

    fun addObsToObsMap(conceptName: String, barcode: String, manufacturerName: String) {
        val currentMap = selectedSubstancesWithBarcodes.value?.toMutableMap() ?: mutableMapOf()
        currentMap[conceptName] = mapOf(Constants.BARCODE_STR to barcode, Constants.MANUFACTURER_NAME_STR to manufacturerName)
        selectedSubstancesWithBarcodes.postValue(currentMap)
    }

    fun addObsToOtherSubstancesObsMap(conceptName: String, value: String) {
        val currentMap = selectedOtherSubstances.value?.toMutableMap() ?: mutableMapOf()
        currentMap[conceptName] = value
        selectedOtherSubstances.postValue(currentMap)
    }

    fun removeObsFromMap(conceptName: String) {
        val currentMap = selectedSubstancesWithBarcodes.value?.toMutableMap() ?: mutableMapOf()
        currentMap.remove(conceptName)
        selectedSubstancesWithBarcodes.postValue(currentMap)
    }

    private fun getMissingSubstanceLabels(): List<String> {
        val selectedConceptNames = selectedSubstancesWithBarcodes.value?.keys?.toSet() ?: setOf()

        return suggestedSubstancesData.value
            ?.filter { it.conceptName !in selectedConceptNames }
           ?.map { it.label } ?: listOf()
    }

    fun checkIfAnyOtherSubstancesEmpty() {
        checkOtherSubstances.value = true
    }

    fun setIsSuggesting(checked: Boolean) {
        if (checked == isSuggesting.value) return
        isSuggesting.value = checked
        selectedSubstancesData.value = suggestedSubstancesData.value
        selectedVisitType.value = suggestedVisitType.value
        otherSubstancesData.value = this.suggestedOtherSubstancesData.value
    }

    fun addToSelectedSubstances(vaccine: SubstanceDataModel) {
        selectedSubstancesData.value = selectedSubstancesData.value?.plus(vaccine)
    }

    fun removeFromSelectedSubstances(vaccine: SubstanceDataModel) {
        selectedSubstancesData.value = selectedSubstancesData.value?.minus(vaccine)
    }

    suspend fun onVisitTypeDropdownChange() {
        val selectedType = selectedVisitType.value ?: ""
        val substancesForVisitType = SubstancesDataUtil.getSubstancesDataForVisitType(
            selectedType,
            configurationManager
        )

        val birthDate = participant.value?.birthDateText
        selectedSubstancesData.value = if (birthDate != null) {
            val childAgeInWeeks = DateUtil.getFullWeeksBetweenDateAndToday(birthDate)
            val substancesConfig = configurationManager.getSubstancesConfig()
            val substancesGroupConfig = configurationManager.getSubstancesGroupConfig()
            val ageFiltered = substancesForVisitType.filter { substance ->
                val config = substancesConfig.find { it.conceptName == substance.conceptName }
                    ?: return@filter true
                val minWeeks = config.weeksAfterBirth - config.weeksAfterBirthLowWindow
                val maxWeeks = config.weeksAfterBirth + config.weeksAfterBirthUpWindow
                childAgeInWeeks in minWeeks..maxWeeks
            }
            SubstancesDataUtil.applyVaccinesCatchUpSchedule(
                ageFiltered, childAgeInWeeks, patientVisits.value ?: emptyList(),
                substancesGroupConfig, substancesConfig
            )
        } else {
            substancesForVisitType
        }

        val otherSubstancesList = SubstancesDataUtil.getOtherSubstancesDataForVisitType(
            selectedType,
            configurationManager
        )
        val filteredOtherSubstancesList = filterOtherSubstancesByLLIN(otherSubstancesList)

        otherSubstancesData.value = filteredOtherSubstancesList
        removeSelectedOtherSubstancesIfNotRelatedToVisitType()
    }

    private fun removeSelectedOtherSubstancesIfNotRelatedToVisitType() {
        val conceptNames = otherSubstancesData.value?.map { it.conceptName } ?: emptyList()
        val currentData = selectedOtherSubstances.value?.toMutableMap()
        currentData?.entries?.removeIf { (key, _) ->
            key !in conceptNames
        }
        selectedOtherSubstances.value = currentData ?: mutableMapOf()
    }

    fun getLocationUuid(): String {
        return syncSettingsRepository.getSiteUuidOrThrow()
    }

    private fun filterOtherSubstancesByLLIN(otherSubstancesList: List<OtherSubstanceDataModel>): List<OtherSubstanceDataModel> {
        return if (isLLINAlreadyAdministered() == true) {
            otherSubstancesList.filter { it.conceptName != Constants.CONCEPT_NAME_RECEIVED_LLIN }
        } else {
            otherSubstancesList
        }
    }

    private fun isLLINAlreadyAdministered(): Boolean? {
        return patientVisits.value?.any { visit ->
            visit.observations[Constants.CONCEPT_NAME_RECEIVED_LLIN]?.value == Constants.YES_ANSWER
        }
    }

    suspend fun onReferralAfterContraindications(referralObservations: Map<String, String>) {
        try {
            createVisitUseCase.createVisit(
                buildNextVisitObject(
                    participant.value!!,
                    Date(contraindicationsRescheduleDate.value!!.unixMillisLong)
                )
            )

            val contraindicationsRescheduleReasonText = contraindicationsRescheduleReasonText.value.toString()
            val attributesToAdd =
                mutableMapOf(Constants.RESCHEDULE_VISIT_REASON_ATTRIBUTE_TYPE_NAME to contraindicationsRescheduleReasonText)
            visitManager.updateVisitAttributes(
                dosingVisit.value,
                participant.value!!.participantUuid,
                attributesToAdd,
                referralObservations
            )
        } catch (ex: Exception) {
            Log.e("Rescheduling a visit", "Reschedule has failed", ex)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun findVisitType(participant: ParticipantSummaryUiModel, visitTime: Date): String {
        val participantVisits = visitManager.getVisitsForParticipant(participant.participantUuid)
        return SubstancesDataUtil.getVisitTypeForVisitWithGivenDate(
            participant.birthDateText,
            DateUtil.convertDateToString(visitTime, DateFormat.FORMAT_DATE.toString()),
            participantVisits,
            configurationManager
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun buildNextVisitObject(participant: ParticipantSummaryUiModel, visitDate: Date): CreateVisit {
        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Location not available")
        val visitType = dosingVisit.value?.visitTypeVxnaid ?: findVisitType(participant, visitDate) // it assigns visit type of current visit to new rescheduled one
        val doseNumber = SubstancesDataUtil.getDoseNumberForVisitType(visitType, configurationManager.getSubstancesConfig())
        return CreateVisit(
            participantUuid = participant.participantUuid,
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = visitDate,
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitType,
                Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to doseNumber.toString()
            )
        )
    }

    private fun formatDateForDisplay(date: Date): String {
        val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
        return dateFormat.format(date)
    }

    private fun calculateNextVisitPreview(selectedDate: Date): String {
        return try {
            val visits = patientVisits.value ?: return ""

            val lastDosingVisit = visits
                .filter {
                    it.visitType == Constants.VISIT_TYPE_DOSING &&
                            it.visitStatus == Constants.VISIT_STATUS_OCCURRED
                }
                .maxByOrNull { it.startDate.time }
                ?: return ""

            val upcomingVisitFromList = visits
                .filter {
                    it.visitStatus == Constants.VISIT_STATUS_SCHEDULED &&
                            it.startDate.after(lastDosingVisit.startDate)
                }
                .minByOrNull { it.startDate }
                ?: return ""

            val lastDoseCalendar = Calendar.getInstance().apply { time = lastDosingVisit.startDate }
            val selectedCalendar = Calendar.getInstance().apply { time = selectedDate }

            val daysDifference =
                ((selectedCalendar.timeInMillis - lastDoseCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

            val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH)
            if (daysDifference != 0) {
                val nextVisitCalendar = Calendar.getInstance().apply {
                    time = upcomingVisitFromList.startDate
                }
                nextVisitCalendar.add(Calendar.DAY_OF_YEAR, daysDifference)

                resourcesWrapper.getString(
                    R.string.visit_preview_next_visit,
                    dateFormat.format(nextVisitCalendar.time)
                )
            } else {
                resourcesWrapper.getString(
                    R.string.visit_preview_next_visit,
                    dateFormat.format(upcomingVisitFromList.startDate)
                )
            }
        } catch (ex: Exception) {
            logWarn("Error calculating next visit preview: ", ex)
            ""
        }
    }

    fun onVisitDateSelected(date: Date) {
        if (validateVisitDate(date)) {
            selectedVisitDate.value = date

            val daysDifference = ((Date().time - date.time) / (1000 * 60 * 60 * 24)).toInt()
            if (daysDifference > 7) {
                logInfo(
                    "Vaccination date selected ${daysDifference} days ago: ${
                        formatDateForDisplay(
                            date
                        )
                    }"
                )
            } else {
                logInfo("Vaccination date selected: ${formatDateForDisplay(date)}")
            }
        }
    }

    private fun validateVisitDate(date: Date): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        if (date > today) {
            errorMessage.set(resourcesWrapper.getString(R.string.visit_error_date_future))
            return false
        }

        val lastDosingVisit = patientVisits.value
            ?.filter {
                it.visitType == Constants.VISIT_TYPE_DOSING &&
                        it.visitStatus == Constants.VISIT_STATUS_OCCURRED
            }
            ?.maxByOrNull { it.startDate.time }

        if (lastDosingVisit != null && date < lastDosingVisit.startDate) {
            val previousDateStr = formatDateForDisplay(lastDosingVisit.startDate)
            errorMessage.set("Vaccination date cannot be before previous visit ($previousDateStr)")
            logWarn("Selected visit date is before previous dosing visit: $previousDateStr")
            return false
        }

        // Clear any previous error messages when a valid date is selected
        errorMessage.set(null)
        return true
    }
}
