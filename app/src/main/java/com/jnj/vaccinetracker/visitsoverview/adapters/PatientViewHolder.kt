package com.jnj.vaccinetracker.visitsoverview.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO

class PatientViewHolder(
    itemView: View,
    private val onPatientClick: (ParticipantDataDTO) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val patientName: TextView = itemView.findViewById(R.id.patient_name)
    private val patientId: TextView = itemView.findViewById(R.id.patient_id)
    private val motherName: TextView = itemView.findViewById(R.id.mother_name)

    fun bind(patient: ParticipantDataDTO) {
        patientName.text = patient.fullName
        patientId.text = patient.participantId
        motherName.text = patient.motherName

        itemView.setOnClickListener {
            onPatientClick(patient)
        }
    }
}