package com.idi.vaccinetracker.common.data.network.apiexceptioninterceptor

import com.idi.vaccinetracker.common.data.repositories.CookieRepository
import com.idi.vaccinetracker.common.di.qualifiers.SyncApi
import com.idi.vaccinetracker.common.exceptions.InvalidSessionException
import com.idi.vaccinetracker.common.helpers.logInfo
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncApiExceptionInterceptor @Inject constructor(
    @SyncApi
    val cookieRepository: CookieRepository,
    moshi: Moshi,
) : ApiExceptionInterceptorBase(moshi) {
    override fun onSessionExpired() {
        logInfo("onSessionExpired")
        cookieRepository.clearSessionCookieBlocking()
        throw InvalidSessionException()
    }
}