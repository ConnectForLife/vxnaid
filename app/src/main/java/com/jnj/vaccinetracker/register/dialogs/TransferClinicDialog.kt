package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogTransferClinicBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.participantflow.model.ParticipantUiModel
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.screens.ContraindicationsActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

class TransferClinicDialog(
    private var participant: ParticipantUiModel,
    private var currentLocationUuid: String
) : BaseDialogFragment() {

    private lateinit var binding: DialogTransferClinicBinding
    @Inject lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource
    @Inject lateinit var syncSettingsRepository: SyncSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_transfer_clinic, container, false)

        val participantSummaryUiModel = ParticipantSummaryUiModel(participant.participantUUID!!, participant.participantId!!,
            participant.gender!!, participant.birthDateText!!, participant.isBirthDateEstimated!!, participant.vaccine, null)
        binding.btnTransferAndContinueVisit.setOnClickListener {
            lifecycleScope.launch {
                try {
                    startParticipantVisitContraindications(participantSummaryUiModel, false)
                    vaccineTrackerSyncApiDataSource.updateParticipantLocation(participant.participantUUID!!, currentLocationUuid)
                } catch (e: Exception) {
                    Log.e("TransferClinicDialog", "Something went wrong during updating participant location", e)
                }
            }
            dismissAllowingStateLoss()
        }

        binding.btnJustVisit.setOnClickListener {
            startParticipantVisitContraindications(participantSummaryUiModel, false)
            dismissAllowingStateLoss()
        }

        return binding.root
    }

    private fun startParticipantVisitContraindications(participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean) {
        startActivity(ContraindicationsActivity.create(requireContext(), participant, newRegisteredParticipant))
        (requireActivity() as BaseActivity).setForwardAnimation()
    }
}
