package com.jnj.vaccinetracker.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Site

class OutreachNameDialogFragment(private val viewModel: LoginViewModel) : DialogFragment() {
    companion object {
        fun newInstance(viewModel: LoginViewModel): OutreachNameDialogFragment {
            return OutreachNameDialogFragment(viewModel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_outreach_name, container, false)

        val editTextOutreachName = view.findViewById<EditText>(R.id.editText_outreach_name)
        val buttonSubmit = view.findViewById<Button>(R.id.button_submit)

        buttonSubmit.setOnClickListener {
            val outreachName = editTextOutreachName.text.toString()
            if (outreachName.isNotEmpty()) {
                viewModel.outreachName.value = outreachName // Update outreachName in ViewModel
                dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_empty_outreach_name), Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }




}