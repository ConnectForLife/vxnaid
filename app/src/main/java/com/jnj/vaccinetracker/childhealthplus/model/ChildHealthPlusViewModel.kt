package com.jnj.vaccinetracker.childhealthplus.model

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.DoseNumber
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
        DOSE_SELECTION,
        ADMINISTRATION_DATE,
        NEXT_VISIT_DATE,
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
    val availableDoses = mutableLiveData<List<DoseNumber>>(emptyList())
    val selectedDose = mutableLiveData<DoseNumber?>()

    val administrationDate = mutableLiveData<Date?>()
    val nextVisitDate = mutableLiveData<Date?>()

    private var visitPlace: String? = null
    private var outreachName: String? = null
    private var attachedClinic: String? = null

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
        val doses = getAvailableDoses(service)
        if (doses.isEmpty()) {
            currentStage.value = WorkflowStage.ADMINISTRATION_DATE
        } else {
            availableDoses.value = doses
            currentStage.value = WorkflowStage.DOSE_SELECTION
        }
    }

    fun selectDose(dose: DoseNumber) {
        selectedDose.value = dose
        currentStage.value = WorkflowStage.ADMINISTRATION_DATE
    }

    fun setAdministrationDate(date: Date) {
        if (date.after(Date())) {
            errorMessage.value = resourcesWrapper.getString(R.string.visit_error_date_future)
            return
        }

        administrationDate.value = date
        val service = currentService.value ?: return

        if (service.requiresNextVisitScheduling()) {
            currentStage.value = WorkflowStage.NEXT_VISIT_DATE
        } else {
            // No follow-up scheduling needed — save service and return to selection
            addSelectedService(service, selectedDose.value, date, null)
            resetServiceSelection()
            currentStage.value = WorkflowStage.SERVICE_SELECTION
        }
    }

    fun setNextVisitDate(date: Date?) {
        val service = currentService.value
        val adminDate = administrationDate.value

        if (date != null && adminDate != null && date.before(adminDate)) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_next_visit_invalid)
            return
        }

        nextVisitDate.value = date

        if (service != null && adminDate != null) {
            addSelectedService(service, selectedDose.value, adminDate, date)
            resetServiceSelection()
            currentStage.value = WorkflowStage.SERVICE_SELECTION
        }
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
                        visitOutreachName = outreachName
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

    private suspend fun createServiceVisit(
        participantUuid: String,
        selectedService: SelectedService,
        siteUuid: String,
        operatorUuid: String,
        isPregnant: Boolean,
    ) {
        val doseOrder = selectedService.dose?.order ?: 1
        val visitAttributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to doseOrder.toString(),
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

        // Schedule a follow-up visit if requested
        selectedService.nextVisitDate?.let { nextDate ->
            createVisitUseCase.createVisit(
                CreateVisit(
                    participantUuid = participantUuid,
                    visitType = Constants.VISIT_TYPE_DOSING,
                    startDatetime = nextDate,
                    locationUuid = siteUuid,
                    attributes = mapOf(
                        Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                        Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                        Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to selectedService.service.serviceKey
                    ) + visitContextAttributes()
                )
            )
        }
    }

    private fun visitContextAttributes(): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        visitPlace?.let { attributes[Constants.ATTRIBUTE_VISIT_LOCATION] = it }
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
            WorkflowStage.SERVICE_SELECTION -> WorkflowStage.CLIENT_INFO
            WorkflowStage.DOSE_SELECTION -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.ADMINISTRATION_DATE -> {
                val service = currentService.value
                if (service != null && getAvailableDoses(service).isNotEmpty()) {
                    WorkflowStage.DOSE_SELECTION
                } else {
                    WorkflowStage.SERVICE_SELECTION
                }
            }
            WorkflowStage.NEXT_VISIT_DATE -> WorkflowStage.ADMINISTRATION_DATE
            WorkflowStage.CONFIRMATION -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.SUCCESS, WorkflowStage.COMPLETED -> return
        }
        currentStage.value = previousStage
    }

    private fun addSelectedService(
        service: ChildHealthPlusService,
        dose: DoseNumber?,
        adminDate: Date,
        nextVisit: Date?,
    ) {
        val newService = SelectedService(
            service = service,
            dose = dose,
            administrationDate = adminDate,
            nextVisitDate = nextVisit
        )
        val current = selectedServices.value.orEmpty().toMutableList()
        current.add(newService)
        selectedServices.value = current
    }

    private fun resetServiceSelection() {
        currentService.value = null
        selectedDose.value = null
        administrationDate.value = null
        nextVisitDate.value = null
        availableDoses.value = emptyList()
    }

    private fun getAvailableDoses(service: ChildHealthPlusService): List<DoseNumber> {
        return when (service) {
            ChildHealthPlusService.VITAMIN_A, ChildHealthPlusService.DEWORMING ->
                listOf(DoseNumber.DOSE_1, DoseNumber.DOSE_2)
            ChildHealthPlusService.TETANUS ->
                listOf(DoseNumber.TD_1, DoseNumber.TD_2, DoseNumber.TD_3, DoseNumber.TD_4, DoseNumber.TD_5)
            ChildHealthPlusService.HEPATITIS_B ->
                listOf(DoseNumber.HEPB_1, DoseNumber.HEPB_2, DoseNumber.HEPB_3)
            ChildHealthPlusService.MR2, ChildHealthPlusService.HPV ->
                emptyList()
        }
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
