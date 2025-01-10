package com.idi.vaccinetracker.common.data.network.apiexceptioninterceptor

import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.exceptions.InvalidSessionException
import com.idi.vaccinetracker.common.helpers.SessionExpiryObserver
import com.idi.vaccinetracker.common.helpers.logInfo
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainApiExceptionInterceptor @Inject constructor(
    private val sessionExpiryObserver: SessionExpiryObserver,
    private val userRepository: UserRepository,
    moshi: Moshi,
) : ApiExceptionInterceptorBase(moshi) {

    override fun onSessionExpired() {
        logInfo("onSessionExpired")
        sessionExpiryObserver.notifySessionExpired()
        userRepository.logOut()
        throw InvalidSessionException()
    }
}