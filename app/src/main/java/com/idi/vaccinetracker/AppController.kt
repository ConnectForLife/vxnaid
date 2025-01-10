package com.idi.vaccinetracker

import android.content.Context
import com.idi.vaccinetracker.common.data.managers.LicenseManager
import com.idi.vaccinetracker.common.helpers.LogWriter
import com.idi.vaccinetracker.common.helpers.Logger
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.config.appSettings
import com.idi.vaccinetracker.sync.p2p.domain.usecases.RemoveStaleDatabaseCopyUseCase
import javax.inject.Inject
import javax.inject.Provider

class AppController @Inject constructor(
    private val context: Context,
    private val logWriterProvider: Provider<LogWriter>,
    private val removeStaleDatabaseCopyUseCase: RemoveStaleDatabaseCopyUseCase,
) {

    fun initState() {
        setupLogging()
        LicenseManager.initState(context)
        removeStaleDatabaseCopyUseCase.removeStaleDatabaseCopy(deviceNameChanged = false)
    }

    private fun setupLogging() {
        Logger.ENABLED = appSettings.logConfig.hasTargets
        if (appSettings.logConfig.fileLoggingEnabled) {
            val defaultExHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                logError("uncaught exception", e)
                defaultExHandler?.uncaughtException(t, e)
            }
            Logger.logWriter = logWriterProvider.get()
        }
    }
}