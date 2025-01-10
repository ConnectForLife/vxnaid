package com.idi.vaccinetracker.participantflow.screens

import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.viewmodel.ViewModelBase
import kotlinx.coroutines.launch
import javax.inject.Inject

class ParticipantFlowMotherNameViewModel @Inject constructor(
    override val dispatchers: AppCoroutineDispatchers
) : ViewModelBase() {

    val canSubmit = mutableLiveBoolean()
    val motherName = mutableLiveData<String>()
    val confirmWithNullValuesEvent = eventFlow<Unit>()

    fun validateInput(motherName: String?) {
        if (motherName.isNullOrEmpty()) {
            canSubmit.set(false)
        } else {
            this.motherName.set(motherName)
            canSubmit.set(true)
        }
    }

    fun onSkipButtonClick() {
        scope.launch {
            confirmWithNullValuesEvent.tryEmit(Unit)
        }
    }
}