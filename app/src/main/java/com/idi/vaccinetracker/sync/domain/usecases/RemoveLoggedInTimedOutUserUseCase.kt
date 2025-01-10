package com.idi.vaccinetracker.sync.domain.usecases

import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.domain.usecases.masterdata.GetConfigurationUseCase
import com.idi.vaccinetracker.common.helpers.SessionExpiryObserver
import com.idi.vaccinetracker.common.helpers.logInfo
import javax.inject.Inject

class RemoveLoggedInTimedOutUserUseCase @Inject constructor(
    private val getConfigurationUseCase: GetConfigurationUseCase,
    private val userRepository: UserRepository,
    private val sessionExpiryObserver: SessionExpiryObserver,
) {

    suspend fun removeLoggedInUserIfTimedOut() {
        logInfo("removeLoggedInUserIfTimedOut")
        val config = getConfigurationUseCase.getMasterData()
        val sessionTimeout = config.operatorOfflineSessionTimeout
        if (sessionTimeout > 0) {
            val currentTime = dateNow().time
            val loginTime = userRepository.getLoginTime()
            fun isSessionTimedOut() = loginTime?.let { it.time + sessionTimeout < currentTime } ?: false
            val user = userRepository.getUser()
            if (user != null && isSessionTimedOut()) {
                userRepository.deleteUser()
                sessionExpiryObserver.notifySessionExpired()
            }
        }
    }

}