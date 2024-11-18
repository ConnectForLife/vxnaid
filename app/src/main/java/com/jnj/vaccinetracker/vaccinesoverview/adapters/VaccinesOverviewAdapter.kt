package com.jnj.vaccinetracker.vaccinesoverview.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.databinding.ItemVaccinesOverviewRecordBinding
import com.jnj.vaccinetracker.vaccinesoverview.dto.VaccinesOverviewDTO

class VaccinesOverviewAdapter : ListAdapter<VaccinesOverviewDTO, VaccinesOverviewAdapter.VaccinesOverviewHolder>(VaccinesOverviewDataDTODiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccinesOverviewHolder {
        val binding = ItemVaccinesOverviewRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VaccinesOverviewHolder(binding)
    }

    override fun onBindViewHolder(holder: VaccinesOverviewHolder, position: Int) {
        val vaccinesOverview = getItem(position)
        holder.bind(vaccinesOverview)
    }

    inner class VaccinesOverviewHolder(private val binding: ItemVaccinesOverviewRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vaccinesOverview: VaccinesOverviewDTO) {
            binding.vaccineOverview = vaccinesOverview
            binding.executePendingBindings()

            val backgroundColor = if (bindingAdapterPosition % 2 == 0) {
                ContextCompat.getColor(binding.root.context, R.color.row_odd_background)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.row_even_background)
            }

            binding.root.setBackgroundColor(backgroundColor)
        }
    }

    class VaccinesOverviewDataDTODiffCallback : DiffUtil.ItemCallback<VaccinesOverviewDTO>() {
        override fun areItemsTheSame(oldItem: VaccinesOverviewDTO, newItem: VaccinesOverviewDTO): Boolean {
            return oldItem.label == newItem.label
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: VaccinesOverviewDTO, newItem: VaccinesOverviewDTO): Boolean {
            return oldItem == newItem
        }
    }
}