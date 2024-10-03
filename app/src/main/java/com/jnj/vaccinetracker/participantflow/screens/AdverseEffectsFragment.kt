package com.jnj.vaccinetracker.participantflow.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentReferralBinding
import com.jnj.vaccinetracker.databinding.FragmentReportAdverseEffectsBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.participantflow.dialogs.AdverseEffectsSuccessfulDialog
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantUiModel
import com.jnj.vaccinetracker.register.screens.RegisterParticipantHistoricalDataViewModel
import com.jnj.vaccinetracker.register.screens.RegisterParticipantParticipantDetailsFragment
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class AdverseEffectsFragment : BaseFragment(),
    AdverseEffectsSuccessfulDialog.OnAdverseEffectsSuccess
{
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentReportAdverseEffectsBinding

    @Inject
    lateinit var configurationManager: ConfigurationManager

    @Inject
    lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource

    @Inject
    lateinit var visitManager: VisitManager

    companion object {
        private const val TAG_ADVERSE_EFFECTS_SUCCESS_DIALOG = "successAdverseEffectsDialog"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_report_adverse_effects, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.participant = flowViewModel.selectedParticipant.value


        setupListeners()
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        return binding.root
    }

    private fun setupListeners() {
        with(binding) {
            btnSaveAdverseEffect.setOnClickListener { onReferButtonClicked() }
        }
    }

    private fun showErrorMessage(message: String) {
        com.jnj.vaccinetracker.common.dialogs.AlertDialog(requireContext()).showAlertDialog(message)
    }

    fun onReferButtonClicked() {
        val adverseEffectText = binding.editTextAdditionalInfo.text.toString()

        if (!validateInputs(adverseEffectText)) return

        lifecycleScope.launch {
            try {
                // todo Dawid please finish the api call
                vaccineTrackerSyncApiDataSource.reportAdverseEffectForPatient(flowViewModel.selectedParticipant.value!!.participantUuid, adverseEffectText)
                AdverseEffectsSuccessfulDialog().show(childFragmentManager, TAG_ADVERSE_EFFECTS_SUCCESS_DIALOG)
            } catch (e: Exception) {
                Log.e("ReportAdverseEffects", "Reporting of adverse effects failed", e)
                showErrorMessage(getString(R.string.adverse_effects_page_failed_text))
            }
        }
    }

    private fun validateInputs(adverseEffectsText: String): Boolean {
        var isValid = true

        if (adverseEffectsText.isEmpty()) {
            binding.editTextAdditionalInfo.error = getString(R.string.adverse_effects_page_cannot_be_empty)
            isValid = false
        } else {
            binding.editTextAdditionalInfo.error = null
        }

        return isValid
    }

    override fun onAdverseEffectsSuccess() {
        requireActivity().finish()
    }
}
