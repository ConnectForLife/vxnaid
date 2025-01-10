package com.idi.vaccinetracker.participantflow.screens

import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.validators.ParticipantIdValidator
import com.idi.vaccinetracker.common.viewmodel.ViewModelBase
import kotlinx.coroutines.launch
import javax.inject.Inject

class ParticipantFlowParticipantIdViewModel @Inject constructor(override val dispatchers: AppCoroutineDispatchers, private val participantIdValidator: ParticipantIdValidator) :
    ViewModelBase() {

    val canSubmit = mutableLiveBoolean()
    val canSkip = mutableLiveBoolean()

    fun validateInput(participantId: String?) {
        scope.launch {
            canSubmit.set(participantId != null && participantIdValidator.validate(participantId))
        }
    }

}
