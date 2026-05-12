package com.jnj.vaccinetracker.childhealthplus.model

import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.DraftChildHealthPlusRepository
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusData
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.DoseNumber
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.domain.entities.BirthDate
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.domain.entities.RegisterParticipant
import com.jnj.vaccinetracker.common.domain.entities.ScheduleFirstVisit
import com.jnj.vaccinetracker.common.domain.entities.UpdateVisit
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.domain.usecases.UpdateVisitUseCase
import kotlin.random.Random
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ChildHealthPlusViewModel @Inject constructor(
    private val repository: DraftChildHealthPlusRepository,
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
    val sex = mutableLiveData<String>()
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

    val submitSuccessEvent = eventFlow<ChildHealthPlusData>()
    val submitFailedEvent = eventFlow<String>()
    val navigationEvent = eventFlow<WorkflowStage>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun generateChildId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun onGenerateQrCode(): String {
        val id = generateChildId()
        generatedChildId.value = id
        return id
    }

    fun proceedToServiceSelection() {
        val errors = validateClientInfo()
        if (errors.isNotEmpty()) {
            errorMessage.value = errors.joinToString(", ")
            return
        }
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
        val firstName = childFirstName.value
        val lastName = childLastName.value
        val dob = dateOfBirth.value
        val sex = this.sex.value
        val phone = telephone.value
        val countryCode = phoneCountryCode.value
        val services = selectedServices.value.orEmpty()

        if (firstName.isNullOrBlank() || lastName.isNullOrBlank() || dob == null || sex.isNullOrBlank()) {
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

                val genderEnum = when (sex) {
                    "M" -> Gender.MALE
                    "F" -> Gender.FEMALE
                    else -> Gender.OTHERS
                }

                val fullPhone = if (!phone.isNullOrBlank()) "${countryCode ?: ""}$phone" else ""

                val personAttributes = mutableMapOf(
                    Constants.ATTRIBUTE_LOCATION to siteUuid,
                    Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                    Constants.ATTRIBUTE_TELEPHONE to fullPhone,
                    Constants.ATTRIBUTE_MOTHER_FIRST_NAME to (motherFirstName.value ?: ""),
                    Constants.ATTRIBUTE_MOTHER_LAST_NAME to (motherLastName.value ?: ""),
                    Constants.ATTRIBUTE_BEST_CONTACT_TIME to (bestContactTime.value ?: ""),
                    Constants.ATTRIBUTE_LANGUAGE to (language.value ?: ""),
                    Constants.ATTRIBUTE_CHILD_HEALTH_PLUS to "true"
                )

                val registerParticipant = RegisterParticipant(
                    participantId = childHealthPlusId,
                    nin = null,
                    childNumber = null,
                    gender = genderEnum,
                    isBirthDateEstimated = false,
                    birthDate = BirthDate(dob.time),
                    address = Address("", "", "", "", "", "", ""),
                    attributes = personAttributes,
                    image = null,
                    biometricsTemplate = null,
                    scheduleFirstVisit = ScheduleFirstVisit(
                        visitType = Constants.VISIT_TYPE_DOSING,
                        startDatetime = Date(),
                        locationUuid = siteUuid,
                        attributes = mapOf(
                            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                            Constants.ATTRIBUTE_OPERATOR to operatorUuid
                        )
                    ),
                    childFirstName = firstName,
                    childLastName = lastName,
                    dateCreated = null
                )

                val draftParticipant = participantManager.registerParticipant(registerParticipant)
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

                val childHealthPlusData = ChildHealthPlusData(
                    childFirstName = firstName,
                    childLastName = lastName,
                    dateOfBirth = dob,
                    sex = sex,
                    telephone = phone,
                    phoneCountryCode = countryCode,
                    motherFirstName = motherFirstName.value,
                    motherLastName = motherLastName.value,
                    language = language.value,
                    bestContactTime = bestContactTime.value,
                    services = services,
                    isPregnantWoman = isPregnantWoman.get(),
                    operatorUuid = operatorUuid,
                    participantUuid = participantUuid,
                    locationUuid = siteUuid
                )
                repository.save(childHealthPlusData)

                loading.set(false)
                currentStage.value = WorkflowStage.SUCCESS
                submitSuccessEvent.tryEmit(childHealthPlusData)

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
            Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to selectedService.service.serviceKey,
            Constants.ATTRIBUTE_CHILD_HEALTH_PLUS to "true"
        )

        val adminDateStr = dateFormat.format(selectedService.administrationDate)
        val observations = mutableMapOf(
            "${selectedService.service.conceptName} ${Constants.DATE_STR}" to adminDateStr
        )
        if (isPregnant && selectedService.service == ChildHealthPlusService.TETANUS_DIPHTHERIA) {
            observations["Pregnant Woman Vxnaid"] = "true"
        }

        // Create visit shell, then record encounter with observations
        val draftVisit = createVisitUseCase.createVisit(
            CreateVisit(
                participantUuid = participantUuid,
                visitType = Constants.VISIT_TYPE_DOSING,
                startDatetime = selectedService.administrationDate,
                locationUuid = siteUuid,
                attributes = visitAttributes
            )
        )

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
                        Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to selectedService.service.serviceKey,
                        Constants.ATTRIBUTE_CHILD_HEALTH_PLUS to "true"
                    )
                )
            )
        }
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
            ChildHealthPlusService.TETANUS_DIPHTHERIA ->
                listOf(DoseNumber.TD_1, DoseNumber.TD_2, DoseNumber.TD_3, DoseNumber.TD_4, DoseNumber.TD_5)
            ChildHealthPlusService.HEPATITIS_B ->
                listOf(DoseNumber.HEPB_1, DoseNumber.HEPB_2, DoseNumber.HEPB_3)
            ChildHealthPlusService.MR1, ChildHealthPlusService.HPV ->
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
        if (sex.value.isNullOrBlank())
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_sex_required))
        return errors
    }
}
