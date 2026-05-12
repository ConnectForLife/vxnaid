package com.jnj.vaccinetracker.childhealthplus.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.repositories.DraftChildHealthPlusRepository
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusData
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.DoseNumber
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.viewmodel.ViewModelBase
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Child Health+ workflow
 * Manages the flow of capturing simplified client information and services
 * without requiring full immunization history
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusViewModel @Inject constructor(
    private val repository: DraftChildHealthPlusRepository,
    private val userRepository: UserRepository,
    override val dispatchers: AppCoroutineDispatchers,
    private val resourcesWrapper: ResourcesWrapper,
) : ViewModelBase() {

    /**
     * Workflow stages
     */
    enum class WorkflowStage {
        CLIENT_INFO,
        SERVICE_SELECTION,
        DOSE_SELECTION,
        ADMINISTRATION_DATE,
        NEXT_VISIT_DATE,
        CONFIRMATION,
        COMPLETED
    }

    // Workflow state
    val currentStage = mutableLiveData<WorkflowStage>(WorkflowStage.CLIENT_INFO)
    val errorMessage = mutableLiveData<String?>()
    val loading = mutableLiveBoolean()
    
    // Client information
    val clientName = mutableLiveData<String>()
    val dateOfBirth = mutableLiveData<Date>()
    val sex = mutableLiveData<String>() // "M" or "F"
    val contactInfo = mutableLiveData<String>()
    val mothersName = mutableLiveData<String>()
    val isPregnantWoman = mutableLiveBoolean()
    
    // Service selection
    val availableServices = mutableLiveData<List<ChildHealthPlusService>>(
        ChildHealthPlusService.values().toList()
    )
    val selectedServices = mutableLiveData<List<SelectedService>>(emptyList())
    
    // Current service being configured
    val currentService = mutableLiveData<ChildHealthPlusService?>()
    val availableDoses = mutableLiveData<List<DoseNumber>>(emptyList())
    val selectedDose = mutableLiveData<DoseNumber?>()
    
    // Dates
    val administrationDate = mutableLiveData<Date?>()
    val nextVisitDate = mutableLiveData<Date?>()
    
    // Events
    val submitSuccessEvent = eventFlow<ChildHealthPlusData>()
    val submitFailedEvent = eventFlow<String>()
    val navigationEvent = eventFlow<WorkflowStage>()
    
    // Validation
    private val clientInfoErrors = mutableLiveData<List<String>>(emptyList())
    private val currentServiceErrors = mutableLiveData<List<String>>(emptyList())
    
    fun setClientInfo(name: String, dob: Date, sex: String, contact: String, motherName: String) {
        clientName.value = name
        dateOfBirth.value = dob
        this.sex.value = sex
        contactInfo.value = contact
        mothersName.value = motherName
    }
    
    fun proceedToServiceSelection() {
        val errors = validateClientInfo()
        if (errors.isNotEmpty()) {
            clientInfoErrors.value = errors
            errorMessage.value = errors.joinToString(", ")
            return
        }
        currentStage.value = WorkflowStage.SERVICE_SELECTION
    }
    
    fun addService(service: ChildHealthPlusService) {
        currentService.value = service
        
        // Get available doses for this service
        val doses = getAvailableDoses(service)
        if (doses.isEmpty()) {
            // Single-dose services like MR1 and HPV
            handleSingleDoseService(service)
        } else {
            // Multi-dose services need dose selection
            availableDoses.value = doses
            currentStage.value = WorkflowStage.DOSE_SELECTION
        }
    }
    
    fun selectDose(dose: DoseNumber) {
        selectedDose.value = dose
        currentStage.value = WorkflowStage.ADMINISTRATION_DATE
    }
    
    fun setAdministrationDate(date: Date) {
        administrationDate.value = date
        currentStage.value = WorkflowStage.NEXT_VISIT_DATE
    }
    
    fun setNextVisitDate(date: Date?) {
        nextVisitDate.value = date
        
        // Create the selected service record
        val service = currentService.value
        val dose = selectedDose.value
        val adminDate = administrationDate.value
        
        if (service != null && adminDate != null) {
            val selectedService = SelectedService(
                service = service,
                dose = dose,
                administrationDate = adminDate,
                nextVisitDate = date
            )
            
            // Add to selected services list
            val current = selectedServices.value.orEmpty().toMutableList()
            current.add(selectedService)
            selectedServices.value = current
            
            // Reset for next service
            resetServiceSelection()
            
            // Ask if user wants to add another service or proceed to confirmation
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
        val name = clientName.value
        val dob = dateOfBirth.value
        val sex = this.sex.value
        val contact = contactInfo.value
        val mother = mothersName.value
        val services = selectedServices.value.orEmpty()
        
        if (name.isNullOrBlank() || dob == null || sex.isNullOrBlank() || 
            contact.isNullOrBlank() || mother.isNullOrBlank()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_validation_error)
            submitFailedEvent.tryEmit(errorMessage.value ?: "Validation failed")
            return
        }
        
        if (services.isEmpty()) {
            errorMessage.value = resourcesWrapper.getString(R.string.child_health_plus_no_services_selected)
            submitFailedEvent.tryEmit(errorMessage.value ?: "No services selected")
            return
        }
        
        loading.set(true)
        scope.launch {
            try {
                val operatorUuid = userRepository.getUser()?.uuid
                if (operatorUuid == null) {
                    throw OperatorUuidNotAvailableException("Operator UUID not available")
                }
                
                val childHealthPlusData = ChildHealthPlusData(
                    clientName = name,
                    dateOfBirth = dob,
                    sex = sex,
                    contactInfo = contact,
                    mothersName = mother,
                    services = services,
                    isPregnantWoman = isPregnantWoman.get(),
                    operatorUuid = operatorUuid
                )
                
                repository.save(childHealthPlusData)
                
                loading.set(false)
                currentStage.value = WorkflowStage.COMPLETED
                submitSuccessEvent.tryEmit(childHealthPlusData)
            } catch (ex: OperatorUuidNotAvailableException) {
                yield()
                loading.set(false)
                errorMessage.value = ex.message
                submitFailedEvent.tryEmit(ex.message ?: "Session expired")
                logError("Failed to submit Child Health+ data: operator UUID not available", ex)
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
    
    fun goBack() {
        val currentStage = currentStage.value ?: return
        val previousStage = when (currentStage) {
            WorkflowStage.CLIENT_INFO -> return // Can't go back from first screen
            WorkflowStage.SERVICE_SELECTION -> WorkflowStage.CLIENT_INFO
            WorkflowStage.DOSE_SELECTION -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.ADMINISTRATION_DATE -> WorkflowStage.DOSE_SELECTION
            WorkflowStage.NEXT_VISIT_DATE -> WorkflowStage.ADMINISTRATION_DATE
            WorkflowStage.CONFIRMATION -> WorkflowStage.SERVICE_SELECTION
            WorkflowStage.COMPLETED -> return
        }
        this.currentStage.value = previousStage
    }
    
    private fun handleSingleDoseService(service: ChildHealthPlusService) {
        // For single-dose services, skip dose selection and go to date
        currentStage.value = WorkflowStage.ADMINISTRATION_DATE
    }
    
    private fun resetServiceSelection() {
        currentService.value = null
        selectedDose.value = null
        administrationDate.value = null
        nextVisitDate.value = null
        availableDoses.value = emptyList()
        currentServiceErrors.value = emptyList()
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
                emptyList() // Single dose, no selection needed
        }
    }
    
    private fun validateClientInfo(): List<String> {
        val errors = mutableListOf<String>()
        
        if (clientName.value.isNullOrBlank()) {
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_name_required))
        }
        if (dateOfBirth.value == null) {
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_dob_required))
        }
        if (sex.value.isNullOrBlank()) {
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_sex_required))
        }
        if (contactInfo.value.isNullOrBlank()) {
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_contact_required))
        }
        if (mothersName.value.isNullOrBlank()) {
            errors.add(resourcesWrapper.getString(R.string.child_health_plus_mother_required))
        }
        
        return errors
    }
}

