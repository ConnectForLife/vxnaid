package com.idi.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.ui.BaseDialogFragment
import com.idi.vaccinetracker.register.screens.RegisterParticipantHistoricalDataViewModel
import com.soywiz.klock.DateTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
class AlreadyAdministeredVaccineDatePickerDialog(
    private var selectedDate: DateTime? = null,
    private val listener: ScheduleVisitDatePickerDialog.OnDateSelectedListener? = null
) : BaseDialogFragment() {
    private val allDataViewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }

    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button
    private lateinit var datePicker: DatePicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_schedule_visit_date_picker)

        initializeViews(dialog)
        setupDatePicker()

        btnOk.setOnClickListener {
            selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            listener?.onDateSelected(selectedDate!!)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun initializeViews(dialog: Dialog) {
        datePicker = dialog.findViewById(R.id.scheduleVisitDatePicker)
        btnOk = dialog.findViewById(R.id.btn_ok)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
    }

    private fun setupDatePicker() {
        selectedDate?.let {
            datePicker.updateDate(it.yearInt, it.month1 - 1, it.dayOfMonth)
        }

        val c = Calendar.getInstance()
        datePicker.maxDate = c.timeInMillis
        val birthDate = allDataViewModel.getParticipantBirthDate()
        birthDate.let {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val birthDateMillis = sdf.parse(it)?.time ?: return
                datePicker.minDate = birthDateMillis
            } catch (e: ParseException) {
                Log.e("DatePicker", "Invalid birth date format", e)
            }
        }

        if (selectedDate == null) {
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)
            datePicker.init(year, month, day, null)
        }
    }
}