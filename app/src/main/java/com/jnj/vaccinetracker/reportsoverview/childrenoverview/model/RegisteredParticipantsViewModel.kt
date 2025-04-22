package com.jnj.vaccinetracker.reportsoverview.childrenoverview.model

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.models.RoomParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegisteredParticipantsViewModel@Inject constructor(
    private val participantRepository: ParticipantRepository,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val patientDTOs = MutableLiveData<List<ParticipantDataDTO>>()
    val isLoading = mutableLiveData<Boolean>()

    fun fetchAllPatients() {
        isLoading.value = true
        viewModelScope.launch {
            val patients = participantRepository.findPatients()
            patientDTOs.value = createParticipantDTOList(patients)
            isLoading.value = false
        }
    }

    private fun createParticipantDTOList(patients: List<RoomParticipantModel>): List<ParticipantDataDTO> {
        return patients.map { patient ->
            ParticipantDataDTO(
                participantId = patient.participantId,
                fullName = "${patient.childFirstName} ${patient.childLastName}",
                motherName = "${patient.motherFirstName} ${patient.motherLastName}",
                birthDate = patient.birthDate.toDateTime(),
                registrationDate = patient.dateModified
            )
        }
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}
