package com.jnj.vaccinetracker.visitsoverview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemPatientRecordBinding
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO

class PatientAdapter : ListAdapter<ParticipantDataDTO, PatientAdapter.PatientViewHolder>(ParticipantDataDTODiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val participant = getItem(position)
        holder.bind(participant)
    }

    inner class PatientViewHolder(private val binding: ItemPatientRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(participant: ParticipantDataDTO) {
            binding.participant = participant
            binding.executePendingBindings()

            val backgroundColor = if (bindingAdapterPosition % 2 == 0) {
                ContextCompat.getColor(binding.root.context, R.color.row_odd_background)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.row_even_background)
            }

            binding.root.setBackgroundColor(backgroundColor)
        }
    }

    class ParticipantDataDTODiffCallback : DiffUtil.ItemCallback<ParticipantDataDTO>() {
        override fun areItemsTheSame(oldItem: ParticipantDataDTO, newItem: ParticipantDataDTO): Boolean {
            return oldItem.participantId == newItem.participantId
        }

        override fun areContentsTheSame(oldItem: ParticipantDataDTO, newItem: ParticipantDataDTO): Boolean {
            return oldItem == newItem
        }
    }
}