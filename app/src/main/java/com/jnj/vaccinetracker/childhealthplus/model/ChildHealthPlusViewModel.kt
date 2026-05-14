package com.jnj.vaccinetracker.childhealthplus.model

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.PastServiceItem
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.domain.entities.UpdateVisit
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.UpdateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random
import javax.inject.Inject

class ChildHealthPlusViewModel @Inject constructor(
    private val participantManager: ParticipantManager,
    private val visitManager: VisitManager,
    private val findParticipantByUuidUseCase: FindParticipantByParticipantUuidUseCase,
    private val userRepository: UserRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val createVisitUseCase: CreateVisitUseCase,
    private val updateVisitUseCase: UpdateVisitUseCase,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    enum class WorkflowStage {
        CLIENT_INFO,
        SERVICE_SELECTION,
        ADMINISTRATION_DATE,
        CONFIRMATION,
        SUCCESS,
        COMPLETED
    }

    val currentStage = mutableLiveData<WorkflowStage>(WorkflowStage.CLIENT_INFO)
    val errorMessage = mutableLiveData<String?>()
    val loading = mutableLiveBoolean()

    val generatedChildId = mutableLiveData<String>()

    val childFirstName = mutableLiveData<String>()
    val childLastName = mutableLiveData<String>()
    val dateOfBirth = mutableLiveData<Date>()
    val gender = mutableLiveData<String>()
    val telephone = mutableLiveData<String>()
    val phoneCountryCode = mutableLiveData<String>()
    val motherFirstName = mutableLiveData<String>()
    val motherLastName = mutableLiveData<String>()
    val language = mutableLiveData<String?>()
    val bestContactTime = mutableLiveData<String?>()
    val isPregnantWoman = mutableLiveBoolean()

    val availableServices = mutableLiveData<List<ChildHealthPlusService>>(
        ChildHealthPlusService.values().toList()
    )
    val selectedServices = mutableLiveData<List<SelectedService>>(emptyList())

    val currentService = mutableLiveData<ChildHealthPlusService?>()

    val administrationDate = mutableLiveData<Date?>()

    val pastServices = mutableLiveData<List<PastServiceItem>>(emptyList())
    val pastServicesLoading = mutableLiveBoolean(false)

    private var visitPlace: String? = null
    private var outreachName: String? = null
    private var attachedClinic: String? = null
    private var returnVisitParticipantUuid: String? = null
    val isReturnVisit = mutableLiveBoolean(false)

    val submitSuccessEvent = eventFlow<Unit>()
    val submitFailedEvent = eventFlow<String>()
    val navigationEvent = eventFlow<WorkflowStage>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
    }

    fun generateChildId(): String {
        val identifierLength = 8
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..identifierLength)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    fun ensureGeneratedChildId(): String {
        val currentId = generatedChildId.value
        if (!currentId.isNullOrBlank()) return currentId

        return generateChildId().also { generatedChildId.value = it }
    }

    fun initReturnVisit(participant: ParticipantSummaryUiModel) {
        returnVisitParticipantUuid = participant.participantUuid
        generatedChildId.value = participant.participantId
        gender.value = when (participant.gender) {
            com.jnj.vaccinetracker.common.domain.entities.Gender.MALE -> "M"
            com.jnj.vaccinetracker.common.domain.entities.Gender.FEMALE -> "F"
            else -> "O"
        }
        isReturnVisit.set(true)
        currentStage.value = WorkflowStage.SERVICE_SELECTION
        loadPastServices(participant.participantUuid)
    }

    private fun loadPastServices(participantUuid: String) {
        pastServicesLoading.set(true)
        scope.launch {
            try {
                val participant = findParticipantByUuidUseCase.findByParticipantUuid(participantUuid)
                if (participant != null) {
                    childFirstName.value = participant.childFirstName
                    childLastName.value = participant.childLastName
                    motherFirstName.value = participant.motherFirstName
                    motherLastName.value = participant.motherLastName
                }

                val servicesByKey = ChildHealthPlusService.values().associateBy { it.serviceKey }
                val items = visitManager.getVisitsForParticipant(participantUuid)
                    .filter {
                        it.visitStatus == Constants.VISIT_STATUS_OCCURRED &&
                        it.visitType == Constants.VISIT_TYPE_DOSING
                    }
                    .sortedByDescending { it.visitDate }
                    .mapNotNull { visit ->
                        val service = servicesByKey[visit.visitTypeVxnaid] ?: return@mapNotNull null
                        PastServiceItem(
                            displayName = service.displayName,
                            date = dateFormat.format(visit.visitDate)
                        )
                    }
                pastServices.value = items
            } catch (throwable: Throwable) {
                logError("Failed to load return visit data", throwable)
            } finally {
                yield()
                pastServicesLoading.set(false)
            }
        }
    }

    fun setVisitContext(visitPlace: String?, outreachName: String?, attachedClinic: String?) {
        this.visitPlace = visitPlace?.takeIf { it.isNotBlank() }
        this.outreachName = outreachName?.takeIf { it.isNotBlank() }
        this.attachedClinic = attachedClinic?.takeIf { it.isNotBlank() }
    }

    fun proceedToServiceSelection() {
        val errors = validateClientInfo()
        if (errors.isNotEmpty()) {
            errorMessage.value = errors.joinToString(", ")
            return
        }
        ensureGeneratedChildId()
        currentStage.value = WorkflowStage.SERVICE_SELECTION
    }

    fun addService(service: ChildHealthPlusService) {
        currentService.value = service
        currentStage.value = WorkflowStage.ADMINISTRATION_DATE
    }

    fun setAdministrationDate(date: Date) {
        if (date.after(Date())) {
            errorMessage.value = resourcesWrapper.getString(R.string.visit_error_date_future)
            return
        }

        val service = currentService.value ?: return
        addSelectedService(service, date)
        resetServiceSelection()
        currentStage.value = WorkflowStage.SERVICE_SELECTION
    }

    fun removeService(serviceUuid: String) {
        val current = selectedServices.value.orEmpty().toMutableList()
        current.removeAll { it.uuid == serviceUuid }
        selectedServices.value = current
    }

    fun proceedToConfirmation() {
        if (selectedServices.value.isNullOrEmpty()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_no_services_selected)
            return
        }
        currentStage.value = WorkflowStage.CONFIRMATION
    }

    fun submitChildHealthPlus() {
        val existingUuid = returnVisitParticipantUuid
        if (existingUuid != null) {
            submitReturnVisit(existingUuid)
            return
        }
        val chlidFirstName = childFirstName.value
        val childLastName = this@ChildHealthPlusViewModel.childLastName.value
        val dob = dateOfBirth.value
        val gender = this.gender.value
        val phone = telephone.value
        val countryCode = phoneCountryCode.value
        val services = selectedServices.value.orEmpty()

        if (chlidFirstName.isNullOrBlank() || childLastName.isNullOrBlank() || dob == null || gender.isNullOrBlank()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_validation_error)
            submitFailedEvent.tryEmit(errorMessage.value ?: "Validation failed")
            return
        }

        if (services.isEmpty()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_no_services_selected)
            submitFailedEvent.tryEmit(errorMessage.value ?: "No services selected")
            return
        }

        val childHealthPlusId = generatedChildId.value
        if (childHealthPlusId.isNullOrBlank()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_id_required)
            submitFailedEvent.tryEmit(errorMessage.value ?: "Child ID not generated")
            return
        }

        loading.set(true)
        scope.launch {
            try {
                val operatorUuid = userRepository.getUser()?.uuid
                    ?: throw OperatorUuidNotAvailableException("Operator UUID not available")

                val siteUuid = syncSettingsRepository.getSiteUuid()
                    ?: throw NoSiteUuidAvailableException("Site UUID not available")

                val genderEnum = when (gender) {
                    "M" -> Gender.MALE
                    "F" -> Gender.FEMALE
                    else -> Gender.OTHERS
                }

                val fullPhone = phone?.takeIf { it.isNotBlank() }?.let { "${countryCode ?: ""}$it" }

                val registerRequest = participantManager.getRegisterParticipant(
                    ParticipantManager.RegisterDetails(
                        participantId = childHealthPlusId,
                        nin = null,
                        childNumber = null,
                        birthWeight = null,
                        bestContactTime = bestContactTime.value,
                        gender = genderEnum,
                        birthDate = com.soywiz.klock.DateTime.fromUnix(dob.time),
                        isBirthDateEstimated = false,
                        telephone = fullPhone,
                        siteUuid = siteUuid,
                        language = language.value,
                        address = Address("", "", "", "", "", "", ""),
                        picture = null,
                        biometricsTemplateBytes = null,
                        motherFirstName = motherFirstName.value ?: "",
                        motherLastName = motherLastName.value ?: "",
                        fatherFirstName = null,
                        fatherLastName = null,
                        childFirstName = chlidFirstName,
                        childLastName = childLastName,
                        childCategory = null,
                        dateCreated = dateNow().time,
                        attachedClinic = attachedClinic,
                        visitPlace = visitPlace,
                        visitOutreachName = outreachName,
                        isChildHealthPlus = true,
                    )
                )

                val draftParticipant = participantManager.registerParticipant(registerRequest)
                val participantUuid = draftParticipant.participantUuid

                // Create an OCCURRED dosing visit for each selected service
                services.forEach { selectedService ->
                    createServiceVisit(
                        participantUuid = participantUuid,
                        selectedService = selectedService,
                        siteUuid = siteUuid,
                        operatorUuid = operatorUuid,
                        isPregnant = isPregnantWoman.get()
                    )
                }

                loading.set(false)
                currentStage.value = WorkflowStage.SUCCESS
                submitSuccessEvent.tryEmit(Unit)

            } catch (ex: OperatorUuidNotAvailableException) {
                yield()
                loading.set(false)
                errorMessage.value = ex.message
                submitFailedEvent.tryEmit(ex.message ?: "Session expired")
                logError("Failed to submit Child Health+ data: operator UUID not available", ex)
            } catch (ex: NoSiteUuidAvailableException) {
                yield()
                loading.set(false)
                errorMessage.value = ex.message
                submitFailedEvent.tryEmit(ex.message ?: "No site selected")
                logError("Failed to submit Child Health+ data: site UUID not available", ex)
            } catch (throwable: Throwable) {
                yield()
                throwable.printStackTrace()
                loading.set(false)
                errorMessage.value = throwable.message
                submitFailedEvent.tryEmit(throwable.message ?: "Submission failed")
                logError("Failed to submit Child Health+ data: ", throwable)
            }
        }
    }

    private fun submitReturnVisit(participantUuid: String) {
        val services = selectedServices.value.orEmpty()
        if (services.isEmpty()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_no_services_selected)
            submitFailedEvent.tryEmit(errorMessage.value ?: "No services selected")
            return
        }
        loading.set(true)
        scope.launch {
            try {
                val operatorUuid = userRepository.getUser()?.uuid
                    ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
                val siteUuid = syncSettingsRepository.getSiteUuid()
                    ?: throw NoSiteUuidAvailableException("Site UUID not available")

                services.forEach { selectedService ->
                    createServiceVisit(
                        participantUuid = participantUuid,
                        selectedService = selectedService,
                        siteUuid = siteUuid,
                        operatorUuid = operatorUuid,
                        isPregnant = isPregnantWoman.get()
                    )
                }
                loading.set(false)
                currentStage.value = WorkflowStage.SUCCESS
                submitSuccessEvent.tryEmit(Unit)
            } catch (ex: OperatorUuidNotAvailableException) {
                yield()
                loading.set(false)
                errorMessage.value = ex.message
                submitFailedEvent.tryEmit(ex.message ?: "Session expired")
                logError("Failed to submit return visit: operator UUID not available", ex)
            } catch (ex: NoSiteUuidAvailableException) {
                yield()
                loading.set(false)
                errorMessage.value = ex.message
                submitFailedEvent.tryEmit(ex.message ?: "No site selected")
                logError("Failed to submit return visit: site UUID not available", ex)
            } catch (throwable: Throwable) {
                yield()
                throwable.printStackTrace()
                loading.set(false)
                errorMessage.value = throwable.message
                submitFailedEvent.tryEmit(throwable.message ?: "Submission failed")
                logError("Failed to submit return visit: ", throwable)
            }
        }
    }

    private suspend fun createServiceVisit(
        participantUuid: String,
        selectedService: SelectedService,
        siteUuid: String,
        operatorUuid: String,
        isPregnant: Boolean,
    ) {
        val visitAttributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to selectedService.service.serviceKey
        ) + visitContextAttributes()

        val draftVisit = createVisitUseCase.createVisit(
            CreateVisit(
                participantUuid = participantUuid,
                visitType = Constants.VISIT_TYPE_DOSING,
                startDatetime = selectedService.administrationDate,
                locationUuid = siteUuid,
                attributes = visitAttributes
            )
        )

        val observations = mutableMapOf<String, String>()
        val administrationDateStr = dateFormat.format(selectedService.administrationDate)
        observations["${selectedService.service.conceptName} ${Constants.DATE_STR}"] = administrationDateStr

        if (isPregnant && selectedService.service == ChildHealthPlusService.TETANUS) {
            observations["Pregnant Woman Vxnaid"] = "true"
        }

        updateVisitUseCase.updateVisit(
            UpdateVisit(
                visitUuid = draftVisit.visitUuid,
                participantUuid = participantUuid,
                startDatetime = selectedService.administrationDate,
                locationUuid = siteUuid,
                attributes = visitAttributes,
                observations = observations
            )
        )

    }

    private fun visitContextAttributes(): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        attributes[Constants.ATTRIBUTE_VISIT_LOCATION] = visitPlace ?: attachedClinic ?: "CHP"
        outreachName?.let { attributes[Constants.ATTRIBUTE_VISIT_OUTREACH_NAME] = it }
        attachedClinic?.let { attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC] = it }
        return attributes
    }

    fun onSuccessDismissed() {
        currentStage.value = WorkflowStage.COMPLETED
    }

    fun goBack() {
        val stage = currentStage.value ?: return
        val previousStage = when (stage) {
            WorkflowStage.CLIENT_INFO -> return
            WorkflowStage.SERVICE_SELECTION -> if (isReturnVisit.get()) {
                currentStage.value = WorkflowStage.COMPLETED
                return
            } else {
                WorkflowStage.CLIENT_INFO
            }
            WorkflowStage.ADMINISTRATION_DATE -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.CONFIRMATION -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.SUCCESS, WorkflowStage.COMPLETED -> return
        }
        currentStage.value = previousStage
    }

    private fun addSelectedService(service: ChildHealthPlusService, adminDate: Date) {
        val newService = SelectedService(service = service, administrationDate = adminDate)
        val current = selectedServices.value.orEmpty().toMutableList()
        current.add(newService)
        selectedServices.value = current
    }

    private fun resetServiceSelection() {
        currentService.value = null
        administrationDate.value = null
    }

    private fun validateClientInfo(): List<String> {
        val errors = mutableListOf<String>()
        if (childFirstName.value.isNullOrBlank())
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_first_name_required))
        if (childLastName.value.isNullOrBlank())
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_last_name_required))
        if (dateOfBirth.value == null)
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_dob_required))
        if (gender.value.isNullOrBlank())
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_sex_required))
        return errors
    }
}
