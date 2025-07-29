package com.jnj.vaccinetracker.visit.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogDosingOutOfWindowBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class DosingOutOfWindowDialog : BaseDialogFragment() {

    private lateinit var binding: DialogDosingOutOfWindowBinding

    companion object {
        fun newInstance(showConfirmButton: Boolean): DosingOutOfWindowDialog {
            val args = Bundle()
            args.putBoolean("showConfirmButton", showConfirmButton)
            val dialog = DosingOutOfWindowDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_dosing_out_of_window, container, false)
        binding.executePendingBindings()

        val showConfirm = arguments?.getBoolean("showConfirmButton") ?: true
        binding.btnConfirm.visibility = if (showConfirm) View.VISIBLE else View.GONE

        binding.btnConfirm.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
            findParent<DosingOutOfWindowDialogListener>()?.onOutOfWindowDosingCanceled()
        }
        return binding.root
    }

    interface DosingOutOfWindowDialogListener {
        fun onOutOfWindowDosingCanceled()
    }
}