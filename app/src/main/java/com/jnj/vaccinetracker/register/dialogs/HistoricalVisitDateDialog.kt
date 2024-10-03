package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
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
import java.util.Calendar

class HistoricalVisitDateDialog() : BaseDialogFragment() {
   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var datePicker: DatePicker

   private lateinit var binding: DialogHistoricalVisitDateBinding

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
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
      val c = Calendar.getInstance()
      datePicker.maxDate = c.timeInMillis
   }

   interface HistoricalVisitDateListener {
      fun onDatePicked(date: DateTime)
   }
}