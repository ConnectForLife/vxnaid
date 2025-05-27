package com.jnj.vaccinetracker.reportsoverview.childrenoverview.model

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.models.RoomParticipantModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import kotlinx.coroutines.launch
import javax.inject.Inject

class RegisteredParticipantsViewModel @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val draftParticipantRepository: DraftParticipantRepository,
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelWithState() {
    val patientDTOs = MutableLiveData<List<ParticipantDataDTO>>()
    val isLoading = mutableLiveData<Boolean>()

    fun fetchAllPatients() {
        isLoading.value = true
        viewModelScope.launch {
            val patients = participantRepository.findPatients()

            // Fetch draft participants
            val draftParticipant = draftParticipantRepository.findDraftParticipants()

            val participantDTOs = createParticipantDTOList(patients)
            val draftParticipantDTOs = createDraftParticipantDTOList(draftParticipant)

            // Merge, sort by registration date, and update LiveData
            patientDTOs.value = (participantDTOs + draftParticipantDTOs)
                .sortedByDescending { it.registrationDate }

            Log.d(
                "RegisteredChildren",
                "Fetched ${participantDTOs.size} regular participants and ${draftParticipantDTOs.size} draft participants. Total: ${patientDTOs.value?.size ?: 0}"
            )
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

    private fun createDraftParticipantDTOList(draftPatients: List<RoomDraftParticipantModel>): List<ParticipantDataDTO> {
        return draftPatients.map { draftPatient ->
            ParticipantDataDTO(
                participantId = draftPatient.participantId,
                fullName = "${draftPatient.childFirstName} ${draftPatient.childLastName}",
                motherName = "${draftPatient.motherFirstName} ${draftPatient.motherLastName}",
                birthDate = draftPatient.birthDate.toDateTime(),
                registrationDate = draftPatient.registrationDate
            )
        }
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}
