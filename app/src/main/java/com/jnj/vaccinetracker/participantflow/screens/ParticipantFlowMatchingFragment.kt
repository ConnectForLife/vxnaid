package com.jnj.vaccinetracker.participantflow.screens

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.IrisPosition
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowMatchingBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel
import com.jnj.vaccinetracker.participantflow.dialogs.ParticipantFlowMissingIdentifiersDialog
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.dialogs.TransferClinicDialog
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.VisitActivity
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class ParticipantFlowMatchingFragment : BaseFragment() {

    private companion object {
        private const val TAG_MISSING_IDENTIFIERS = "missingIdentifiersDialog"
        private const val REQ_RELOAD = 55
    }

    private val viewModel: ParticipantFlowMatchingViewModel by viewModels { viewModelFactory }
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowMatchingBinding
    private lateinit var adapter: ParticipantFlowMatchingAdapter
    @Inject lateinit var syncSettingsRepository: SyncSettingsRepository

    private var errorSnackbar: Snackbar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_matching, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        adapter = ParticipantFlowMatchingAdapter(::onItemSelected)
        binding.recyclerViewParticipants.adapter = adapter

        val currentLocationUuid = syncSettingsRepository.getSiteUuid()

        binding.btnNewParticipant.setOnClickListener {
            viewModel.getSelectedParticipantSummary()?.let { startParticipantEdit(it) }
        }
        binding.btnMatchParticipant.setOnClickListener {
            if (viewModel.selectedParticipant.value?.siteUUID != currentLocationUuid) {
                viewModel.selectedParticipant.value?.let { TransferClinicDialog(it, currentLocationUuid!!).show(childFragmentManager, "transferClinicDialog") }
            } else {
                viewModel.getSelectedParticipantSummary()?.let { startParticipantVisitContraindications(it, false) }
            }

        }

        binding.btnReportAdverseEffects.setOnClickListener {
            viewModel.getSelectedParticipantSummary()?.let {startParticipantReportAdverseEffects(it)}
        }

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        logInfo("observeViewModel")
        viewModel.noIdentifierUsed
            .asFlow()
            .onEach {
                ParticipantFlowMissingIdentifiersDialog().show(requireActivity().supportFragmentManager, TAG_MISSING_IDENTIFIERS)
            }.launchIn(lifecycleOwner)

        viewModel.items.observe(lifecycleOwner) { items ->
            adapter.updateItems(items.orEmpty())
        }
        viewModel.errorMessage.observe(lifecycleOwner) { errorMessage ->
            errorSnackbar?.dismiss()

            if (errorMessage == null) {
                return@observe
            }

            errorSnackbar = Snackbar
                .make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.general_label_retry) {
                    errorSnackbar?.dismiss()
                    viewModel.onRetryClick()
                }.also { it.show() }
        }
        viewModel.launchRegistrationFlowEvents.asFlow().onEach {
            startActivityForResult(
                RegisterParticipantFlowActivity.create(
                    context = requireContext(),
                    participantId = flowViewModel.participantId.value,
                    isManualEnteredParticipantId = flowViewModel.isManualSetParticipantId.value,
                    irisScannedLeft = flowViewModel.irisScans[IrisPosition.LEFT] ?: false,
                    irisScannedRight = flowViewModel.irisScans[IrisPosition.RIGHT] ?: false,
                    countryCode = flowViewModel.phoneCountryCode.value,
                    phoneNumber = flowViewModel.participantPhone.value
                ), Constants.REQ_REGISTER_PARTICIPANT
            )
            (requireActivity() as BaseActivity).setForwardAnimation()
        }.launchIn(lifecycleOwner)
        viewModel.participantIdGenerated.observe(this) { participantIdGenerated ->
            if (participantIdGenerated != null) {
                flowViewModel.setAutoGeneratedParticipantId(participantIdGenerated)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val participantId = flowViewModel.participantId.value.takeIf { !it.isNullOrEmpty() }
        val irisScans = flowViewModel.irisScans.takeIf { scans -> scans.values.any { it } }
        val phone = flowViewModel.getFullPhoneNumber()

        viewModel.setArguments(ParticipantFlowMatchingViewModel.Args(participantId = participantId,
            irisScans = irisScans,
            phone = phone))
    }

    private fun onItemSelected(matchingListItem: ParticipantFlowMatchingViewModel.MatchingListItem) {
        val selectedParticipant = viewModel.setSelectedParticipant(matchingListItem)
        if (selectedParticipant != null) {
            flowViewModel.selectedParticipant.value = selectedParticipant
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            Constants.REQ_REGISTER_PARTICIPANT -> {
                val participant = data?.getParcelableExtra<ParticipantSummaryUiModel>(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT)
                if (participant == null) {
                    // If no participant passed, we will return to the start of the identification flow
                    startActivity(ParticipantFlowActivity.create(requireContext()))
                } else {
                    // If participant passed, we continue
                    startParticipantVisitContraindications(participant, true)
                }
                requireActivity().finish()
            }
            Constants.REQ_VISIT -> {
                flowViewModel.reset()
            }
            REQ_RELOAD -> { viewModel.initState() }
        }
    }

    private fun startParticipantVisitContraindications(participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean) {
        startActivity(VisitActivity.create(requireContext(), participant, newRegisteredParticipant))
        (requireActivity() as BaseActivity).setForwardAnimation()
    }

    private fun startParticipantReportAdverseEffects(participant: ParticipantSummaryUiModel) {
        flowViewModel.onStartAdverseEffects()
    }

    private fun startParticipantEdit(participant: ParticipantSummaryUiModel) {
        startActivityForResult(
            RegisterParticipantFlowActivity.create(
                context = requireContext(),
                participantId = null,
                isManualEnteredParticipantId = null,
                irisScannedLeft = false,
                irisScannedRight = false,
                countryCode = null,
                phoneNumber = null,
                participantUuid = participant.participantUuid
            ), REQ_RELOAD
        )
        (requireActivity() as BaseActivity).setForwardAnimation()
    }
}