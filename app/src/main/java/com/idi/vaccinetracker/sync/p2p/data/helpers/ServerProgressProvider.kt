package com.idi.vaccinetracker.sync.p2p.data.helpers

import com.idi.vaccinetracker.sync.p2p.domain.entities.ServerProgress
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerProgressProvider @Inject constructor() {
    val serverProgress = MutableStateFlow<ServerProgress>(ServerProgress.Idle)
}