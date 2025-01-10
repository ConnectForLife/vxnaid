package com.idi.vaccinetracker.setup.screens.p2p.device_role

import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.viewmodel.ViewModelBase
import com.idi.vaccinetracker.setup.models.P2pDeviceRole
import javax.inject.Inject

class SetupP2pDeviceRoleViewModel @Inject constructor(override val dispatchers: AppCoroutineDispatchers) : ViewModelBase() {

    val deviceRole = mutableLiveData<P2pDeviceRole?>()

    val canContinue = mutableLiveBoolean()
    val showMessage = mutableLiveBoolean()

    val confirmP2pDeviceRoleEvent = eventFlow<P2pDeviceRole>()

    fun onDeviceRoleSelected(deviceRole: P2pDeviceRole) {
        if (deviceRole == this.deviceRole.value) return
        this.deviceRole.value = deviceRole
        updateButtonStates()
    }

    private fun updateButtonStates() {
        canContinue.value = deviceRole.value != null
        showMessage.value = deviceRole.value == null
    }

    fun onContinueClick() {
        val deviceRole = deviceRole.value
        if (deviceRole != null) {
            confirmP2pDeviceRoleEvent.tryEmit(deviceRole)
        } else {
            this.showMessage.value = true
        }
    }

}