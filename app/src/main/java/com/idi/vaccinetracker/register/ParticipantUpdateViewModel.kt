package com.idi.vaccinetracker.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.idi.vaccinetracker.common.data.managers.ParticipantManager
import com.idi.vaccinetracker.common.domain.entities.UpdateParticipant
import kotlinx.coroutines.launch

class ParticipantUpdateViewModel(private val participantManager: ParticipantManager): ViewModel() {

    fun updateParticipantInBackground(participantToUpdate: UpdateParticipant) {
        viewModelScope.launch {
            try {
                participantManager.updateParticipant(participantToUpdate)
            } catch (e: Exception) {
                Log.e("ParticipantUpdateModel", "Something went wrong during updating participant", e)
            }
        }
    }
}