package com.jnj.vaccinetracker.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants

class OutreachNameDialog : DialogFragment() {
    private lateinit var loginViewModel: LoginViewModel

    companion object {
        fun newInstance(): OutreachNameDialog {
            return OutreachNameDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_outreach_name, container, false)

        // Initialize LoginViewModel scoped to the parent activity
        loginViewModel = ViewModelProvider(requireActivity())[LoginViewModel::class.java]

        val editTextOutreachName = view.findViewById<EditText>(R.id.editText_outreach_name)
        val buttonSubmit = view.findViewById<Button>(R.id.button_submit)

        buttonSubmit.setOnClickListener {
            val userInput = editTextOutreachName.text.toString().trim()
            if (userInput.isNotEmpty()) {
                val outreachName = "Outreach $userInput"
                Log.d("OutreachNameDialog", "User entered outreach name: $outreachName")
                // Save outreach name in SharedPreferences
                val sharedPreferences = requireContext().getSharedPreferences(
                    Constants.USER_PREFERENCES_FILE_NAME, Context.MODE_PRIVATE
                )
                sharedPreferences.edit().putString(Constants.OUTREACH_NAME_KEY, outreachName).apply()

                // Set outreach name in LoginViewModel
                loginViewModel.setOutreachName(outreachName)

                dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.error_empty_outreach_name), Toast.LENGTH_SHORT).show()
            }
        }
        return view
    }
}