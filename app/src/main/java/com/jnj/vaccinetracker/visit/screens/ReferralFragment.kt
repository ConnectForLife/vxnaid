package com.jnj.vaccinetracker.visit.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
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
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class ReferralFragment : BaseFragment(), RescheduleVisitDialog.RescheduleVisitListener {

    private lateinit var binding: FragmentReferralBinding
    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }

    @Inject
    lateinit var configurationManager: ConfigurationManager

    @Inject
    lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource

    @Inject
    lateinit var visitManager: VisitManager

    private lateinit var allVisits: List<VisitDetail>
    private var currentVisitUuid: String? = null
    private var participantUuid: String? = null

    private var referAfterDialogSave: Boolean = false

    val isAfterVisit: Boolean by lazy {
        requireArguments().getBoolean(IS_AFTER_VISIT, false)
    }

    companion object {
        private const val IS_AFTER_VISIT = "isAfterVisit"

        fun create(isAfterVisit: Boolean = false): ReferralFragment {
            return ReferralFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_AFTER_VISIT, isAfterVisit)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_referral, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        initializeViews()
        setupListeners()
        fetchLocations()

        return binding.root
    }

    private fun initializeViews() {
        with(binding) {
            if (!isAfterVisit) textViewSaveVisit.visibility = View.INVISIBLE
            currentVisitUuid = arguments?.getString("currentVisitUuid")
            participantUuid = arguments?.getString("participantUuid")
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnSaveReferral.setOnClickListener { onReferButtonClicked() }
            btnCancelReferral.setOnClickListener { onDoNotReferClicked() }
            btnCloseReferral.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        }
    }

    private fun fetchLocations() {
        lifecycleScope.launch {
            try {
                val locations = configurationManager.getSites()
                val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, locations.map { it.name })
                binding.dropdownClinics.setAdapter(adapter)
                allVisits = visitManager.getVisitsForParticipant(participantUuid!!)
            } catch (e: Exception) {
                Log.e("ReferralFragment", "Locations fetching failed", e)
                showErrorMessage(getString(R.string.referral_page_failed_referral_text))
            }
        }
    }

    private fun showErrorMessage(message: String) {
        com.jnj.vaccinetracker.common.dialogs.AlertDialog(requireContext()).showAlertDialog(message)
    }

    fun onReferButtonClicked(saveVisit: Boolean = true) {
        val selectedClinic = binding.dropdownClinics.text.toString()
        val referralReason = binding.editTextAdditionalInfo.text.toString()

        if (!validateInputs(selectedClinic, referralReason)) return

        val referralObservations = createReferralObservations(selectedClinic, referralReason)

        lifecycleScope.launch {
            try {
                if (isAfterVisit && saveVisit) {
                    onRefer(referralObservations, selectedClinic)
                } else if (!isAfterVisit) {
                    referAfterDialogSave = true
                    showRescheduleVisitDialog()
                }
            } catch (e: Exception) {
                Log.e("ReferralFragment", "Referral failed", e)
                showErrorMessage(getString(R.string.referral_page_failed_referral_text))
            }
        }
    }

    private suspend fun onRefer(referralObservations: Map<String, String>, selectedClinic: String) {
        vaccineTrackerSyncApiDataSource.updateEncounterObservationsByVisit(currentVisitUuid!!, referralObservations)
        binding.textViewReferralResult.text = "${getString(R.string.referral_page_success_referral_text)} $selectedClinic"
        binding.textViewReferralResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.successDark))
        binding.btnSaveReferral.visibility = View.GONE
        binding.btnCancelReferral.visibility = View.GONE
        if (!isAfterVisit) {
            binding.btnCloseReferral.visibility = View.VISIBLE
        }
    }

    private fun onDoNotReferClicked() {
        lifecycleScope.launch {
            if (isAfterVisit) {
                viewModel.isReferring.value = false
                findParent<OnReferralPageFinishListener>()?.onReferralPageFinish()
            } else {
                referAfterDialogSave = false
                showRescheduleVisitDialog()
            }
        }
    }

    private fun showRescheduleVisitDialog() {
        RescheduleVisitDialog.create(participant = viewModel.participant.value)
            .show(parentFragmentManager, RescheduleVisitDialog.TAG_DIALOG_RESCHEDULE_VISIT)
    }

    private fun createReferralObservations(selectedClinic: String, referralReason: String): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            put(Constants.REFERRAL_CLINIC_CONCEPT_NAME, selectedClinic)
            put(Constants.REFERRAL_ADDITIONAL_INFO_CONCEPT_NAME, referralReason)
        }
    }

    private fun validateInputs(selectedClinic: String, referralReason: String): Boolean {
        var isValid = true

        if (selectedClinic.isEmpty()) {
            binding.dropdownClinics.error = getString(R.string.referral_page_referral_clinic_cannot_be_empty)
            isValid = false
        } else {
            binding.dropdownClinics.error = null
        }

        if (referralReason.isEmpty()) {
            binding.editTextAdditionalInfo.error = getString(R.string.referral_page_referral_reason_cannot_be_empty)
            isValid = false
        } else {
            binding.editTextAdditionalInfo.error = null
        }

        return isValid
    }

    private fun goToSplashActivity() {
        val intent = Intent(requireContext(), SplashActivity::class.java)
        startActivity(intent)
        requireActivity().finishAffinity()
    }


    interface OnReferralPageFinishListener {
        fun onReferralPageFinish()
    }

    override fun onRescheduleVisitListener() {
        if (referAfterDialogSave) {
            onReferButtonClicked(saveVisit = false)
        } else {
            goToSplashActivity()
        }
    }
}
