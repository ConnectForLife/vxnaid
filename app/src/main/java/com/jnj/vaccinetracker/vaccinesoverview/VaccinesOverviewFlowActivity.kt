package com.jnj.vaccinetracker.vaccinesoverview

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
import com.jnj.vaccinetracker.databinding.ActivityVaccinesOverviewFlowBinding
import com.jnj.vaccinetracker.vaccinesoverview.model.VaccinesOverviewViewModel
import com.jnj.vaccinetracker.vaccinesoverview.screens.VaccinesOverviewFragment

class VaccinesOverviewFlowActivity : BaseActivity() {

    companion object {
        fun create(context: Context): Intent {
            return Intent(context, VaccinesOverviewFlowActivity::class.java)
        }
    }

    private val vaccinesOverviewViewModel: VaccinesOverviewViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityVaccinesOverviewFlowBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { vaccinesOverviewViewModel.restoreInstanceState(it) }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_vaccines_overview_flow)
        binding.lifecycleOwner = this

        vaccinesOverviewViewModel.currentScreen.observe(this) { screen ->
            navigateToScreen(screen, vaccinesOverviewViewModel.navigationDirection)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToScreen(screen: VaccinesOverviewViewModel.Screen?, navigationDirection: NavigationDirection) {
        val fragment = when (screen) {
            VaccinesOverviewViewModel.Screen.VACCINES_OVERVIEW -> VaccinesOverviewFragment()
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