package com.jnj.vaccinetracker.visit.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.databinding.DialogDosingOutOfWindowBinding

/**
 * @author maartenvangiel
 * @version 1
 */
class DosingOutOfWindowDialog : BottomSheetDialogFragment() {

    private lateinit var binding: DialogDosingOutOfWindowBinding

    companion object {
        fun newInstance(showConfirmButton: Boolean, descriptionResId: Int, scheduledDate: String? = null): DosingOutOfWindowDialog {
            val args = Bundle()
            args.putBoolean("showConfirmButton", showConfirmButton)
            args.putInt("descriptionResId", descriptionResId)
            scheduledDate?.let { args.putString("scheduledDate", it) }
            val dialog = DosingOutOfWindowDialog()
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_dosing_out_of_window, container, false)
        binding.executePendingBindings()

        val showConfirm = arguments?.getBoolean("showConfirmButton") ?: true
        val descriptionResId = arguments?.getInt("descriptionResId") ?: R.string.visit_dosing_warning_out_of_time_window_description
        val scheduledDate = arguments?.getString("scheduledDate")

        binding.btnConfirm.visibility = if (showConfirm) View.VISIBLE else View.GONE
        binding.textViewDescription.setText(descriptionResId)

        if (!scheduledDate.isNullOrEmpty()) {
            binding.textViewScheduledDate.visibility = View.VISIBLE
            binding.textViewScheduledDate.text = getString(R.string.visit_dosing_scheduled_date_label, scheduledDate)
        }

        binding.btnConfirm.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnCancel.setOnClickListener {
            dismissAllowingStateLoss()
            findParent<DosingOutOfWindowDialogListener>()?.onOutOfWindowDosingCanceled()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as? BottomSheetDialog
            val bottomSheet = bottomSheetDialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
    }

    interface DosingOutOfWindowDialogListener {
        fun onOutOfWindowDosingCanceled()
    }
}