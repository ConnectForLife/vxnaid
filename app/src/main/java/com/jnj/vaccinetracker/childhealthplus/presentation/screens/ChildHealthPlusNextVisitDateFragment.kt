package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusNextVisitDateBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for setting next visit date in Child Health+ workflow
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusNextVisitDateFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusNextVisitDateBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_next_visit_date,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.nextVisitDate.setOnClickListener {
            showDatePicker { selectedDate ->
                viewModel.nextVisitDate.value = selectedDate
                binding.nextVisitDate.setText(dateFormat.format(selectedDate))
            }
        }

        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        binding.btnSkip.setOnClickListener {
            viewModel.setNextVisitDate(null)
        }

        binding.btnNext.setOnClickListener {
            viewModel.setNextVisitDate(viewModel.nextVisitDate.value)
        }

        return binding.root
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        viewModel.nextVisitDate.value?.let { calendar.time = it }

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

