package com.jnj.vaccinetracker.inappupdate

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdate(val context: Context) {
    private val TAB = "InAppUpdate"
    private val appUpdateManager = AppUpdateManagerFactory.create(context)
    var updateWasRequested = false

    //private val listener: InstallStateUpdatedListener? = null
   // private val DAYS_FOR_FLEXIBLE_UPDATE = 2


    fun checkImmediateUpdate(activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>){
        //this should return an intent object that you use to check for an update
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        //this checks that the platform will allow the spedified type of update
        appUpdateInfoTask.addOnSuccessListener{ appUpdateInfo ->
            val isAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            if(isAllowed && isAvailable && !updateWasRequested){
                print("updating starting.............")
                updateWasRequested = true// this will allow the app not constantly showing up to the users
                startUpdate(
                    appUpdateInfo = appUpdateInfo,
                    activityResultLauncher = activityResultLauncher,
                    appUpdateType = AppUpdateType.IMMEDIATE
                )
            } else {
                print("updating starting   did not start.............")

            }
        }
        appUpdateInfoTask.addOnCanceledListener {
            print("updating the app canceled...............")
        }
        appUpdateInfoTask.addOnFailureListener{ exception ->
            print("updating the app failed........... ${exception.message.toString()}")
        }
    }

    // check resume function

    fun checkResumeUpdate(
        activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>
    ){
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if(appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                print("updating resume....................... update in progress, please wait")

        }
    }





    private fun startUpdate(appUpdateInfo: AppUpdateInfo,
                            activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
                            appUpdateType: Int) {
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo,
            activityResultLauncher,
            AppUpdateOptions.newBuilder(appUpdateType)
                .setAllowAssetPackDeletion(false)
                .build()
            )

    }

}