package com.idi.vaccinetracker.participantflow.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.helpers.findParent
import com.idi.vaccinetracker.common.ui.BaseDialogFragment
import com.idi.vaccinetracker.databinding.DialogReportAdverseEffectsSuccessfulBinding

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
            findParent<OnAdverseEffectsSuccess>()?.onAdverseEffectsSuccess()
        }
        return binding.root
    }

    interface OnAdverseEffectsSuccess {
        fun onAdverseEffectsSuccess()
    }
}