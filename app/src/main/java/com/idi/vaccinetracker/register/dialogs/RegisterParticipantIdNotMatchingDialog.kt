package com.idi.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.ui.BaseDialogFragment
import com.idi.vaccinetracker.databinding.DialogRegisterParticipantIdNotMatchingBinding

/**
 * @author tnelen
 * @version 1
 */
class RegisterParticipantIdNotMatchingDialog : BaseDialogFragment() {

    private lateinit var binding: DialogRegisterParticipantIdNotMatchingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_register_participant_id_not_matching, container, false)

        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        return binding.root
    }

    interface RegisterParticipantIdNotMatchingConfirmationListener {
        fun dismissAllowingStateLoss()
    }

}