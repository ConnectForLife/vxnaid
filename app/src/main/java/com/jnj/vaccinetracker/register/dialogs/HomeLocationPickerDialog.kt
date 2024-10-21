package com.jnj.vaccinetracker.register.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.domain.entities.Address
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.helpers.logInfo
import com.jnj.vaccinetracker.common.helpers.logVerbose
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogHomeLocationPickerBinding
import com.jnj.vaccinetracker.databinding.ItemHomeLocationAddressFieldBinding
import com.jnj.vaccinetracker.register.dialogs.HomeLocationPickerViewModel.*

/**
 * @author maartenvangiel
 * @version 1
 * A dialog that allows the user to pick an address. Fields dynamically appear/disappear depending on the chosen dropdown items.
 */
class HomeLocationPickerDialog(
    private val selectedHomeLocation: Address?
) : BaseDialogFragment() {

    private val viewModel: HomeLocationPickerViewModel by viewModels { viewModelFactory }
    private lateinit var binding: DialogHomeLocationPickerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_home_location_picker, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.buttonCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.buttonConfirm.setOnClickListener {
            viewModel.confirmAddress(confirmationListener = { address ->
                findParent<HomeLocationPickerListener>()?.onHomeLocationPicked(address)
                dismiss()
            })
        }
        viewModel.selectedAddress = selectedHomeLocation
        return binding.root
    }


    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.countries.observe(lifecycleOwner) { countries ->
            binding.spinnerCountry.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, countries?.map { it.display }.orEmpty())
            binding.spinnerCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val country = viewModel.countries.value?.getOrNull(position)?.value ?: return
                    viewModel.setCountry(country)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No-op
                }
            }
        }
        viewModel.addressInputFields.observe(lifecycleOwner) { inputFields ->
            logVerbose("Dynamic input fields: {}", inputFields)
            inflateAddressFields(inputFields.orEmpty())
            viewModel.setUserInputInAddressInputFieldsFromAddress()
        }
        viewModel.country.observe(lifecycleOwner) { country ->
            country?.let { c ->
                val index = viewModel.countries.value.orEmpty().indexOfFirst { it.value == c }
                binding.spinnerCountry.setSelection(index)
            }
        }
    }

    private fun inflateAddressFields(inputFields: List<AddressInputField>) {
        binding.addressFieldsContainer.removeAllViews()

        inputFields.forEach { inputField ->
            val view = DataBindingUtil.inflate<ItemHomeLocationAddressFieldBinding>(layoutInflater, R.layout.item_home_location_address_field, binding.addressFieldsContainer, true)
            view.addressField = inputField
            view.executePendingBindings()

            when (inputField.type) {
                AddressInputFieldType.DROPDOWN -> {
                    bindDropdownItemView(inputField, view)
                }
                AddressInputFieldType.NOT_IN_LIST -> {
                    bindDropdownItemView(inputField, view)
                    bindFreeInputItemView(inputField, view)
                }
                AddressInputFieldType.FREE_INPUT -> {
                    bindFreeInputItemView(inputField, view)
                }
            }
        }
    }

    private fun bindFreeInputItemView(inputField: AddressInputField, view: ItemHomeLocationAddressFieldBinding) {
        view.editText.doAfterTextChanged {
            viewModel.onAddressFieldSelected(inputField, view.editText.text.toString(), isDropdownValue = false)
        }
        inputField.userInput?.value?.let { userInputValue ->
            if (userInputValue != HomeLocationPickerViewModel.ITEM_NOT_IN_LIST) view.editText.setText(userInputValue)
        }
    }

    private fun bindDropdownItemView(inputField: AddressInputField, view: ItemHomeLocationAddressFieldBinding) {
        val dropdownValues = inputField.dropdownValues.orEmpty()
        val autoCompleteTextView = view.autoCompleteTextView
        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, dropdownValues.map { it.display })
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.threshold = 0

        autoCompleteTextView.setOnClickListener {
            autoCompleteTextView.showDropDown()
        }

        autoCompleteTextView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                autoCompleteTextView.showDropDown()
            }
        }

        autoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedOption = parent.getItemAtPosition(position) as String
            val selectedOptionValue = dropdownValues.firstOrNull { it.display == selectedOption }?.value
            if (selectedOptionValue.isNullOrEmpty()) return@setOnItemClickListener
            viewModel.onAddressFieldSelected(inputField, selectedOptionValue, isDropdownValue = true)
        }

        view.autoCompleteTextView.post {
            inputField.userInput?.let { input ->
                val inputToMatch = when (input) {
                    is UserInput.Dropdown.Item, is UserInput.FreeInput -> input.value
                    is UserInput.Dropdown.NotInThisList -> HomeLocationPickerViewModel.ITEM_NOT_IN_LIST
                }
                val index = dropdownValues.map { it.value }.indexOfFirst { dropdownValue -> dropdownValue == inputToMatch }
                if (index in dropdownValues.indices) {
                    autoCompleteTextView.setText(dropdownValues[index].display, false)
                }
            }
        }

        val errorView = view.autoCompleteTextView
        if (!inputField.errorMessage.isNullOrEmpty()) {
            errorView.error = inputField.errorMessage
            errorView.requestFocus()
        } else {
            errorView.error = null
        }
    }

    interface HomeLocationPickerListener {
        fun onHomeLocationPicked(address: AddressUiModel)
    }
}