package com.jnj.vaccinetracker.register.dialogs

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.Spinner
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogHistoricalVisitDateBinding
import com.soywiz.klock.DateTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoricalVisitDateDialog : BaseDialogFragment() {

   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var datePicker: DatePicker
   private lateinit var binding: DialogHistoricalVisitDateBinding

   private var birthDate: String? = null
   private var validator: VisitDateValidator? = null

   companion object {
      private const val BIRTH_DATE_STR = "birthDateStr"

      fun create(birthDate: String): HistoricalVisitDateDialog {
         val dialog = HistoricalVisitDateDialog()
         val args = Bundle().apply {
            putString(BIRTH_DATE_STR, birthDate)
         }
         dialog.arguments = args
         return dialog
      }
   }

   fun setValidator(validator: VisitDateValidator) {
      this.validator = validator
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      birthDate = arguments?.getString(BIRTH_DATE_STR)
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
   }

   private lateinit var visitTypeSpinner: Spinner

   @SuppressLint("SetTextI18n")
   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_historical_visit_date, container, false)
      initializeViews()
      setupDatePicker()

      // Initialize Spinner
      visitTypeSpinner = binding.visitTypeSpinner
      val visitTypes = listOf("At Birth", "6 Weeks", "10 Weeks", "18 Months")
      val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, visitTypes)
      adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
      visitTypeSpinner.adapter = adapter

      btnOk.setOnClickListener {
         val selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
         val selectedVisitType = visitTypeSpinner.selectedItem.toString()

         if (validator?.isVisitAlreadyFilled(selectedVisitType) == true) {
            binding.textViewError.text = "This visit type is already filled."
            binding.textViewError.visibility = View.VISIBLE
         } else if (validator?.isDateAlreadyUsed(selectedDate, selectedVisitType) == true) {
            binding.textViewError.text = "This date is already used for another visit."
            binding.textViewError.visibility = View.VISIBLE
         } else {
            binding.textViewError.visibility = View.GONE
            findParent<HistoricalVisitDateListener>()?.onDatePicked(selectedDate)
            (validator as? VisitDateValidatorImpl)?.addVisitDate(selectedDate, selectedVisitType)
            dismissAllowingStateLoss()
         }
      }

      btnCancel.setOnClickListener {
         dismissAllowingStateLoss()
      }

      return binding.root
   }

   private fun initializeViews() {
      datePicker = binding.datePicker
      btnOk = binding.btnOk
      btnCancel = binding.btnCancel
   }

   private fun setupDatePicker() {
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.DAY_OF_YEAR, -1)
      datePicker.maxDate = calendar.timeInMillis

      birthDate?.let {
         try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDateMillis = sdf.parse(it)?.time ?: return
            datePicker.minDate = birthDateMillis
         } catch (e: ParseException) {
            Log.e("DatePicker", "Invalid birth date format", e)
         }
      }
   }

   interface HistoricalVisitDateListener {
      fun onDatePicked(date: DateTime)
   }

   interface VisitDateValidator {
      fun isDateAlreadyUsed(date: DateTime, visitType: String): Boolean
      fun isVisitAlreadyFilled(visitType: String): Boolean
   }
}