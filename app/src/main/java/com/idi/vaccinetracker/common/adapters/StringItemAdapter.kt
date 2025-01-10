package com.idi.vaccinetracker.common.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.idi.vaccinetracker.R

class StringItemAdapter(
   private val items: List<String>,
) : RecyclerView.Adapter<StringItemAdapter.SubstanceViewHolder>() {

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_dropdown, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position])
   }

   override fun getItemCount(): Int = items.size

   inner class SubstanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      private val stringContent: TextView = itemView.findViewById(R.id.textView_item_dropdown)

      fun bind(substanceLabel: String) {
         stringContent.text = substanceLabel
      }
   }
}
