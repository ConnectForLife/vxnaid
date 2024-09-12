package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.soywiz.klock.DateTime
import java.util.Calendar

class BirthDatePickerDialog(
    private var selectedDate: DateTime? = null,
) : DialogFragment() {
    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button
    private lateinit var datePicker: DatePicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_birth_date_picker)

        initializeViews(dialog)
        setupDatePicker()

        btnOk.setOnClickListener {
            selectedDate = DateTime(datePicker.year, datePicker.month + 1, datePicker.dayOfMonth)
            (parentFragment as? BirthDatePickerListener)?.onBirthDatePicked(selectedDate!!, false)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun initializeViews(dialog: Dialog) {
        datePicker = dialog.findViewById(R.id.datePicker)
        btnOk = dialog.findViewById(R.id.btn_ok)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
    }

    private fun setupDatePicker() {
        selectedDate?.let {
            datePicker.updateDate(it.yearInt, it.month1 - 1, it.dayOfMonth)
        }

        val c = Calendar.getInstance()
        datePicker.maxDate = c.timeInMillis

        if (selectedDate == null) {
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH) + 1
            val day = c.get(Calendar.DAY_OF_MONTH)
            datePicker.init(year, month, day, null)
        }
    }

    interface BirthDatePickerListener {
        fun onBirthDatePicked(birthDate: DateTime?, isEstimated: Boolean)
    }
}
