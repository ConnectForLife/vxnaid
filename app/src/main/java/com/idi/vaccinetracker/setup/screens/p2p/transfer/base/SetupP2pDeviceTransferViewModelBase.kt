package com.idi.vaccinetracker.setup.screens.p2p.transfer.base

import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.di.ResourcesWrapper
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.viewmodel.ViewModelBase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

abstract class SetupP2pDeviceTransferViewModelBase(override val dispatchers: AppCoroutineDispatchers) : ViewModelBase() {

    protected abstract val userRepository: UserRepository
    protected abstract val resourcesWrapper: ResourcesWrapper
    val deviceName = mutableLiveData<String?>()
    val errorMessage = mutableLiveData<String?>()

    protected open fun initState() {
        userRepository.observeDeviceName().onEach {
            deviceName.value = it
        }.launchIn(scope)
    }
}