package com.jnj.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
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
import com.jnj.vaccinetracker.common.dialogs.AlertDialog
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.helpers.findDosingVisit
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentReferralBinding
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.visit.VisitViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class ReferralFragment : BaseFragment() {

    private lateinit var binding: FragmentReferralBinding
    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }

    @Inject
    lateinit var configurationManager: ConfigurationManager

    @Inject
    lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource

    @Inject
    lateinit var visitManager: VisitManager

    private lateinit var allVisits: List<VisitDetail>
    private var currentVisit: VisitDetail? = null
    private var participantUuid: String? = null
    private var adapter: ArrayAdapter<String>? = null
    private var isReferWithin: Boolean = true

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
        setupReferralWithinFacilityDropdown()

        viewModel.fetchAllLocations()

        lifecycleScope.launch {
            getVisitsForParticipant()
        }

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    private fun initializeViews() {
        with(binding) {
            if (!isAfterVisit) textViewSaveVisit.visibility = View.GONE
            participantUuid = arguments?.getString("participantUuid")
            lifecycleScope.launch {
                if (participantUuid != null) {
                    val allVisits = visitManager.getVisitsForParticipant(participantUuid!!)
                    currentVisit = allVisits.findDosingVisit()
                }
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            btnSaveReferral.setOnClickListener { onReferButtonClicked() }
            btnCancelReferral.setOnClickListener { onDoNotReferClicked() }
            btnCloseReferral.setOnClickListener { requireActivity().finish() }
            switchReferWithinClinic.setOnCheckedChangeListener { _, isChecked ->
                onSwitchChange(isChecked)
            }
        }
    }

    private fun getVisitsForParticipant() {
        lifecycleScope.launch {
            try {
                allVisits = visitManager.getVisitsForParticipant(participantUuid!!)
            } catch (e: Exception) {
                Log.e("ReferralFragment", "Visits fetching failed", e)
                showErrorMessage(getString(R.string.referral_page_failed_referral_text))
            }
        }
    }

    private fun setupReferralWithinFacilityDropdown() {
        val dropdownOptions = listOf("Nutrition Clinic", "EID (Early Infant Diagnosis) Clinic", "Post Natal Care Clinic","Family Planning Clinic", "Others Specify")
        adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, dropdownOptions)
        binding.referralPlaces.setAdapter(adapter)
    }

    private fun showErrorMessage(message: String) {
        AlertDialog(requireContext()).showAlertDialog(message)
    }

    private fun onReferButtonClicked() {
        val referralReason = binding.editTextAdditionalInfo.text.toString()
        val referralPlace = if (isReferWithin) {
            binding.referralPlaces.text.toString()
        } else {
            binding.editTextReferOutsideFacility.text.toString()
        }

        if (!validateInputs(referralPlace, referralReason, isReferWithin)) return

        val referralObservations = createReferralObservations(referralPlace, referralReason)

        lifecycleScope.launch {
            try {
                onRefer(referralPlace)
                if (isAfterVisit) {
                    findParent<OnReferralPageFinishListener>()?.onReferralAfterVisitPageFinish(referralObservations)
                } else {
                    findParent<OnReferralPageFinishListener>()
                        ?.onReferralAfterContraindicationsPageFinish(false, referralObservations)
                }
            } catch (e: Exception) {
                Log.e("ReferralFragment", "Referral failed", e)
                showErrorMessage(getString(R.string.referral_page_failed_referral_text))
            }
        }

        binding.editTextAdditionalInfo.text = null
    }

    fun onRefer(selectedClinic: String) {
        binding.textViewReferralResult.text = "${getString(R.string.referral_page_success_referral_text)} $selectedClinic"
        binding.textViewReferralResult.setTextColor(ContextCompat.getColor(requireContext(), R.color.successDark))
        binding.btnSaveReferral.visibility = View.GONE
        binding.btnCancelReferral.visibility = View.GONE
        if (!isAfterVisit) {
            binding.btnCloseReferral.visibility = View.VISIBLE
        }
    }

    private fun onSwitchChange(isChecked: Boolean) {
        if (isChecked) {
            isReferWithin = false
            setNullInDropdown()
            binding.linearLayoutClinic.visibility = View.GONE
            binding.textViewReferOutsideClinicSwitchLabel.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            binding.textViewReferWithinClinicSwitchLabel.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.colorTextOnLight)
            )
            binding.linearLayoutReferOutsideFacility.visibility = View.VISIBLE
        } else {
            isReferWithin = true
            binding.linearLayoutClinic.visibility = View.VISIBLE
            binding.textViewReferOutsideClinicSwitchLabel.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.colorTextOnLight)
            )
            binding.textViewReferWithinClinicSwitchLabel.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            )
            binding.linearLayoutReferOutsideFacility.visibility = View.GONE
        }
    }

    private fun setNullInDropdown() {
        binding.referralPlaces.setText(null, false)
    }

    private fun onDoNotReferClicked() {
        lifecycleScope.launch {
            if (isAfterVisit) {
                findParent<OnReferralPageFinishListener>()?.onReferralAfterVisitPageFinish()
            } else {
                findParent<OnReferralPageFinishListener>()?.onReferralAfterContraindicationsPageFinish(true)
            }
        }
    }

    private fun createReferralObservations(referralPlace: String, referralReason: String): Map<String, String> {
        return mutableMapOf<String, String>().apply {
            put(Constants.REFERRAL_CLINIC_CONCEPT_NAME, referralPlace)
            put(Constants.REFERRAL_ADDITIONAL_INFO_CONCEPT_NAME, referralReason)
        }
    }

    private fun validateInputs(referralPlace: String, referralReason: String, isReferWithin: Boolean): Boolean {
        var isValid = true
        val errorMessage = getString(R.string.referral_page_referral_clinic_cannot_be_empty)

        if (referralPlace.isEmpty()) {
            if (isReferWithin) {
                binding.referralPlaces.error = errorMessage
            } else {
                binding.editTextReferOutsideFacility.error = errorMessage
            }
            isValid = false
        } else {
            binding.referralPlaces.error = null
        }

        if (referralReason.isEmpty()) {
            binding.editTextAdditionalInfo.error = getString(R.string.referral_page_referral_reason_cannot_be_empty)
            isValid = false
        } else {
            binding.editTextAdditionalInfo.error = null
        }

        return isValid
    }

    interface OnReferralPageFinishListener {
        fun onReferralAfterVisitPageFinish(referralObservations: Map<String, String> = emptyMap())
        fun onReferralAfterContraindicationsPageFinish(finish: Boolean = false,
                                                       referralObservations: Map<String, String> = emptyMap()
        )
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_cancel).isVisible = false
    }
}
