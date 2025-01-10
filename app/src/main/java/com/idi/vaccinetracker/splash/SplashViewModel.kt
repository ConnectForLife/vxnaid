package com.idi.vaccinetracker.splash

import androidx.lifecycle.ViewModel
import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.sync.domain.helpers.SyncSettingsObserver
import javax.inject.Inject

/**
 * ViewModel for the splash activity
 *
 * @author maartenvangiel
 * @version 1
 */
class SplashViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syncSettingsObserver: SyncSettingsObserver,
) :
    ViewModel() {

    fun getStartTarget(): StartTarget {
        val isAuthenticated = userRepository.isLoggedIn()
        val syncSettingsAvailable = syncSettingsObserver.isSyncSettingsAvailable()
        return when {
            !syncSettingsAvailable -> StartTarget.SETUP
            isAuthenticated -> StartTarget.HOMEPAGE
            else -> StartTarget.LOGIN
        }
    }

    enum class StartTarget {
        LOGIN,
        HOMEPAGE,
        SETUP
    }

}
