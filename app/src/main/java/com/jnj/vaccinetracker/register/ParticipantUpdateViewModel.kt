package com.jnj.vaccinetracker.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jnj.vaccinetracker.common.data.managers.ParticipantManager
import com.jnj.vaccinetracker.common.domain.entities.UpdateParticipant
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