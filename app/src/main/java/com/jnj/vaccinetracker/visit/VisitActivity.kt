package com.jnj.vaccinetracker.visit

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.barcode.ScanBarcodeViewModel
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.SyncBanner
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.ActivityVisitBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowActivity
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.VaccineDialog
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.dialog.DialogScheduleMissingSubstances
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog
import java.util.Date
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.screens.ContraindicationsFragment
import com.jnj.vaccinetracker.visit.screens.ReferralFragment
import com.soywiz.klock.DateTime
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitActivity :
    BaseActivity(),
    DosingOutOfWindowDialog.DosingOutOfWindowDialogListener,
    DialogScheduleMissingSubstances.DialogScheduleMissingSubstancesListener,
    VaccineDialog.AddVaccineListener,
    ReferralFragment.OnReferralPageFinishListener,
    RescheduleVisitDialog.RescheduleVisitListener
{

    companion object {
        private const val EXTRA_PARTICIPANT = "participant"
        private const val TAG_DIALOG_SUCCESS = "successDialog"
        private const val EXTRA_TYPE = "newParticipantRegistration"
        const val TAG_DIALOG_DOSING_OUT_OF_WINDOW = "dosingOutOfWindowDialog"
        private const val TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES = "scheduleMissingSubstances"
        private const val TAG_VACCINE_PICKER = "vaccinePicker"
        const val CURRENT_VISIT_UUID = "currentVisitUuid"
        const val PARTICIPANT_UUID = "participantUuid"
        const val IS_AFTER_VISIT = "isAfterVisit"

        fun create(context: Context, participant: ParticipantSummaryUiModel, newRegisteredParticipant: Boolean): Intent {
            return Intent(context, VisitActivity::class.java)
                .putExtra(EXTRA_PARTICIPANT, participant)
                .putExtra(EXTRA_TYPE, newRegisteredParticipant)
        }
    }

    private val participantArg: ParticipantSummaryUiModel by lazy { intent.getParcelableExtra(EXTRA_PARTICIPANT)!! }
    private val newRegisteredParticipantArg: Boolean by lazy { intent.getBooleanExtra(EXTRA_TYPE, false) }
    private val viewModel: VisitViewModel by viewModels { viewModelFactory }
    private val scanModel:ScanBarcodeViewModel by viewModels{ viewModelFactory }
    private lateinit var binding: ActivityVisitBinding
    private var isFirstTab = true

    private var errorSnackbar: Snackbar? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanModel.setArguments(participantArg)
        viewModel.setArguments(participantArg)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_visit)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.viewPagerVisit.adapter = VisitPagerAdapter(this, supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPagerVisit)
        setupClickListeners()

        if (savedInstanceState == null) {
            val fragment = ContraindicationsFragment()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, fragment)
                addToBackStack(null)
            }
        }

        setTitle(R.string.visit_label_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(!newRegisteredParticipantArg)
    }

    private fun setupClickListeners() {
        binding.root.setOnClickListener { this.currentFocus?.hideKeyboard() }
        binding.btnSubmit.setOnClickListener {
            onSubmit()
        }
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == 0) {
                    makeSubmitBtnInvisible()
                    makeAddVaccineButtonInvisible()
                    isFirstTab = true
                } else if (tab.position == 1) {
                    makeSubmitBtnVisible()
                    makeAddVaccineButtonVisible()
                    isFirstTab = false
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Optional: Handle tab unselected logic here
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Optional: Handle tab reselected logic here
            }
        })
        binding.switchSuggest.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsSuggesting(isChecked)
            onVisitTypesChanged(viewModel.visitTypes.value)
            if (!isChecked) {
                lifecycleScope.launch {
                    viewModel.onVisitTypeDropdownChange()
                }
            }
        }
        binding.btnAddVaccine.setOnClickListener {
            onBtnAddVaccine()
        }
        binding.dropdownVisitTypes.setOnItemClickListener { _, _, position, _ ->
            val selectedVisitType = viewModel.visitTypes.value?.distinct()?.get(position)
                ?: return@setOnItemClickListener

            lifecycleScope.launch {
                updateSelectedVisitType(selectedVisitType)
                viewModel.onVisitTypeDropdownChange()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorSnackbar?.dismiss()

            if (errorMessage == null) {
                return@observe
            }

            errorSnackbar = Snackbar
                .make(binding.root, errorMessage, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.general_label_retry) {
                    errorSnackbar?.dismiss()
                    viewModel.onRetryClick()
                }.also {
                    it.show()
                }
        }
        viewModel.visitEvents
            .asFlow()
            .onEach { success ->
                if (success)
                    onDosingVisitRegistrationSuccessful()
                else
                    onDosingVisitRegistrationFailed()
            }.launchIn(this)
        viewModel.isSuggesting.observe(this) {isSuggesting ->
            onSuggestingSwitch(isSuggesting)
        }
        viewModel.visitTypes.observe(this) { visitTypes ->
            onVisitTypesChanged(visitTypes)
        }
    }

    private fun onVisitTypesChanged(visitTypes: List<String>?) {
        val adapter =
            ArrayAdapter(this, R.layout.item_dropdown, visitTypes?.distinct().orEmpty())
        binding.dropdownVisitTypes.setAdapter(adapter)
    }

    private fun validateDosingVisit(): Boolean {
        return viewModel.validateDosingVisit(
            missingSubstancesListener = ::showScheduleMissingSubstancesDialog
        )
    }

    private fun showScheduleMissingSubstancesDialog(substances: List<String>) {
        DialogScheduleMissingSubstances(substances).show(supportFragmentManager,
            TAG_DIALOG_SCHEDULE_MISSING_SUBSTANCES
        )
    }

    override fun onOutOfWindowDosingCanceled() {
        goToMatchParticipantFragment()
    }

    override fun onDialogScheduleMissingSubstancesConfirmed(date: Date) {
        viewModel.missingSubstancesVisitDate.value = date
        navigateToReferralFragment(isAfterVisit = true)
    }

    fun makeSubmitBtnVisible() {
        binding.btnSubmit.visibility = View.VISIBLE
    }

    fun makeSubmitBtnInvisible() {
        binding.btnSubmit.visibility = View.INVISIBLE
    }

    fun makeAddVaccineButtonVisible() {
        if (viewModel.isSuggesting.value == false && !isFirstTab) {
            binding.btnAddVaccine.visibility = View.VISIBLE
        }
    }

    fun makeAddVaccineButtonInvisible() {
        binding.btnAddVaccine.visibility = View.INVISIBLE
    }

    private fun makeVisitTypeDropdownVisible() {
        binding.groupVisitTypdropdown.visibility = View.VISIBLE
    }

    private fun makeVisitTypeDropdownGone() {
        binding.groupVisitTypdropdown.visibility = View.GONE
    }

    private fun makeVisitTypeLabelVisible() {
        binding.labelVisitType.visibility = View.VISIBLE
    }

    private fun makeVisitTypeLabelGone() {
        binding.labelVisitType.visibility = View.GONE
    }

    private fun onSubmit() {
        if (!validateSubmission()) {
            viewModel.isAnyOtherSubstancesEmpty.value = false
            binding.tabLayout.getTabAt(0)?.select()
            return
        }
        val isValid = validateDosingVisit()
        if (isValid) {
            navigateToReferralFragment(isAfterVisit = true)
        }
    }


    private fun validateSubmission(): Boolean {
        viewModel.checkIfAnyOtherSubstancesEmpty()
        return viewModel.isAnyOtherSubstancesEmpty.value != true
    }


    private fun navigateToReferralFragment(isAfterVisit: Boolean) {
        val dosingVisit = viewModel.dosingVisit.value
        val referralFragment = ReferralFragment().apply {
            arguments = Bundle().apply {
                putString(CURRENT_VISIT_UUID, dosingVisit!!.uuid)
                putString(PARTICIPANT_UUID, viewModel.participant.value!!.participantUuid)
                putBoolean(IS_AFTER_VISIT, isAfterVisit)
            }
        }

        supportFragmentManager.beginTransaction()
            .animateNavigationDirection(NavigationDirection.FORWARD)
            .replace(R.id.fragment_container, referralFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun onSuggestingSwitch(suggestingValue: Boolean) {
        if (!suggestingValue) {
            val generalColor = ContextCompat.getColorStateList(this, R.color.colorTextOnLight)
            binding.tabLayout.backgroundTintList = null
            binding.viewPagerVisit.backgroundTintList = null
            binding.labelSuggest.setTextColor(generalColor)
            makeAddVaccineButtonVisible()
            makeVisitTypeDropdownVisible()
            makeVisitTypeLabelGone()
        } else {
            val colorAccent = ContextCompat.getColorStateList(this, R.color.colorAccent)
            binding.tabLayout.backgroundTintList = colorAccent
            binding.viewPagerVisit.backgroundTintList = colorAccent
            binding.labelSuggest.setTextColor(colorAccent)
            makeAddVaccineButtonInvisible()
            makeVisitTypeDropdownGone()
            makeVisitTypeLabelVisible()
        }
    }

    private fun onBtnAddVaccine() {
        val allSubstances = viewModel.substancesDataAll.value.orEmpty()
        val selectedSubstances = viewModel.selectedSubstancesData.value?.map { it.conceptName }?.toSet() ?: setOf()
        val filteredSubstances = allSubstances.filter { it.conceptName !in selectedSubstances }
        VaccineDialog(filteredSubstances).show(supportFragmentManager, TAG_VACCINE_PICKER)
    }

    private fun updateSelectedVisitType(name: String?) {
        if (name != viewModel.selectedVisitType.value) {
            viewModel.selectedVisitType.value = name.orEmpty()
        }
    }

    override fun addVaccine(vaccine: SubstanceDataModel) {
        viewModel.addToSelectedSubstances(vaccine)
    }

    override fun onReferralAfterVisitPageFinish() {
        val missingSubstanceVisitDate = viewModel.missingSubstancesVisitDate.value
        val visitPlace =
            getSharedPreferences(Constants.USER_PREFERENCES_FILE_NAME, MODE_PRIVATE).getString(
                Constants.VISIT_PLACE_FILE_KEY,
                Constants.VISIT_PLACE_STATIC
            )
        viewModel.submitDosingVisit(newVisitDate = missingSubstanceVisitDate, visitPlace = visitPlace)
    }

    override fun onReferralAfterContraindicationsPageFinish() {
        val context = this
        lifecycleScope.launch {
            try {
            viewModel.onReferralAfterContraindications()
            } catch (e: Exception) {
                Log.e("Rescheduling a visit", "Reschedule has failed failed", e)
                com.jnj.vaccinetracker.common.dialogs.AlertDialog(context).showAlertDialog(getString(R.string.reschedule_visit_failed))
            }
        }
    }
    private fun onDosingVisitRegistrationSuccessful() {
        val dosingVisit = viewModel.dosingVisit.value
        VisitRegisteredSuccessDialog.create(viewModel.upcomingVisit.value, viewModel.participant.value, dosingVisit!!.uuid).show(supportFragmentManager,
            TAG_DIALOG_SUCCESS
        )
    }

    private fun onDosingVisitRegistrationFailed() {
        Snackbar.make(binding.root, R.string.general_label_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        handleOnBackPressed(currentFragment)
    }

    private fun handleOnBackPressed(currentFragment: Fragment?) {
        when {
            currentFragment is ContraindicationsFragment && currentFragment.isVisible -> {
                // When in ContraindicationsFragment, launch ParticipantFlowActivity
                goToMatchParticipantFragment()
            }
            currentFragment is ReferralFragment && currentFragment.isVisible -> {
                handleReferralFragmentBackPress(currentFragment)
            }
            else -> {
                // When in other activity or fragment
                navigateToContraindicationsFragment()
            }
        }
    }

    private fun goToMatchParticipantFragment() {
        finish()
    }

    private fun handleReferralFragmentBackPress(currentFragment: ReferralFragment) {
        if (currentFragment.isAfterVisit) {
            supportFragmentManager.beginTransaction()
                .animateNavigationDirection(NavigationDirection.BACKWARD)
                .remove(currentFragment)
                .commit()
        } else {
            navigateToContraindicationsFragment()
        }
    }

    private fun navigateToContraindicationsFragment() {
        val fragment = ContraindicationsFragment()
        supportFragmentManager.beginTransaction()
            .animateNavigationDirection(NavigationDirection.BACKWARD)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override val syncBanner: SyncBanner
        get() = binding.syncBanner

    override fun onRescheduleVisitListener(newVisitDate: DateTime, rescheduleReasonText: String) {
        viewModel.contraindicationsRescheduleDate.value = newVisitDate
        viewModel.contraindicationsRescheduleReasonText.value = rescheduleReasonText
        navigateToReferralFragment(isAfterVisit = false)
    }
}