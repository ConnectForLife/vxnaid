package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusClientInfoBinding
import com.jnj.vaccinetracker.register.dialogs.BestContactTimePickerDialog
import com.jnj.vaccinetracker.register.dialogs.QrCodeGeneratorDialog
import java.text.SimpleDateFormat
import java.util.*

class ChildHealthPlusClientInfoFragment : BaseFragment(),
    BestContactTimePickerDialog.BestContactTimePickerListener {

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

        setupSexRadioButtons()
        setupDatePicker()
        setupPhoneInput()
        setupLanguageDropdown()
        setupBestContactTime()
        setupQrCodeButton()
        setupNavigationButtons()

        return binding.root
    }

    private fun setupSexRadioButtons() {
        binding.radiogroupSex.setOnCheckedChangeListener { _, checkedId ->
            val sex = when (checkedId) {
                binding.rbSexMale.id -> "M"
                binding.rbSexFemale.id -> "F"
                else -> null
            }
            sex?.let { viewModel.sex.value = it }
        }
        viewModel.sex.value?.let {
            when (it) {
                "M" -> binding.rbSexMale.isChecked = true
                "F" -> binding.rbSexFemale.isChecked = true
            }
        }
    }

    private fun setupDatePicker() {
        viewModel.dateOfBirth.value?.let { binding.dateOfBirth.setText(dateFormat.format(it)) }

        binding.dateOfBirth.setOnClickListener {
            showDatePicker { selectedDate ->
                viewModel.dateOfBirth.value = selectedDate
                binding.dateOfBirth.setText(dateFormat.format(selectedDate))
            }
        }
    }

    private fun setupPhoneInput() {
        binding.countryCodePickerPhone.registerCarrierNumberEditText(binding.editTelephone)
        binding.countryCodePickerPhone.setOnCountryChangeListener {
            viewModel.phoneCountryCode.value = binding.countryCodePickerPhone.selectedCountryCode
        }
        viewModel.phoneCountryCode.value = binding.countryCodePickerPhone.selectedCountryCode

        binding.editTelephone.setText(viewModel.telephone.value.orEmpty())
        binding.editTelephone.doAfterTextChanged { text ->
            viewModel.telephone.value = text?.toString()
        }
    }

    private fun setupLanguageDropdown() {
        val languages = listOf(Constants.PERSONAL_LANGUAGE_ENGLISH, Constants.PERSONAL_LANGUAGE_LUGANDA)
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, languages)
        binding.dropdownLanguage.setAdapter(adapter)

        viewModel.language.value?.let { binding.dropdownLanguage.setText(it, false) }

        binding.dropdownLanguage.setOnItemClickListener { _, _, position, _ ->
            viewModel.language.value = languages[position]
        }
    }

    private fun setupBestContactTime() {
        viewModel.bestContactTime.value?.let { binding.tvBestContactTime.setText(it) }

        binding.btnSelectBestContactTime.setOnClickListener {
            val dialog = BestContactTimePickerDialog(viewModel.bestContactTime.value)
            dialog.setListener(this)
            dialog.show(childFragmentManager, "BestContactTimePicker")
        }
    }

    private fun setupQrCodeButton() {
        binding.btnGenerateQrCode.setOnClickListener {
            val childId = viewModel.generateChildId().also {
                viewModel.generatedChildId.value = it
            }
            if (childId.isNotBlank()) {
                QrCodeGeneratorDialog(childId).show(parentFragmentManager, "QrCodeGeneratorDialog")
            }
        }
    }

    private fun setupNavigationButtons() {
        binding.btnCancel.setOnClickListener {
            requireActivity().finish()
        }
        binding.btnNext.setOnClickListener {
            viewModel.telephone.value = binding.editTelephone.text?.toString()
            viewModel.phoneCountryCode.value = binding.countryCodePickerPhone.selectedCountryCode
            viewModel.proceedToServiceSelection()
        }
    }

    override fun onBestContactTimePicked(time: String) {
        viewModel.bestContactTime.value = time
        binding.tvBestContactTime.setText(time)
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
        }, year, month, day).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }
}
