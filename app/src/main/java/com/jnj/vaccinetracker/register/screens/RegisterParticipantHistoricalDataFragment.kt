package com.jnj.vaccinetracker.register.screens

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.dialogs.AlertDialog
import com.jnj.vaccinetracker.common.helpers.dpToPx
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentRegisterHistoricalVisitsBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowActivity
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.HistoricalVisitDateDialog
import com.jnj.vaccinetracker.register.dialogs.MultipleVisitsDialog
import com.jnj.vaccinetracker.register.dialogs.RegisterParticipantSuccessfulDialog
import com.soywiz.klock.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class RegisterParticipantHistoricalDataFragment : BaseFragment(),
   MultipleVisitsDialog.MultipleVisitsListener,
   RegisterParticipantSuccessfulDialog.RegisterParticipationCompletionListener {

   private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
   private val viewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }
   private val registerViewModel: RegisterParticipantParticipantDetailsViewModel by activityViewModels { viewModelFactory }
   private lateinit var binding: FragmentRegisterHistoricalVisitsBinding
   @Inject lateinit var configurationManager: ConfigurationManager
   private var setupButtonsJob: Job? = null

   companion object {
      private const val TAG_SUCCESS_DIALOG = "successDialog"
      private const val TAG_MULTIPLE_VISITS_DIALOG = "multipleVisitsDialog"
   }

   override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(
         inflater,
         R.layout.fragment_register_historical_visits,
         container,
         false
      )
      binding.apply {
         viewModel = this@RegisterParticipantHistoricalDataFragment.viewModel
         lifecycleOwner = viewLifecycleOwner
         flowViewModel = this@RegisterParticipantHistoricalDataFragment.flowViewModel
      }
      viewModel.setArguments(flowViewModel.registerParticipant.value, flowViewModel.participant.value)
      binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

      setupClickListeners()

      return binding.root
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      val combinedSource = MediatorLiveData<Unit>().apply {
         addSource(viewModel.visitTypesData) { value = Unit }
         addSource(viewModel.groupedVisitsByType) { value = Unit }
      }

      combinedSource.observe(lifecycleOwner) {
         setupButtons()
      }
      observeViewModelEvents(lifecycleOwner)
   }

   private fun observeViewModelEvents(lifecycleOwner: LifecycleOwner) {
      viewModel.registerVaccinesSuccessEvents
         .asFlow()
         .onEach { participant ->
            RegisterParticipantSuccessfulDialog.create(participant)
               .show(childFragmentManager, TAG_SUCCESS_DIALOG)
         }
         .launchIn(lifecycleOwner.lifecycleScope)
   }

   private fun showDatePickerDialog() {
     val birthDate = viewModel.getParticipantBirthDate()
      val visitType = "historical"
      val disabledDates = viewModel.getDisabledDatesForVisitType(visitType)

      val dialog = HistoricalVisitDateDialog.create(birthDate, disabledDates, visitType)
      dialog.show(parentFragmentManager, "HistoricalVisitDateDialog")
   }

   fun onDatePicked(date: DateTime) {
      viewModel.setHistoricalVisitDate(date)
   }

   private fun setupClickListeners() {
      binding.btnSubmit.setOnClickListener {
         lifecycleScope.launch {
            if (!viewModel.isEdit.value!!) {
               try {
                  viewModel.loading.set(true)
                  val participantUiModel =
                     registerViewModel.doRegistrationUsingRegisterRequest(flowViewModel.registerParticipant.value!!)
                  viewModel.participant.value = participantUiModel
                  submitVaccineRegistration()
                  registerViewModel.registerParticipantSuccessDialogEvents.tryEmit(
                     participantUiModel!!
                  )
               } catch (ex: Exception) {
                  showErrorMessage(resourcesWrapper.getString(R.string.participant_registration_smth_wrong_happened))
               } finally {
                  viewModel.loading.set(false)
               }
            }
         }
      }
   }

   private fun showErrorMessage(message: String) {
      AlertDialog(requireContext()).showAlertDialog(message)
   }

   private fun submitVaccineRegistration() {
      lifecycleScope.launch {
         try {
            viewModel.submitVaccineRegistration()
         } catch (e: Exception) {
            e.printStackTrace()
         }
      }
   }

   private fun setupButtons() {
      setupButtonsJob?.cancel()

      setupButtonsJob = lifecycleScope.launch {
         binding.buttonGrid.apply {
            removeAllViews()
            configurationManager.getSubstancesConfig()
               .map { it.visitType }
               .distinct()
               .forEach { visitTypeName ->
                  addView(createButton(visitTypeName))
               }
         }
      }
   }

   private fun createButton(name: String): FrameLayout {
      val button = Button(requireContext()).apply {
         layoutParams = createButtonLayoutParams()
         text = name
         setTextAppearance(R.style.ButtonTextStyling)
         setTextColor(ContextCompat.getColorStateList(context, R.color.colorTextOnPrimary))
         background = ContextCompat.getDrawable(context, R.drawable.rounded_button_30dp)
         backgroundTintList = getButtonBackgroundTint(name)
         setOnClickListener { onButtonClicked(name) }
      }

      val badgeCount = viewModel.groupedVisitsByType.value?.get(name)?.size ?: 0
      val badgeView = if (badgeCount > 0) createBadgeView(badgeCount) else null

      return FrameLayout(requireContext()).apply {
         layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
         )
         addView(button)
         badgeView?.let { addView(it) }
      }
   }

   private fun createBadgeView(count: Int): TextView {
      return TextView(requireContext()).apply {
         layoutParams = FrameLayout.LayoutParams(
            24.dpToPx, // Width of the badge
            24.dpToPx, // Height of the badge
            Gravity.END or Gravity.TOP // Position the badge on the top-right of the button
         ).apply {
            setMargins(0, 8.dpToPx, 8.dpToPx, 0) // Margin to position the badge
         }
         text = count.toString()
         textSize = 12f
         setTextColor(ContextCompat.getColor(context, R.color.colorTextOnPrimary))
         background = ContextCompat.getDrawable(context, R.drawable.circle_badge_background)
         gravity = Gravity.CENTER
      }
   }

   private fun createButtonLayoutParams(): FrameLayout.LayoutParams {
      val size = 100.dpToPx
      return FrameLayout.LayoutParams(size, size).apply {
         setMargins(32.dpToPx, 32.dpToPx, 32.dpToPx, 32.dpToPx)
      }
   }

   private fun getButtonBackgroundTint(visitTypeName: String) = if (shouldButtonHighlight(visitTypeName)) {
      ContextCompat.getColorStateList(requireContext(), R.color.colorPrimary)
   } else {
      ContextCompat.getColorStateList(requireContext(), R.color.colorTextOnLight)
   }

   private fun shouldButtonHighlight(visitTypeName: String): Boolean {
      return viewModel.visitTypesData.value?.containsKey(visitTypeName) == true
              || viewModel.groupedVisitsByType.value?.containsKey(visitTypeName) == true
   }

   private fun onButtonClicked(name: String) {
      val isVisitEmpty = viewModel.groupedVisitsByType.value?.get(name)?.isEmpty() ?: true
      if (viewModel.isEdit.value == true && !isVisitEmpty) {
         displayMultipleVisitsDialog(name)
      } else {
         flowViewModel.openHistoricalDataForVisitType(name)
      }
   }

   private fun displayMultipleVisitsDialog(name: String) {
      MultipleVisitsDialog.create(name).show(childFragmentManager, TAG_MULTIPLE_VISITS_DIALOG)
   }

   override fun continueWithParticipantVisit(participant: ParticipantSummaryUiModel) {
      finishActivityWithResult(participant)
   }

   override fun finishParticipantFlow() {
      finishActivityWithResult()
   }

   private fun finishActivityWithResult(participant: ParticipantSummaryUiModel? = null) {
      (requireActivity() as BaseActivity).run {
         setResult(
            RESULT_OK,
            Intent().putExtra(RegisterParticipantFlowActivity.EXTRA_PARTICIPANT, participant)
         )
         finish()
      }
   }

   override fun onVisitPicked(visitType: String, visitUuid: String) {
      flowViewModel.openHistoricalDataForVisitType(visitType, visitUuid)
   }
}
