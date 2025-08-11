package com.jnj.vaccinetracker.register.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R

class PastVisitDateInfoDialog(
    private val onContinue: (() -> Unit)? = null
) : DialogFragment() {

    @SuppressLint("UseGetLayoutInflater")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = requireArguments().getString(ARG_TITLE)
        val message = requireArguments().getString(ARG_MESSAGE)
        val buttonLabel = requireArguments().getString(ARG_BUTTON_LABEL)

        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_past_visit_date_info, null)

        view.findViewById<TextView>(R.id.textView_title).text = title
        view.findViewById<TextView>(R.id.textView_description).text = message
        view.findViewById<Button>(R.id.btn_continue).apply {
            text = buttonLabel
            setOnClickListener {
                onContinue?.invoke()
                dismiss()
            }
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .create()
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_BUTTON_LABEL = "button_label"

        fun newInstance(title: String, message: String, buttonLabel: String, onContinue: (() -> Unit)?): PastVisitDateInfoDialog {
            val fragment = PastVisitDateInfoDialog(onContinue)
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_MESSAGE, message)
            args.putString(ARG_BUTTON_LABEL, buttonLabel)
            fragment.arguments = args
            return fragment
        }
    }
}