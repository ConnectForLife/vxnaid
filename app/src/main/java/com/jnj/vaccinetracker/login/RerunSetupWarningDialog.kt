package com.jnj.vaccinetracker.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogRerunSetupWarningBinding
import com.jnj.vaccinetracker.settings.SettingsViewModel

class RerunSetupWarningDialog : BaseDialogFragment() {

    private val viewModel: SettingsViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding: DialogRerunSetupWarningBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_rerun_setup_warning,
            container,
            false
        )

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnContinue.setOnClickListener {
            viewModel.rerunSetupWizard()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        const val TAG = "RerunSetupWarningDialog"
    }
}