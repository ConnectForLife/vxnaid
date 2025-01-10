package com.idi.vaccinetracker.participantflow.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.data.managers.ConfigurationManager
import com.idi.vaccinetracker.common.data.managers.VisitManager
import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.domain.entities.CreateVisit
import com.idi.vaccinetracker.common.domain.entities.DraftVisit
import com.idi.vaccinetracker.common.domain.entities.VisitDetail
import com.idi.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.idi.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.idi.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.idi.vaccinetracker.common.ui.BaseFragment
import com.idi.vaccinetracker.databinding.FragmentReportAdverseEffectsBinding
import com.idi.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.idi.vaccinetracker.participantflow.dialogs.AdverseEffectsSuccessfulDialog
import com.idi.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import kotlinx.coroutines.launch
import java.util.Date
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

    @Inject
    lateinit var createVisitUseCase: CreateVisitUseCase

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var syncSettingsRepository: SyncSettingsRepository

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
        com.idi.vaccinetracker.common.dialogs.AlertDialog(requireContext()).showAlertDialog(message)
    }

    fun onReferButtonClicked() {
        val adverseEffectText = binding.editTextAdditionalInfo.text.toString()

        if (!validateInputs(adverseEffectText)) return

        lifecycleScope.launch {
            try {
                if (adverseEffectText.isNotEmpty()) {
                    val emptyVisit = createVisitUseCase.createVisit(
                        buildAdverseEffectsVisitObject(
                            flowViewModel.selectedParticipant.value,
                            Date()
                        )
                    )
                    val obsToAdd =
                        mutableMapOf(Constants.ADVERSE_EFFECTS_OBSERVATION to adverseEffectText)
                    visitManager.updateVisitObservations(
                        emptyVisit.toVisitDetail(),
                        flowViewModel.selectedParticipant.value!!.participantUuid,
                        obsToAdd
                    )
                }
                AdverseEffectsSuccessfulDialog().show(childFragmentManager, TAG_ADVERSE_EFFECTS_SUCCESS_DIALOG)
            } catch (e: Exception) {
                Log.e("ReportAdverseEffects", "Reporting of adverse effects failed", e)
                showErrorMessage(getString(R.string.adverse_effects_page_failed_text))
            }
        }
    }

    private fun DraftVisit.toVisitDetail(): VisitDetail {
        return VisitDetail(
            uuid = visitUuid,
            visitType = visitType,
            visitDate = startDatetime,
            attributes = attributes,
            observations = mapOf()
        )
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildAdverseEffectsVisitObject(
        participant: ParticipantSummaryUiModel?,
        visitDate: Date
    ): CreateVisit {
        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Location not available")
        return CreateVisit(
            participantUuid = participant!!.participantUuid,
            visitType = Constants.VISIT_TYPE_ADVERSE_EFFECTS,
            startDatetime = visitDate,
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to Constants.VISIT_TYPE_ADVERSE_EFFECTS,
            )
        )
    }

    override fun onAdverseEffectsSuccess() {
        flowViewModel.navigateBack()
    }
}
