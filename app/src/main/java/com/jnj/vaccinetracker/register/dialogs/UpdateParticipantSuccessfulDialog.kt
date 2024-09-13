package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRegisterParticipantSuccessfulBinding
import com.jnj.vaccinetracker.databinding.DialogUpdateParticipantSuccessfulBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog
import com.jnj.vaccinetracker.visit.screens.ContraindicationsActivity

/**
 * @author maartenvangiel
 * @version 1
 */
class UpdateParticipantSuccessfulDialog : BaseDialogFragment() {
    private lateinit var binding: DialogUpdateParticipantSuccessfulBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_update_participant_successful, container, false)
        binding.btnOk.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }
}