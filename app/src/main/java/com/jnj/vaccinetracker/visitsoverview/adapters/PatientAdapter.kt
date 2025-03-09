package com.jnj.vaccinetracker.visitsoverview.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO

class PatientAdapter : ListAdapter<ParticipantDataDTO, PatientAdapter.PatientViewHolder>(PatientDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient, parent, false)
        return PatientViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PatientViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val patientName: TextView = itemView.findViewById(R.id.patient_name)
        private val patientId: TextView = itemView.findViewById(R.id.patient_id)
        //private val motherName: TextView = itemView.findViewById(R.id.mother_name)

        fun bind(patient: ParticipantDataDTO) {
            patientName.text = patient.fullName
            patientId.text = patient.participantId
          //  motherName.text = patient.motherName
        }
    }

    companion object {
        private val PatientDiffCallback = object : DiffUtil.ItemCallback<ParticipantDataDTO>() {
            override fun areItemsTheSame(oldItem: ParticipantDataDTO, newItem: ParticipantDataDTO): Boolean {
                return oldItem.participantId == newItem.participantId
            }

            override fun areContentsTheSame(oldItem: ParticipantDataDTO, newItem: ParticipantDataDTO): Boolean {
                return oldItem == newItem
            }
        }
    }
}