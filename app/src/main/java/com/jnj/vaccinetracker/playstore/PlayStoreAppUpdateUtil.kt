package com.jnj.vaccinetracker.playstore

import android.app.Activity
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.listener.InstallStateUpdatedListener
import com.google.android.play.core.tasks.Task
import javax.inject.Inject

class PlayStoreAppUpdateUtil @Inject constructor() {

    companion object {
        const val APP_UPDATE_REQUEST_CODE = 100
    }

    private lateinit var appUpdateManager: AppUpdateManager

    private fun getUpdateType(): Int {
        return AppUpdateType.FLEXIBLE
    }

    fun checkForAppUpdates(activity: Activity) {
        appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask: Task<AppUpdateInfo> = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { info ->
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = when (getUpdateType()) {
                AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
                AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
                else -> false
            }

            if (isUpdateAvailable && isUpdateAllowed) {
                activity.requestAppUpdate(appUpdateManager, info, getUpdateType())
            }
        }
    }

    fun setupAppUpdateListeners(activity: Activity) {
        val updateType = getUpdateType()

        when (updateType) {
            AppUpdateType.FLEXIBLE -> activity.setupFlexibleUpdateSuccessListener(appUpdateManager)
            AppUpdateType.IMMEDIATE -> activity.setupImmediateUpdateSuccessListener(appUpdateManager)
        }

        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(activity.getInstallStateUpdateListener(appUpdateManager))
        }
    }

    fun unregisterAppUpdateListeners(activity: Activity) {
        if (getUpdateType() == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(activity.getInstallStateUpdateListener(appUpdateManager))
        }
    }
}

// Extensions for Activity
private fun Activity.requestAppUpdate(
    appUpdateManager: AppUpdateManager,
    appUpdateInfo: AppUpdateInfo,
    updateType: Int
) {
    try {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            this,
            AppUpdateOptions.newBuilder(updateType).build(),
            PlayStoreAppUpdateUtil.APP_UPDATE_REQUEST_CODE
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun Activity.setupImmediateUpdateSuccessListener(appUpdateManager: AppUpdateManager) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
            requestAppUpdate(appUpdateManager, appUpdateInfo, AppUpdateType.IMMEDIATE)
        }
    }
}

private fun Activity.setupFlexibleUpdateSuccessListener(appUpdateManager: AppUpdateManager) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
            showInstallSnackBar(appUpdateManager)
        }
    }
}

private fun Activity.getInstallStateUpdateListener(appUpdateManager: AppUpdateManager) =
    InstallStateUpdatedListener {
        if (it.installStatus() == InstallStatus.DOWNLOADED) {
            this.showInstallSnackBar(appUpdateManager)
        }
    }

private fun Activity.showInstallSnackBar(appUpdateManager: AppUpdateManager) {
    Snackbar.make(
        findViewById(android.R.id.content),
        getString(R.string.str_download_complete),
        Snackbar.LENGTH_INDEFINITE
    ).apply {
        setAction(getString(R.string.str_restart)) {
            appUpdateManager.completeUpdate()
        }
        show()
    }
}
