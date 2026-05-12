package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusClientInfoBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for capturing Child Health+ client basic information
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusClientInfoFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusClientInfoBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_child_health_plus_client_info, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup sex spinner
        val sexOptions = listOf("M", "F")
        val sexAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sexOptions)

        // Date picker for DOB
        binding.dateOfBirth.setOnClickListener {
            showDatePicker { selectedDate ->
                viewModel.dateOfBirth.value = selectedDate
                binding.dateOfBirth.setText(dateFormat.format(selectedDate))
            }
        }

        // Buttons
        binding.btnCancel.setOnClickListener {
            requireActivity().finish()
        }

        binding.btnNext.setOnClickListener {
            viewModel.proceedToServiceSelection()
        }

        return binding.root
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        viewModel.dateOfBirth.value?.let { calendar.time = it }

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

