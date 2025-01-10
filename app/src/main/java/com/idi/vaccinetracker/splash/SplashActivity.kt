package com.idi.vaccinetracker.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.idi.vaccinetracker.common.ui.BaseActivity
import com.idi.vaccinetracker.login.LoginActivity
import com.idi.vaccinetracker.participantflow.ParticipantFlowActivity
import com.idi.vaccinetracker.setup.SetupFlowActivity

/**
 * @author maartenvangiel
 * @version 1
 */
class SplashActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, SplashActivity::class.java)
        }
    }

    private val viewModel: SplashViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = when (viewModel.getStartTarget()) {
            SplashViewModel.StartTarget.LOGIN -> LoginActivity.create(this)
            SplashViewModel.StartTarget.HOMEPAGE -> ParticipantFlowActivity.create(this)
            SplashViewModel.StartTarget.SETUP -> SetupFlowActivity.create(this)
        }

        startActivity(intent)
        finish()
        setBackAnimation()
    }

}