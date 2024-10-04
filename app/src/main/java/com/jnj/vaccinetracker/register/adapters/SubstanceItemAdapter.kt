package com.jnj.vaccinetracker.register.adapters

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.di.ResourcesWrapper
import com.jnj.vaccinetracker.register.dialogs.AlreadyAdministeredVaccineDatePickerDialog
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.register.screens.HistoricalDataForVisitTypeViewModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class SubstanceItemAdapter(
   private val items: MutableList<SubstanceDataModel>,
   private val viewModel: HistoricalDataForVisitTypeViewModel,
   private val supportFragmentManager: FragmentManager,
   private val context: Context
) : RecyclerView.Adapter<SubstanceItemAdapter.SubstanceViewHolder>() {

   @Inject
   lateinit var resourcesWrapper: ResourcesWrapper
   companion object {
      const val DIALOG_TAG = "alreadyAdministeredVaccineDatePickerDialog"
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubstanceViewHolder {
      val itemView = LayoutInflater.from(parent.context)
         .inflate(R.layout.item_registration_administered_vaccine, parent, false)
      return SubstanceViewHolder(itemView)
   }

   override fun onBindViewHolder(holder: SubstanceViewHolder, position: Int) {
      holder.bind(items[position])
   }

   override fun getItemCount(): Int = items.size

   fun updateList(newSubstances: List<SubstanceDataModel>?) {
      val existingItemsMap = items.associateBy { it.conceptName }
      items.clear()
      if (newSubstances != null) {
         newSubstances.forEach { newItem ->
            val existingItem = existingItemsMap[newItem.conceptName]
            if (existingItem != null) {
               newItem.obsDate = existingItem.obsDate
            }
            items.add(newItem)
         }
      } else {
         items.addAll(emptyList())
      }
      notifyDataSetChanged()
   }

   fun reload() {
      notifyDataSetChanged()
   }

   fun checkIfSubstanceHasDate(substance: SubstanceDataModel): Boolean {
      return !viewModel.substancesAndDates.value?.get(substance.conceptName).isNullOrEmpty()
   }

   inner class SubstanceViewHolder(itemView: View) :
      RecyclerView.ViewHolder(itemView),
      ScheduleVisitDatePickerDialog.OnDateSelectedListener
   {
      private val substanceName: TextView = itemView.findViewById(R.id.textView_participantId)
      private val btnPickDate: Button = itemView.findViewById(R.id.btn_pick_date)
      private val vaccineDateText: TextView = itemView.findViewById(R.id.textView_vaccination_date)
      private val removeDateBtn: ImageButton = itemView.findViewById(R.id.remove_date_button)
      private var selectedSubstance: SubstanceDataModel? = null

      fun bind(substance: SubstanceDataModel) {
         selectedSubstance = substance
         substanceName.text = substance.label
         val substanceDateStr = substance.obsDate?.format(DateFormat.FORMAT_DATE) ?: ""
         vaccineDateText.text = substanceDateStr
         btnPickDate.setOnClickListener {
            // nice to have calculate
            AlreadyAdministeredVaccineDatePickerDialog(null, this).show(
               supportFragmentManager,
               DIALOG_TAG
            )
         }
         removeDateBtn.setOnClickListener{
            onDateRemove(substance)
         }
         if (viewModel.visitDate.value != null && !checkIfSubstanceHasDate(substance)) {
            onDateSelected(viewModel.visitDate.value!!)
         }
      }

      override fun onDateSelected(dateTime: DateTime) {
         val dateString = dateTime.format(DateFormat.FORMAT_DATE)
         vaccineDateText.text = dateString
         if (selectedSubstance != null) {
            viewModel.addVaccineDate(selectedSubstance!!.conceptName, dateString)
         }
         removeDateBtn.visibility = View.VISIBLE
      }
      private fun onDateRemove(substance: SubstanceDataModel) {
         viewModel.removeVaccineDate(substance.conceptName)
         vaccineDateText.text = context.getString(R.string.vaccine_never_administered)
         removeDateBtn.visibility = View.INVISIBLE
      }
   }
}