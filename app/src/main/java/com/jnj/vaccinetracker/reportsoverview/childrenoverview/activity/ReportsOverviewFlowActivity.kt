package com.jnj.vaccinetracker.reportsoverview.childrenoverview.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.logWarn
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityReportsOverviewFlowBinding
import com.jnj.vaccinetracker.reportsoverview.childrenoverview.model.ReportsOverviewViewModel
import com.jnj.vaccinetracker.reportsoverview.childrenoverview.screens.ReportsOverviewFragment

class ReportsOverviewFlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, ReportsOverviewFlowActivity::class.java)
        }
    }

    private val reportsOverviewViewModel: ReportsOverviewViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityReportsOverviewFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { reportsOverviewViewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_reports_overview_flow)
        binding.lifecycleOwner = this

        reportsOverviewViewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen, reportsOverviewViewModel.navigationDirection)
        }
    }

    private fun navigateToScreen(screen: ReportsOverviewViewModel.Screen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            ReportsOverviewViewModel.Screen.REPORTS_OVERVIEW -> ReportsOverviewFragment()
            else -> null
        }
        screen?.let { title = getString(it.title) }

        fragment?.let { newFragment ->
            supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { existingFragment ->
                if (newFragment::class == existingFragment::class) return logWarn("Fragment of this type is already shown, not navigating")
            }

            val transaction = supportFragmentManager.beginTransaction()

            transaction.animateNavigationDirection(navigationDirection)

            transaction
                .replace(R.id.fragment_container, newFragment)
                .commit()
        }
    }
}