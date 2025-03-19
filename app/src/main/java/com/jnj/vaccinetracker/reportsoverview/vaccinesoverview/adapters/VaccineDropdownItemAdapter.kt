package com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView

class VaccineDropdownItemAdapter(
    context: Context,
    private val vaccineLabels: List<String>,
    private val vaccineConceptNames: List<String>,
    private val selectedVaccineConceptNames: MutableSet<String>,
    var isSelectAllChecked: Boolean = false
) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_multiple_choice, vaccineLabels) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)
        val textView = view.findViewById<CheckedTextView>(android.R.id.text1)

        val selectedVaccineLabel = vaccineLabels[position]
        textView.text = selectedVaccineLabel

        if (position == 0) {
            textView.isChecked = isSelectAllChecked
        } else {
            val selectedVaccineConceptName = vaccineConceptNames[position]
            textView.isChecked = selectedVaccineConceptNames.contains(selectedVaccineConceptName)
        }

        return view
    }

    fun toggleSelectAll() {
        isSelectAllChecked = !isSelectAllChecked
        notifyDataSetChanged()
    }
}