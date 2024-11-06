package com.jnj.vaccinetracker.visitsoverview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemVisitRecordBinding
import com.jnj.vaccinetracker.visitsoverview.model.VisitDataDTO

class VisitsAdapter(
    private val onEyeIconClick: (VisitDataDTO) -> Unit
) : ListAdapter<VisitDataDTO, VisitsAdapter.VisitViewHolder>(VisitDataDTODiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisitViewHolder {
        val binding = ItemVisitRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisitViewHolder, position: Int) {
        val visit = getItem(position)
        holder.bind(visit)
    }

    inner class VisitViewHolder(private val binding: ItemVisitRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(visit: VisitDataDTO) {
            binding.visit = visit
            binding.executePendingBindings()

            val backgroundColor = if (bindingAdapterPosition % 2 == 0) {
                ContextCompat.getColor(binding.root.context, R.color.row_odd_background)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.row_even_background)
            }

            binding.root.setBackgroundColor(backgroundColor)

            binding.iconEye.setOnClickListener {
                onEyeIconClick(visit)
            }
        }
    }

    class VisitDataDTODiffCallback : DiffUtil.ItemCallback<VisitDataDTO>() {
        override fun areItemsTheSame(oldItem: VisitDataDTO, newItem: VisitDataDTO): Boolean {
            return oldItem.participant.participantUuid == newItem.participant.participantUuid
        }

        override fun areContentsTheSame(oldItem: VisitDataDTO, newItem: VisitDataDTO): Boolean {
            return oldItem == newItem
        }
    }
}