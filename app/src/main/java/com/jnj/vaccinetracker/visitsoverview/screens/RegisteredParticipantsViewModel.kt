package com.jnj.vaccinetracker.visitsoverview.screens

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.database.models.RoomParticipantModel
import com.jnj.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.jnj.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.jnj.vaccinetracker.common.viewmodel.ViewModelWithState
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
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
            try {
                val patients = participantRepository.findPatients()

                // Log the size of the returned list or check the actual list
                Log.d(".......................................................RegisteredParticipantsViewModel", "Number of patients: ${patients.size}")


                if (patients.isEmpty()) {
                    Log.d("**********************************RegisteredParticipantsViewModel", "No patients found.")
                }

                // Set the patient DTO list if patients are found
                patientDTOs.value = createParticipantDTOList(patients)

            } catch (e: Exception) {
                // Log any errors
                Log.e("RegisteredParticipantsViewModel", "Error fetching patients: ${e.message}")
            } finally {
                // Hide the loading indicator
                isLoading.value = false
            }
        }
    }



    private fun getTodayMidnight(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private suspend fun createParticipantDTOList(patients: List<RoomParticipantModel>): List<ParticipantDataDTO> {
        return patients.map { patient ->
            ParticipantDataDTO(
                participantId = patient.participantId,
                startDatetime = patient.dateModified,
                fullName = "${patient.childFirstName} ${patient.childLastName}",
                motherName = "${patient.motherFirstName} ${patient.motherLastName}",
                registrationDate = com.soywiz.klock.Date(patient.dateModified.time.toInt())
            )
        }
    }

    override fun saveInstanceState(outState: Bundle) {}

    override fun restoreInstanceState(savedInstanceState: Bundle) {}
}
