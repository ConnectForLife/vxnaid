package com.idi.vaccinetracker.sync.domain.services

import com.idi.vaccinetracker.common.data.managers.LicenseManager
import com.idi.vaccinetracker.common.data.models.LicenseType
import com.idi.vaccinetracker.common.exceptions.ManualLicenseActivationRequiredException
import com.idi.vaccinetracker.common.helpers.AppCoroutineDispatchers
import com.idi.vaccinetracker.common.helpers.debugLabel
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.common.helpers.logWarn
import com.idi.vaccinetracker.config.Counters
import com.idi.vaccinetracker.sync.data.helpers.ServerPollUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseSyncService @Inject constructor(
    private val dispatchers: AppCoroutineDispatchers,
    private val licenseManager: LicenseManager,
    private val serverPollUtil: ServerPollUtil,
) {
    companion object {
        private val counter = Counters.LicenseSync
    }

    private val job = SupervisorJob()
    private val scope
        get() = CoroutineScope(dispatchers.mainImmediate + job)

    private var pollServerJob: Job? = null

    fun start() {
        if (pollServerJob?.isActive != true) {
            pollServerJob = scope.launch {
                pollServerPeriodically()
            }
        }
    }

    private suspend fun pollServer(): Boolean {
        return try {
            val licenseStatus = licenseManager.getLicenses(
                swallowErrors = true,
                fromUserInput = false,
                licenseTypes = listOf(LicenseType.IRIS_CLIENT, LicenseType.IRIS_MATCHING)
            )
            logInfo("pollServer getLicenses $licenseStatus")
            !licenseStatus.isObtained
        } catch (ex: ManualLicenseActivationRequiredException) {
            logWarn("manual license activation required")
            false
        }
    }


    private suspend fun pollServerPeriodically() {
        serverPollUtil.pollServerPeriodically(counter.DELAY_NO_LICENSE, debugLabel()) { pollServer() }
    }
}