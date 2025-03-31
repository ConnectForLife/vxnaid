package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.soywiz.klock.DateTime

class BirthDatePickerDialog(
    private var selectedDate: DateTime? = null,
) : DialogFragment() {
    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button
    private lateinit var datePicker: DatePicker

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_date_picker)

        initializeViews(dialog)
        setupDatePicker()

        btnOk.setOnClickListener {
            selectedDate = DateTime.createAdjusted(
                year = datePicker.year,
                month = datePicker.month + 1,
                day = datePicker.dayOfMonth
            )

            Log.d("BirthDatePicker", "Selected Date: $selectedDate")

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

        val today = DateTime.now()
        datePicker.maxDate = today.unixMillisLong

        if (selectedDate == null) {
            datePicker.init(today.yearInt, today.month1 - 1, today.dayOfMonth, null)
        }
    }

    interface BirthDatePickerListener {
        fun onBirthDatePicked(birthDate: DateTime?, isEstimated: Boolean)
    }
}
