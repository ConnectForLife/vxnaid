package com.jnj.vaccinetracker.reportsoverview.hmis105.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.databinding.ItemHmis105ReportRowBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ReportDTO

class Hmis105Adapter : ListAdapter<Hmis105ReportDTO, Hmis105Adapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemHmis105ReportRowBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemHmis105ReportRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(report: Hmis105ReportDTO) {
            binding.textViewDoses.text = report.doses
            binding.textViewUnder1Static.text = report.under1Static.toString()
            binding.textViewUnder1Outreach.text = report.under1Outreach.toString()
            binding.textView1to4Static.text = report.age1to4Static.toString()
            binding.textView1to4Outreach.text = report.age1to4Outreach.toString()
            binding.textView5to14Static.text = report.age5to14Static.toString()
            binding.textView5to14Outreach.text = report.age5to14Outreach.toString()
            binding.textViewTotal.text = report.total.toString()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Hmis105ReportDTO>() {
        override fun areItemsTheSame(
            oldItem: Hmis105ReportDTO,
            newItem: Hmis105ReportDTO
        ): Boolean = oldItem.doses == newItem.doses

        override fun areContentsTheSame(
            oldItem: Hmis105ReportDTO,
            newItem: Hmis105ReportDTO
        ): Boolean = oldItem == newItem
    }
}

