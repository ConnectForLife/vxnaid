package com.jnj.vaccinetracker.register.dialogs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogHistoricalVisitDateBinding
import com.jnj.vaccinetracker.register.screens.RegisterParticipantHistoricalDataViewModel
import com.soywiz.klock.DateTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HistoricalVisitDateDialog : BaseDialogFragment() {

   private lateinit var btnOk: Button
   private lateinit var btnCancel: Button
   private lateinit var datePicker: DatePicker
   private var birthDate: String? = null
   private var lastValidDate: Long? = null
   private lateinit var binding: DialogHistoricalVisitDateBinding
   private val disabledDates = mutableSetOf<Long>()

   private val viewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels()


   companion object {
      private const val BIRTH_DATE_STR = "birthDateStr"
      private const val VISIT_TYPE = "visitType"
      private const val DISABLED_DATES_KEY = "disabled_dates"

      fun create(birthDate: String, disabledDates: Set<Long>, visitType: String): HistoricalVisitDateDialog {
         val dialog = HistoricalVisitDateDialog()
         val args = Bundle().apply {
            putString(BIRTH_DATE_STR, birthDate)
            putString(DISABLED_DATES_KEY, disabledDates.joinToString(","))
            putString(VISIT_TYPE, visitType)
         }
         dialog.arguments = args
         return dialog
      }
   }

   @RequiresApi(Build.VERSION_CODES.O)
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      arguments?.getString(BIRTH_DATE_STR)?.let { birthDate = it }

      arguments?.getString(DISABLED_DATES_KEY)?.split(",")?.mapNotNull { it.toLongOrNull() }?.let {
         disabledDates.addAll(viewModel.getAllDisabledDates())
      }

      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
   }

   @RequiresApi(Build.VERSION_CODES.O)
   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_historical_visit_date, container, false)

      initializeViews()
      setupDatePicker()

      btnOk.setOnClickListener {
         val selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
         val normalized = selectedDate.unixMillisLong.toMidnight()
         val visitType = arguments?.getString(VISIT_TYPE) ?: return@setOnClickListener

         if (visitType != "At Birth") {
            // Validate against birth date
            if (!viewModel.isDateValidForVisitType(visitType, selectedDate)) {
               Toast.makeText(requireContext(), "Selected date must be after birth date", Toast.LENGTH_SHORT).show()
               return@setOnClickListener
            }

            // Check for duplicate dates
            if (disabledDates.contains(normalized)) {
               Toast.makeText(requireContext(), "This date is already used. Please select another.", Toast.LENGTH_SHORT).show()
               Log.d("DatePicker", "Blocked attempt to select a disabled date: $normalized")
               return@setOnClickListener
            }

            viewModel.addDisabledDate(normalized)

            val currentDisabledDates = viewModel.getDisabledDatesForVisitType(visitType).toMutableSet()
            currentDisabledDates.add(normalized)
            viewModel.setDisabledDatesForVisitType(visitType, currentDisabledDates)
         }

         // Always notify listener
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

   @RequiresApi(Build.VERSION_CODES.O)
   private fun setupDatePicker() {
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.DAY_OF_YEAR, -1)
      datePicker.maxDate = calendar.timeInMillis

      birthDate?.let {
         try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDateMillis = sdf.parse(it)?.time ?: return
            datePicker.minDate = birthDateMillis
            disabledDates.add(birthDateMillis.toMidnight())
         } catch (e: ParseException) {
            Log.e("DatePicker", "Invalid birth date format", e)
         }
      }

      logDisabledDates()

      datePicker.setOnDateChangedListener { _, year, month, day ->
          val selectedCalendar = Calendar.getInstance()
          selectedCalendar.set(year, month, day, 0, 0, 0)
          selectedCalendar.set(Calendar.MILLISECOND, 0)
          val selectedDateMillis = selectedCalendar.timeInMillis
          val visitType = arguments?.getString(VISIT_TYPE) ?: ""
          val selectedDate = DateTime(year, month + 1, day)

          // Chronological order validation
          val isChronologicalValid = viewModel.isHistoricalVisitDateValid(selectedDate)
          val error = viewModel.errorMessage.value

          if (!isChronologicalValid && !error.isNullOrEmpty()) {
              Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
              Log.d("DatePicker", "Selected date is invalid: $error")
              viewModel.clearErrorMessage()
              lastValidDate?.let {
                  val resetCalendar = Calendar.getInstance()
                  resetCalendar.timeInMillis = it
                  datePicker.updateDate(
                      resetCalendar.get(Calendar.YEAR),
                      resetCalendar.get(Calendar.MONTH),
                      resetCalendar.get(Calendar.DAY_OF_MONTH)
                  )
              }
          } else if (visitType != "At Birth" && disabledDates.contains(selectedDateMillis)) {
              Toast.makeText(requireContext(), "This date is already used. Please select another.", Toast.LENGTH_SHORT).show()
              Log.d("DatePicker", "Selected date is disabled")
              lastValidDate?.let {
                  val resetCalendar = Calendar.getInstance()
                  resetCalendar.timeInMillis = it
                  datePicker.updateDate(
                      resetCalendar.get(Calendar.YEAR),
                      resetCalendar.get(Calendar.MONTH),
                      resetCalendar.get(Calendar.DAY_OF_MONTH)
                  )
              }
          } else {
              Log.d("DatePicker", "Selected date is valid")
              lastValidDate = selectedDateMillis
              viewModel.setHistoricalVisitDate(selectedDate)
          }
      }
   }

   private fun Long.toMidnight(): Long {
      val cal = Calendar.getInstance()
      cal.timeInMillis = this
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      return cal.timeInMillis
   }

   interface HistoricalVisitDateListener {
      fun onDatePicked(date: DateTime)
   }

   private fun logDisabledDates() {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
      disabledDates.forEach { millis ->
         val formattedDate = sdf.format(java.util.Date(millis))
         Log.d("DisabledDate", "Disabled date: $formattedDate")
      }
   }
}
