package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusAdministrationDateBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for selecting administration date in Child Health+ workflow
 */
class ChildHealthPlusAdministrationDateFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusAdministrationDateBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_administration_date,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.administrationDate.value?.let {
            binding.administrationDate.setText(dateFormat.format(it))
        }

        binding.administrationDate.setOnClickListener {
            showDatePicker { selectedDate ->
                viewModel.administrationDate.value = selectedDate
                binding.administrationDate.setText(dateFormat.format(selectedDate))
            }
        }

        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        binding.btnNext.setOnClickListener {
            viewModel.administrationDate.value?.let {
                viewModel.setAdministrationDate(it)
            }
        }

        return binding.root
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        viewModel.administrationDate.value?.let { calendar.time = it }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(selectedCalendar.time)
        }, year, month, day).show()
    }
}
