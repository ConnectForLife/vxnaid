package com.jnj.vaccinetracker.visitsoverview

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityVisitOverviewFlowBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { visitsOverviewViewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_visit_overview_flow)
        binding.lifecycleOwner = this

        visitsOverviewViewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen.toString(), visitsOverviewViewModel.navigationDirection)
        }
    }

    private fun navigateToScreen(screenKey: String, navigationDirection: NavigationDirection) {
        val screen = visitsOverviewViewModel.getScreenByKey(screenKey)
        val fragment = when (screen) {
            VisitsOverviewViewModel.Screen.VISITS_OVERVIEW -> VisitsOverviewFragment()
            VisitsOverviewViewModel.Screen.SCHEDULED_VISITS -> ScheduledVisitsFragment()
            VisitsOverviewViewModel.Screen.HISTORICAL_VISITS -> HistoricalVisitsFragment()
            VisitsOverviewViewModel.Screen.MISSED_VISITS -> MissedVisitsFragment()
            else -> null
        }

        screen?.let { title = getString(it.title) }

        fragment?.let { newFragment ->
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) {
                    logWarn("Fragment of this type is already shown, not navigating")
                    return
                }
            }

            supportFragmentManager.beginTransaction()
                .animateNavigationDirection(navigationDirection)
                .replace(R.id.fragment_container, newFragment)
                .commit()
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        visitsOverviewViewModel.saveInstanceState(outState)
    }
}
