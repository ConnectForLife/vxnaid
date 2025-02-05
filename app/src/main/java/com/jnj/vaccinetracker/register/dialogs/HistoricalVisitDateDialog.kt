package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
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

class HistoricalVisitDateDialog() : BaseDialogFragment() {
   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var datePicker: DatePicker
   private var birthDate: String? = null

   private lateinit var binding: DialogHistoricalVisitDateBinding

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

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      arguments?.getString(BIRTH_DATE_STR)?.let { birthDateString ->
         birthDate = birthDateString
      }
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_historical_visit_date, container, false)
      initializeViews()
      setupDatePicker()

      btnOk.setOnClickListener {
         val selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
         findParent<HistoricalVisitDateListener>()?.onDatePicked(selectedDate)
         dismissAllowingStateLoss()
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
      //  historical visits are restricted to dates strictly before today.
      datePicker.updateDate(
         calendar.get(Calendar.YEAR),
         calendar.get(Calendar.MONTH),
         calendar.get(Calendar.DAY_OF_MONTH)
      )

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
}