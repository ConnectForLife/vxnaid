package com.jnj.vaccinetracker.register.dialogs

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.jnj.vaccinetracker.R
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateTimeSpan

class EstimatedAgeDialog(
    private var estimatedBirthDate: DateTime? = null,
    private var yearsEstimated: Int? = null,
    private var monthsEstimated: Int? = null,
    private var weeksEstimated: Int? = null
) : DialogFragment() {
    private lateinit var numberPickerYears: NumberPicker
    private lateinit var numberPickerMonths: NumberPicker
    private lateinit var numberPickerWeeks: NumberPicker
    private lateinit var btnOk: Button
    private lateinit var btnCancel: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_estimated_age_picker)

        initializeViews(dialog)
        setupNumberPickers()

        btnOk.setOnClickListener {
            estimatedBirthDate = calculateEstimatedDate()

            val years = numberPickerYears.value
            val months = numberPickerMonths.value
            val weeks = numberPickerWeeks.value

            (parentFragment as? EstimatedAgePickerListener)?.onEstimatedAgePicked(
                estimatedBirthDate!!,
                years,
                months,
                weeks
            )
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        return dialog
    }

    private fun initializeViews(dialog: Dialog) {
        numberPickerYears = dialog.findViewById(R.id.numberPicker_years)
        numberPickerMonths = dialog.findViewById(R.id.numberPicker_months)
        numberPickerWeeks = dialog.findViewById(R.id.numberPicker_weeks)
        btnOk = dialog.findViewById(R.id.btn_ok)
        btnCancel = dialog.findViewById(R.id.btn_cancel)
    }

    private fun setupNumberPickers() {
        val yearLabels = Array(101) { index -> "$index Years" }
        val monthLabels = Array(12) { index -> "$index Months" }
        val weeksLabels = Array(6) { index -> "$index Weeks" }

        numberPickerYears.minValue = 0
        numberPickerYears.maxValue = yearLabels.size - 1
        numberPickerYears.displayedValues = yearLabels
        numberPickerYears.value = yearsEstimated ?: 0

        numberPickerMonths.minValue = 0
        numberPickerMonths.maxValue = monthLabels.size - 1
        numberPickerMonths.displayedValues = monthLabels
        numberPickerMonths.value = monthsEstimated ?: 0

        numberPickerWeeks.minValue = 0
        numberPickerWeeks.maxValue = weeksLabels.size - 1
        numberPickerWeeks.displayedValues = weeksLabels
        numberPickerWeeks.value = weeksEstimated ?: 0

        numberPickerYears.wrapSelectorWheel = false
        numberPickerMonths.wrapSelectorWheel = false
        numberPickerWeeks.wrapSelectorWheel = false
    }

    private fun calculateEstimatedDate(): DateTime {
        val years = numberPickerYears.value
        val months = numberPickerMonths.value
        val weeks = numberPickerWeeks.value

        val dateTimeSpan = DateTimeSpan(years = years, months = months, weeks = weeks)

        return DateTime.now().minus(dateTimeSpan)
    }

    interface EstimatedAgePickerListener {
        fun onEstimatedAgePicked(
            estimatedBirthDate: DateTime?,
            yearsEstimated: Int?,
            monthsEstimated: Int?,
            weeksEstimated: Int?
        )
    }
}