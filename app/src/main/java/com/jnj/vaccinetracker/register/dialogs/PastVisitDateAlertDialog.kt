package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R

class PastVisitDateAlertDialog(
    private val title: String,
    private val message: String,
    private val buttonLabel: String,
    private val onButtonClick: (() -> Unit)? = null
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_past_visit_date_error, null)

        view.findViewById<TextView>(R.id.textView_error_title).text = title
        view.findViewById<TextView>(R.id.textView_error_description).text = message
        view.findViewById<Button>(R.id.btn_ok).apply {
            text = buttonLabel
            setOnClickListener {
                onButtonClick?.invoke()
                dismiss()
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    companion object {
        fun newInstance(
            title: String,
            message: String,
            buttonLabel: String,
            onButtonClick: (() -> Unit)? = null
        ): PastVisitDateAlertDialog {
            return PastVisitDateAlertDialog(title, message, buttonLabel, onButtonClick)
        }
    }
}