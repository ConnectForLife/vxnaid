package com.jnj.vaccinetracker.playstore

import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.jnj.vaccinetracker.R

class HomeActivity : AppCompatActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdates(appUpdateManager)
    }

    override fun onResume() {
        super.onResume()
        setupAppUpdateListeners(appUpdateManager)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterAppUpdateListeners(appUpdateManager)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == APP_UPDATE_REQUEST_CODE && resultCode != RESULT_OK){
            toastError(getString(R.string.str_app_update_fail))
        }
    }
}