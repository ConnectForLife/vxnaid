package com.jnj.vaccinetracker.participantflow.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogReportAdverseEffectsSuccessfulBinding
import com.jnj.vaccinetracker.databinding.DialogUpdateParticipantSuccessfulBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class AdverseEffectsSuccessfulDialog : BaseDialogFragment() {
    private lateinit var binding: DialogReportAdverseEffectsSuccessfulBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_report_adverse_effects_successful, container, false)
        binding.btnOk.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface OnAdverseEffectsSuccess {
        fun onAdverseEffectsSuccess()
    }
}