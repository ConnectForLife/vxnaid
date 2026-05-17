package com.jnj.vaccinetracker.reportsoverview.hmis105.adapters

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.databinding.ItemHmis105ChildHealthRowBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ChildHealthReportDTO

class Hmis105ChildHealthAdapter :
    ListAdapter<Hmis105ChildHealthReportDTO, Hmis105ChildHealthAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHmis105ChildHealthRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHmis105ChildHealthRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(row: Hmis105ChildHealthReportDTO) {
            binding.tvDose.text     = row.dose
            binding.tvLocation.text = row.visitLocation
            binding.tv611Male.text   = row.months6to11Male.toString()
            binding.tv611Female.text = row.months6to11Female.toString()
            binding.tv1259Male.text  = row.months12to59Male.toString()
            binding.tv1259Female.text= row.months12to59Female.toString()
            binding.tv514Male.text   = row.years5to14Male.toString()
            binding.tv514Female.text = row.years5to14Female.toString()
            binding.tvTotal.text     = row.total.toString()

            if (row.isTotalRow) {
                binding.root.setBackgroundColor(Color.parseColor("#E8F0F7"))
                listOf(
                    binding.tvDose, binding.tvLocation,
                    binding.tv611Male, binding.tv611Female,
                    binding.tv1259Male, binding.tv1259Female,
                    binding.tv514Male, binding.tv514Female,
                    binding.tvTotal
                ).forEach { it.setTypeface(null, Typeface.BOLD) }
            } else {
                val isEven = bindingAdapterPosition % 2 == 0
                binding.root.setBackgroundColor(
                    if (isEven) Color.WHITE else Color.parseColor("#F5F5F5")
                )
                listOf(
                    binding.tvLocation,
                    binding.tv611Male, binding.tv611Female,
                    binding.tv1259Male, binding.tv1259Female,
                    binding.tv514Male, binding.tv514Female
                ).forEach { it.setTypeface(null, Typeface.NORMAL) }
                binding.tvDose.setTypeface(null, Typeface.BOLD)
                binding.tvTotal.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Hmis105ChildHealthReportDTO>() {
        override fun areItemsTheSame(a: Hmis105ChildHealthReportDTO, b: Hmis105ChildHealthReportDTO) =
            a.dose == b.dose && a.visitLocation == b.visitLocation

        override fun areContentsTheSame(a: Hmis105ChildHealthReportDTO, b: Hmis105ChildHealthReportDTO) =
            a == b
    }
}
