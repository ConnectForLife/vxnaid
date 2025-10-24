package com.jnj.vaccinetracker.visitsoverview.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityVisitOverviewFlowBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.visitsoverview.model.VisitsOverviewViewModel
import com.jnj.vaccinetracker.visitsoverview.screens.VisitsOverviewFragment

@RequiresApi(Build.VERSION_CODES.O)
class VisitsOverviewFlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, VisitsOverviewFlowActivity::class.java)
        }
    }

    private val visitsOverviewViewModel: VisitsOverviewViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityVisitOverviewFlowBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { visitsOverviewViewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_visit_overview_flow)
        binding.lifecycleOwner = this

        if (savedInstanceState == null) {
            visitsOverviewViewModel.currentScreen.observe(this) { screen ->
                navigateToScreen(screen, visitsOverviewViewModel.navigationDirection)
            }
        } else {
            visitsOverviewViewModel.currentScreen.observe(this) { screen ->
                screen?.let { title = getString(it.title) }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToScreen(screen: VisitsOverviewViewModel.Screen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            VisitsOverviewViewModel.Screen.VISITS_OVERVIEW -> VisitsOverviewFragment()
            else -> null
        }
        screen?.let { title = getString(it.title) }

        fragment?.let { newFragment ->
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) return logWarn("Fragment of this type is already shown, not navigating")
            }

            val transaction = supportFragmentManager.beginTransaction()
            transaction.animateNavigationDirection(navigationDirection)
            transaction.replace(R.id.fragment_container, newFragment).commit()
        }
    }

    fun goToRegisteredParticipant(childID: String) {
        val intent = Intent(this, ParticipantFlowActivity::class.java)
        intent.putExtra(Constants.CALL_NAVIGATE_TO_MATCH_SCREEN, true)
        intent.putExtra(Constants.PARTICIPANT_MATCH_ID, childID)
        startActivity(intent)
        finish()
    }
}
