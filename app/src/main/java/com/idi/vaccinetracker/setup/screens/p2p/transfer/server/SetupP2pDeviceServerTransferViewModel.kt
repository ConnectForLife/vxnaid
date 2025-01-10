package com.idi.vaccinetracker.setup.screens.p2p.transfer.server

import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.di.ResourcesWrapper
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.setup.screens.p2p.transfer.base.SetupP2pDeviceTransferViewModelBase
import com.idi.vaccinetracker.sync.p2p.common.models.NsdSession
import com.idi.vaccinetracker.sync.p2p.domain.entities.ServerProgress
import com.idi.vaccinetracker.sync.p2p.domain.services.P2pServer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class SetupP2pDeviceServerTransferViewModel @Inject constructor(
    dispatchers: AppCoroutineDispatchers,
    private val server: P2pServer,
    override val userRepository: UserRepository, override val resourcesWrapper: ResourcesWrapper,
) : SetupP2pDeviceTransferViewModelBase(dispatchers) {

    val progress: StateFlow<ServerProgress> get() = server.serverProgress
    val nsdSession: StateFlow<NsdSession?> get() = server.nsdSession
    val finishScreenEvent = eventFlow<Unit>()
    val showConfirmFinishServicePopupEvent = eventFlow<Unit>()

    init {
        initState()
    }

    fun onConfirmStopService() {
        finishScreenEvent.tryEmit(Unit)
    }

    fun onStopBroadcastClick() {
        showConfirmFinishServicePopupEvent.tryEmit(Unit)
    }

    override fun initState() {
        super.initState()
        server.errorMessage
            .onEach { errorMessage.value = it }
            .launchIn(scope)
        scope.launch {
            server.registerService()
        }
    }

    override fun onCleared() {
        super.onCleared()
        server.dispose()
    }
}