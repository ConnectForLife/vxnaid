package com.jnj.vaccinetracker.register.screens

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeActivity
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.helpers.*
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterParticipantParticipantDetailsBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.*
import com.soywiz.klock.DateTime
import kotlinx.coroutines.flow.onEach

/**
 * @author maartenvangiel
 * @version 1
 */
@SuppressWarnings("TooManyFunctions")
class RegisterParticipantParticipantDetailsFragment : BaseFragment(),
    HomeLocationPickerDialog.HomeLocationPickerListener,
    BirthDatePickerDialog.BirthDatePickerListener,
    RegisterParticipantConfirmNoTelephoneDialog.RegisterParticipationNoTelephoneConfirmationListener,
    RegisterParticipantHasChildEverVaccinatedDialog.RegisterParticipationIsChildNewbornListener,
    RegisterParticipantSuccessfulDialog.RegisterParticipationCompletionListener,
    EstimatedAgeDialog.EstimatedAgePickerListener {

    private companion object {
        private const val TAG_HOME_LOCATION_PICKER = "homeLocationPicker"
        private const val TAG_DATE_PICKER = "datePicker";
        private const val TAG_SUCCESS_DIALOG = "successDialog"
        private const val TAG_UPDATE_SUCCESS_DIALOG = "successUpdateDialog"
        private const val TAG_NO_PHONE_DIALOG = "confirmNoPhoneDialog"
        private const val TAG_NO_MATCHING_ID = "noMatchingIdDialog"
        private const val TAG_CHILD_NEWBORN_ID = "childNewBornDialog"
        private const val REQ_BARCODE = 213
        private const val TAG_ESTIMATED_AGE_PICKER = "estimatedAgePicker"
    }

    private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
    private val viewModel: RegisterParticipantParticipantDetailsViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentRegisterParticipantParticipantDetailsBinding

    private var birthDatePicked: DateTime? = null
    private var isBirthDateEstimated: Boolean = false
    private var yearsEstimated: Int? = null
    private var monthsEstimated: Int? = null
    private var weeksEstimated: Int? = null
    private var estimatedBirthDatePicked: DateTime? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_register_participant_participant_details,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.flowViewModel = flowViewModel
        binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }
        binding.textViewParticipantHomeLocation.movementMethod = ScrollingMovementMethod()

        setHasOptionsMenu(true)

        setupPhoneInput()
        setupDropdowns()
        setupClickListeners()
        setupInputListeners()

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (activity as AppCompatActivity).supportActionBar?.setHomeButtonEnabled(true)

        return binding.root
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.defaultPhoneCountryCode.observe(lifecycleOwner) { countryCode ->
            if (countryCode != null) {
                binding.countryCodePickerPhone.setDefaultCountryUsingNameCode(countryCode)
                if (flowViewModel.countryCode.get().isNullOrEmpty())
                    binding.countryCodePickerPhone.resetToDefaultCountry()
            }
        }
        flowViewModel.countryCode.observe(lifecycleOwner) { countryCode ->
            if (countryCode != null) {
                binding.countryCodePickerPhone.setCountryForPhoneCode(countryCode.toInt())
            }
        }

        viewModel.birthWeightValidationMessage.observe(lifecycleOwner) { birthWeightValidationMessage ->
            logDebug("validate birth weight" + birthWeightValidationMessage)
           // binding.birthWeightError.requestFocus()
        }

        viewModel.childCategoryNames.observe(lifecycleOwner) { childCategoryNames ->
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.item_dropdown,
                childCategoryNames.orEmpty().map { it.display })
            binding.dropdownChildCategory.setAdapter(adapter)
        }
        viewModel.genderValidationMessage.observe(lifecycleOwner) { genderValidationMessage ->
            logDebug("validate gender" + genderValidationMessage)

            binding.genderError.requestFocus()
            binding.genderError.error = genderValidationMessage
        }
        viewModel.participantUuid.observe(lifecycleOwner) {
            setupEditableFields()
        }
        observeViewModelEvents(lifecycleOwner)
    }

    private fun observeViewModelEvents(lifecycleOwner: LifecycleOwner) = viewModel.apply {
        registerNoPhoneEvents
            .asFlow()
            .onEach {
                RegisterParticipantConfirmNoTelephoneDialog()
                    .show(childFragmentManager, TAG_NO_PHONE_DIALOG)
            }.launchIn(lifecycleOwner)
        registerNoMatchingIdEvents
            .asFlow()
            .onEach {
                RegisterParticipantIdNotMatchingDialog()
                    .show(childFragmentManager, TAG_NO_MATCHING_ID)
            }.launchIn(lifecycleOwner)
        registerFailedEvents
            .asFlow()
            .onEach { errorMessage ->
                Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG).show()
            }.launchIn(lifecycleOwner)
        registerChildNewbornEvents
            .asFlow()
            .onEach {
                flowViewModel.registerDetails.value = it
                RegisterParticipantHasChildEverVaccinatedDialog()
                    .show(childFragmentManager, TAG_CHILD_NEWBORN_ID)
            }.launchIn(lifecycleOwner)
        registerParticipantSuccessDialogEvents
            .asFlow()
            .onEach { participant ->
                RegisterParticipantSuccessfulDialog.create(participant)
                    .show(childFragmentManager, TAG_SUCCESS_DIALOG)
            }.launchIn(lifecycleOwner)
        updateParticipantSuccessDialogEvents
            .asFlow()
            .onEach { participant ->
                setupEditableFields()
                UpdateParticipantSuccessfulDialog().show(childFragmentManager, TAG_UPDATE_SUCCESS_DIALOG)
            }.launchIn(lifecycleOwner)
    }

    private fun setupEditableFields() {
        if (viewModel.participantUuid.value != null) {
            binding.rbGenderMale.isEnabled = false
            binding.rbGenderFemale.isEnabled = false
            binding.btnScanParticipantId.visibility = View.INVISIBLE
            if (viewModel.birthWeight.value != null) {
                binding.editBirthWeight.isEnabled = false
            }
        }
    }

    private fun setupInputListeners() {
        binding.editTextChildNumber.doAfterTextChanged {
            val childNumber = it?.toString().orEmpty()
            viewModel.setChildNumber(childNumber)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(childNumber = childNumber)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editParticipantNin.doAfterTextChanged {
            val nin = it?.toString().orEmpty()
            viewModel.setNin(nin)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(nin = nin)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editTelephone.doAfterTextChanged {
            val phoneNumber = it?.toString().orEmpty()
            viewModel.setPhone(phoneNumber)
            flowViewModel.phoneNumber.set(phoneNumber)
        }
        
        binding.editBirthWeight.doAfterTextChanged {
            val birthWeight = it?.toString().orEmpty()
            viewModel.setBirthWeight(birthWeight)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(birthWeight = birthWeight)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editMotherFirstName.doAfterTextChanged {
            val motherFirstName = it?.toString().orEmpty()
            viewModel.setMotherFirstName(motherFirstName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(motherFirstName = motherFirstName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editMotherLastName.doAfterTextChanged {
            val motherLastName = it?.toString().orEmpty()
            viewModel.setMotherLastName(motherLastName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(motherLastName = motherLastName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editFatherFirstName.doAfterTextChanged {
            val fatherFirstName = it?.toString().orEmpty()
            viewModel.setFatherFirstName(fatherFirstName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(fatherFirstName = fatherFirstName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editFatherLastName.doAfterTextChanged {
            val fatherLastName = it?.toString().orEmpty()
            viewModel.setFatherLastName(fatherLastName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(fatherLastName = fatherLastName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editChildFirstName.doAfterTextChanged {
            val childFirstName = it?.toString().orEmpty()
            viewModel.setChildFirstName(childFirstName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(childFirstName = childFirstName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }

        binding.editChildLastName.doAfterTextChanged {
            val childLastName = it?.toString().orEmpty()
            viewModel.setChildLastName(childLastName)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(childLastName = childLastName)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }
    }

    private fun setupPhoneInput() {
        binding.countryCodePickerPhone.registerCarrierNumberEditText(binding.editTelephone)
        binding.countryCodePickerPhone.setOnCountryChangeListener {
            viewModel.setPhoneCountryCode(binding.countryCodePickerPhone.selectedCountryCode)
        }
        val countryCode = binding.countryCodePickerPhone.selectedCountryCode
        viewModel.setPhoneCountryCode(countryCode)
    }

    private fun setupClickListeners() {
        binding.btnSetHomeLocation.setOnClickListener {
            HomeLocationPickerDialog(
                viewModel.homeLocation.value
            ).show(childFragmentManager, TAG_HOME_LOCATION_PICKER)
        }

        binding.btnPickDate.setOnClickListener {
            BirthDatePickerDialog(birthDatePicked).show(childFragmentManager, TAG_DATE_PICKER);
        }

        binding.btnEstimatedDate.setOnClickListener {
            EstimatedAgeDialog(
                birthDatePicked, yearsEstimated, monthsEstimated, weeksEstimated
            ).show(childFragmentManager, TAG_ESTIMATED_AGE_PICKER)
        }

        binding.btnSubmit.setOnClickListener {
            submitRegistration()
        }
        binding.rbGenderMale.setOnClickListener {
            updateGender()
        }
        binding.rbGenderFemale.setOnClickListener {
            updateGender()
        }
        binding.genderError.setOnClickListener {
            it.requestFocus()
        }
        binding.btnScanParticipantId.setOnClickListener {
            startActivityForResult(
                ScanBarcodeActivity.create(
                    requireContext(),
                    ScanBarcodeActivity.PARTICIPANT
                ), REQ_BARCODE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_BARCODE && resultCode == Activity.RESULT_OK) {
            val participantIdBarcode =
                data?.getStringExtra(ScanBarcodeActivity.EXTRA_BARCODE) ?: return
            viewModel.onParticipantIdScanned(participantIdBarcode)
            val currentDetails = flowViewModel.registerDetails.value
            if (currentDetails != null) {
                val updatedDetails = currentDetails.copy(participantId = participantIdBarcode)
                flowViewModel.registerDetails.set(updatedDetails)
            }
        }
    }

 private fun setupDropdowns() {
    viewModel.childCategoryNames.observe(viewLifecycleOwner) { categoryList ->
        if (categoryList != null) {
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryList.map { it.display })
            binding.dropdownChildCategory.setAdapter(adapter)

            val selectedCategoryValue = flowViewModel.registerDetails.value?.childCategory
            if (selectedCategoryValue != null) {
                val index = categoryList.indexOfFirst { it.value == selectedCategoryValue } 
                if (index >= 0) {
                    binding.dropdownChildCategory.setText(categoryList[index].display, false)
                }
            }
        }
    }

    binding.dropdownChildCategory.setOnItemClickListener { _, _, position, _ ->
        val selectedCategory = viewModel.childCategoryNames.value?.get(position) ?: return@setOnItemClickListener
        viewModel.setSelectedChildCategory(selectedCategory)

        val currentDetails = flowViewModel.registerDetails.value
        if (currentDetails != null) {
            val updatedDetails = currentDetails.copy(childCategory = selectedCategory.value) 
            flowViewModel.registerDetails.set(updatedDetails)
        }
    }
}



    override fun onStart() {
        super.onStart()
        viewModel.setArguments(
            RegisterParticipantParticipantDetailsViewModel.Args(
                participantId = flowViewModel.participantId.value,
                isManualSetParticipantID = flowViewModel.isManualEnteredId.value,
                leftEyeScanned = flowViewModel.leftEyeScanned.value,
                rightEyeScanned = flowViewModel.rightEyeScanned.value,
                phoneNumber = flowViewModel.phoneNumber.value,
                participantUuid = flowViewModel.participantUuid.value,
                registerDetails = flowViewModel.registerDetails.value
            )
        )
    }

    private fun submitRegistration() {
        viewModel.submitRegistration(flowViewModel.participantPicture.value)
    }

    private fun updateGender() {
        val gender = when {
            binding.rbGenderMale.isChecked -> Gender.MALE
            binding.rbGenderFemale.isChecked -> Gender.FEMALE
            else -> return
        }
        viewModel.setGender(gender)
        val currentDetails = flowViewModel.registerDetails.value
        if (currentDetails != null) {
            val updatedDetails = currentDetails.copy(gender = gender)
            flowViewModel.registerDetails.set(updatedDetails)
        }
    }

    override fun onHomeLocationPicked(address: HomeLocationPickerViewModel.AddressUiModel) {
        viewModel.setHomeLocation(address.addressMap, address.stringRepresentation)

        val currentDetails = flowViewModel.registerDetails.value
        if (currentDetails != null) {
            val updatedDetails = currentDetails.copy(address = address.addressMap)
            flowViewModel.registerDetails.set(updatedDetails)
        }
    }

    override fun confirmNoTelephone() {
        viewModel.canSkipPhone = true
        submitRegistration()
    }

    override fun continueRegistrationWithSuccessDialog() {
        viewModel.hasChildBeenVaccinatedAlreadyAsked = true
        submitRegistration()
    }

    override fun continueRegistrationWithCaptureVaccinesPage() {
        flowViewModel.confirmRegistrationWithCaptureVaccinesPage(viewModel.registerParticipantRequest.value!!)
    }

    override fun onBirthDatePicked(birthDate: DateTime?, isEstimated: Boolean) {
        birthDatePicked = birthDate
        isBirthDateEstimated = isEstimated
        viewModel.setBirthDate(birthDate)
        val currentDetails = flowViewModel.registerDetails.value
        if (currentDetails != null) {
            val updatedDetails = currentDetails.copy(birthDate = birthDate!!, isBirthDateEstimated = false)
            flowViewModel.registerDetails.set(updatedDetails)
        }
    }

    override fun onEstimatedAgePicked(
        estimatedBirthDate: DateTime?,
        yearsEstimated: Int?,
        monthsEstimated: Int?,
        weeksEstimated: Int?
    ) {
        estimatedBirthDatePicked = estimatedBirthDate
        this.yearsEstimated = yearsEstimated
        this.monthsEstimated = monthsEstimated
        this.weeksEstimated = weeksEstimated

        viewModel.setBirthDateBasedOnEstimatedBirthdate(estimatedBirthDate)
        viewModel.setEstimatedAgeText(yearsEstimated, monthsEstimated, weeksEstimated)
        val currentDetails = flowViewModel.registerDetails.value
        if (currentDetails != null) {
            val updatedDetails = currentDetails.copy(birthDate = estimatedBirthDate!!, isBirthDateEstimated = true)
            flowViewModel.registerDetails.set(updatedDetails)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (flowViewModel.participantUuid.value != null) {
            menu.findItem(R.id.action_cancel).isVisible = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel) {
        (requireActivity() as BaseActivity).run {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant)
            )
            finish()
        }
    }

    override fun finishParticipantFlow() {
        (requireActivity() as BaseActivity).run {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}