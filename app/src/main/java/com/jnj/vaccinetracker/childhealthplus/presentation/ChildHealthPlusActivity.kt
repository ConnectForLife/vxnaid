package com.jnj.vaccinetracker.childhealthplus.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusClientInfoFragment
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusConfirmationFragment
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusDoseSelectionFragment
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusAdministrationDateFragment
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusNextVisitDateFragment
import com.jnj.vaccinetracker.childhealthplus.presentation.screens.ChildHealthPlusServiceSelectionFragment
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.databinding.ActivityChildHealthPlusBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Activity that hosts the Child Health+ workflow
 * Manages the flow of simplified client registration and service documentation
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusActivity : BaseActivity() {

    companion object {
        private const val EXTRA_PARTICIPANT = "participant"

        fun create(context: Context, participant: ParticipantSummaryUiModel? = null): Intent {
            return Intent(context, ChildHealthPlusActivity::class.java).apply {
                if (participant != null) {
                    putExtra(EXTRA_PARTICIPANT, participant)
                }
            }
        }
    }

    private val viewModel: ChildHealthPlusViewModel by viewModels { viewModelFactory }
    private lateinit var binding: ActivityChildHealthPlusBinding

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_child_health_plus)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        // Load participant if available
        val participant = intent.getParcelableExtra<ParticipantSummaryUiModel>(EXTRA_PARTICIPANT)

        // Setup initial fragment
        if (savedInstanceState == null) {
            showFragment(ChildHealthPlusClientInfoFragment())
        }

        // Observe view model stage changes to navigate between fragments
        viewModel.currentStage.observe(this) { stage ->
            when (stage) {
                ChildHealthPlusViewModel.WorkflowStage.CLIENT_INFO ->
                    showFragment(ChildHealthPlusClientInfoFragment())
                ChildHealthPlusViewModel.WorkflowStage.SERVICE_SELECTION ->
                    showFragment(ChildHealthPlusServiceSelectionFragment())
                ChildHealthPlusViewModel.WorkflowStage.DOSE_SELECTION ->
                    showFragment(ChildHealthPlusDoseSelectionFragment())
                ChildHealthPlusViewModel.WorkflowStage.ADMINISTRATION_DATE ->
                    showFragment(ChildHealthPlusAdministrationDateFragment())
                ChildHealthPlusViewModel.WorkflowStage.NEXT_VISIT_DATE ->
                    showFragment(ChildHealthPlusNextVisitDateFragment())
                ChildHealthPlusViewModel.WorkflowStage.CONFIRMATION ->
                    showFragment(ChildHealthPlusConfirmationFragment())
                ChildHealthPlusViewModel.WorkflowStage.COMPLETED -> {
                    // Return to home
                    finish()
                }
                null -> {
                    // Do nothing if stage is null
                }
            }
        }

        // Observe submission success
        lifecycleScope.launch {
            viewModel.submitSuccessEvent.asFlow().collect { data ->
                // Show success message
                Snackbar.make(binding.root, R.string.child_health_plus_success, Snackbar.LENGTH_SHORT)
                    .show()
                // Return to home
                val intent = Intent().apply {
                    putExtra("child_health_plus_data", data.toString())
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        // Observe submission failed
        lifecycleScope.launch {
            viewModel.submitFailedEvent.asFlow().collect { error ->
                // Show error message to user
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onBackPressed() {
        viewModel.goBack()
        if (viewModel.currentStage.value == ChildHealthPlusViewModel.WorkflowStage.CLIENT_INFO) {
            super.onBackPressed()
        }
    }
}

