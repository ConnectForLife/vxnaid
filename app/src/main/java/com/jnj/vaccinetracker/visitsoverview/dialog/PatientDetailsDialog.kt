package com.jnj.vaccinetracker.visitsoverview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.DialogPatientDetailsBinding
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO

class PatientDetailsDialog : DialogFragment() {

    private lateinit var binding: DialogPatientDetailsBinding
    private lateinit var patientDetails: ParticipantDataDTO
    private lateinit var participantKey: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_patient_details, container, false)

        arguments?.let {
            patientDetails = it.getParcelable(ARG_PATIENT_DETAILS) ?: throw IllegalArgumentException("Patient details are missing")
            participantKey = it.getString(ARG_PARTICIPANT_KEY) ?: throw IllegalArgumentException("Participant key is missing")
        } ?: throw IllegalArgumentException("Arguments are missing")

        binding.patientDetails = patientDetails
        binding.closeButton.setOnClickListener { dismissAllowingStateLoss() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tableRowViews = listOf(
            binding.tableRowChildId,
            binding.tableRowChildName,
            binding.tableRowMotherFullName
        )

        tableRowViews.forEachIndexed { index, tableRow ->
            val backgroundColor = if (index % 2 == 0) {
                ContextCompat.getColor(requireContext(), R.color.row_odd_background)
            } else {
                ContextCompat.getColor(requireContext(), R.color.row_even_background)
            }
            tableRow.setBackgroundColor(backgroundColor)
        }
    }

    companion object {
        private const val ARG_PATIENT_DETAILS = "patient_details"
        private const val ARG_PARTICIPANT_KEY = "participant_key"

        fun newInstance(patient: ParticipantDataDTO, participantKey: String): PatientDetailsDialog {
            return PatientDetailsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PATIENT_DETAILS, patient)
                    putString(ARG_PARTICIPANT_KEY, participantKey)
                }
            }
        }
    }
}