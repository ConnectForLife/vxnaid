package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRegisterParticipantHasChildEverVaccinatedBinding

class RegisterParticipantHasChildEverVaccinatedDialog : BaseDialogFragment() {

    private lateinit var binding: DialogRegisterParticipantHasChildEverVaccinatedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_register_participant_has_child_ever_vaccinated, container, false)
        binding.btnYes.setOnClickListener {
            showFollowDatesInfoDialog()
        }
        binding.btnNo.setOnClickListener {
            findParent<RegisterParticipationIsChildNewbornListener>()?.continueRegistrationWithSuccessDialog()
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    private fun showFollowDatesInfoDialog() {
        val infoDialog = PastVisitDateInfoDialog.newInstance(
            getString(R.string.medical_worker_follow_dates_title),
            getString(R.string.medical_worker_follow_dates_message),
            getString(R.string.continue_button_label)
        ) {
            findParent<RegisterParticipationIsChildNewbornListener>()?.continueRegistrationWithCaptureVaccinesPage()
            dismissAllowingStateLoss()
        }
        infoDialog.show(parentFragmentManager, "infoDialog")
    }

    interface RegisterParticipationIsChildNewbornListener {
        fun continueRegistrationWithSuccessDialog()
        fun continueRegistrationWithCaptureVaccinesPage()
    }
}