package com.jnj.vaccinetracker.register.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentHistoricalDataForVisitTypeBinding
import com.jnj.vaccinetracker.register.adapters.SubstanceItemAdapter
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.HistoricalVisitDateDialog
import com.jnj.vaccinetracker.register.dialogs.UpdateParticipantSuccessfulDialog
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.jnj.vaccinetracker.visit.screens.ReferralFragment
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class HistoricalDataForVisitTypeFragment :
   BaseFragment(),
   OtherSubstanceItemAdapter.AddSubstanceValueListener,
   HistoricalVisitDateDialog.HistoricalVisitDateListener {
   @Inject lateinit var visitManager: VisitManager
   @Inject lateinit var userRepository: UserRepository
   @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
   @Inject lateinit var createVisitUseCase: CreateVisitUseCase
   @Inject lateinit var configurationManager: ConfigurationManager
   @Inject lateinit var sessionExpiryObserver: SessionExpiryObserver

   private val viewModel: HistoricalDataForVisitTypeViewModel by viewModels { viewModelFactory }
   private val allDataViewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }
   private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }

   private lateinit var binding: FragmentHistoricalDataForVisitTypeBinding
   private lateinit var substanceAdapter: SubstanceItemAdapter
   private lateinit var otherSubstanceAdapter: OtherSubstanceItemAdapter
   private var visitTypeName: String? = null
   private var visitUuid: String? = null

   companion object {
      private const val ARG_VISIT_TYPE_NAME = "visitTypeName"
      private const val ARG_VISIT_UUID = "visitUuid"
      private const val TAG_HISTORICAL_VISIT_DATE = "historicalVisitDateDialog"

      fun create(visitTypeName: String?, visitUuid: String?): HistoricalDataForVisitTypeFragment {
         return HistoricalDataForVisitTypeFragment().apply {
            arguments = Bundle().apply {
               putString(ARG_VISIT_TYPE_NAME, visitTypeName)
               putString(ARG_VISIT_UUID, visitUuid)
            }
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      initViewModels()
      setupActionBarTitle()
   }

   override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(
         inflater,
         R.layout.fragment_historical_data_for_visit_type,
         container,
         false
      )
      binding.viewModel = viewModel
      binding.lifecycleOwner = viewLifecycleOwner

      binding.root.setOnClickListener { activity?.currentFocus?.hideKeyboard() }

      setupRecyclerViews()
      setupClickListeners()

      if (!doesSubstancesHaveAnyDates() && viewModel.isLocalEdit.value != true) {
         HistoricalVisitDateDialog().show(childFragmentManager, TAG_HISTORICAL_VISIT_DATE)
      }

      return binding.root
   }

   private fun initViewModels() {
      visitTypeName = arguments?.getString(ARG_VISIT_TYPE_NAME)
      visitUuid = arguments?.getString(ARG_VISIT_UUID)

      viewModel.isLocalEdit.value = allDataViewModel.isEdit.value != null && visitUuid != null

      viewModel.setArguments(
         HistoricalDataForVisitTypeViewModel.Args(
            visitTypeName = visitTypeName,
            visitUuid = visitUuid
         )
      )

      if (allDataViewModel.isEdit.value == true) {
         handleEditMode()
      } else {
         handleCreateMode()
      }
   }

   private fun handleCreateMode() {
      allDataViewModel.visitTypesData.value?.get(visitTypeName)?.let { visitTypeData ->
         viewModel.substancesAndDates.value = visitTypeData[Constants.SUBSTANCES_AND_DATES_STR]
         viewModel.otherSubstancesAndValues.value = visitTypeData[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]

         viewModel.substancesData.value = mapToSubstanceDataList(
            visitTypeData[Constants.SUBSTANCES_AND_DATES_STR]
         )
         viewModel.otherSubstancesData.value = mapToOtherSubstanceDataList(
            visitTypeData[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]
         )
      }
   }

   private fun handleEditMode() {
      val visit = allDataViewModel.groupedVisitsByType.value
         ?.get(visitTypeName)
         ?.firstOrNull { it.uuid == visitUuid }

      visit?.observations?.let { observations ->
         lifecycleScope.launch {
            val substancesForVisitTypeList = viewModel.getSubstancesDataForVisitType(visitTypeName!!)
            viewModel.substancesData.value = mapEditSubstanceDataList(observations, substancesForVisitTypeList)
            viewModel.substancesAndDates.value = viewModel.substancesData.value
               ?.associate { it.conceptName to it.obsDate.toString() }
               ?.toMutableMap()
         }

         lifecycleScope.launch {
            val otherDataForVisitTypeList = viewModel.getOtherDataForVisitType(visitTypeName!!)
            viewModel.otherSubstancesData.value = mapEditOtherSubstanceDataList(observations, otherDataForVisitTypeList)
            viewModel.otherSubstancesAndValues.value = viewModel.otherSubstancesData.value
               ?.associate { it.conceptName to it.value.toString() }
               ?.toMutableMap()
         }
      }
   }

   private fun mapToSubstanceDataList(data: Map<String, String>?): List<SubstanceDataModel> {
      return data?.map { (conceptName, date) ->
         SubstanceDataModel(
            conceptName = conceptName,
            label = "notNeeded",
            category = "notNeeded",
            routeOfAdministration = "notNeeded",
            group = "notNeeded",
            maximumAgeInWeeks = null,
            minimumWeeksNumberAfterPreviousDose = null,
            visitType = "notNeeded",
            obsDate = date
         )
      } ?: emptyList()
   }

   private fun mapToOtherSubstanceDataList(data: Map<String, String>?): List<OtherSubstanceDataModel> {
      return data?.map { (conceptName, value) ->
         OtherSubstanceDataModel(
            conceptName = conceptName,
            label = conceptName,
            category = "notNeeded",
            inputType = "notNeeded",
            visitType = "notNeeded",
            options = emptyList(),
            value = value
         )
      } ?: emptyList()
   }

   private fun mapEditSubstanceDataList(
      observations:  Map<String, ObservationValue>,
      substancesList: List<SubstanceDataModel>
   ): List<SubstanceDataModel> {
      return observations.filter { it.key.endsWith("Vxnaid Date") }.map { (key, value) ->
         val conceptName = key.removeSuffix("${Constants.SPACE_STR}${Constants.DATE_STR}")
         val label = substancesList.find { it.conceptName == conceptName }?.label ?: conceptName

         SubstanceDataModel(
            conceptName = conceptName,
            label = label,
            category = "notNeeded",
            routeOfAdministration = "notNeeded",
            group = "notNeeded",
            maximumAgeInWeeks = null,
            minimumWeeksNumberAfterPreviousDose = null,
            visitType = "notNeeded",
            obsDate = value.value
         )
      }
   }

   private fun mapEditOtherSubstanceDataList(
      observations:  Map<String, ObservationValue>,
      otherDataList: List<OtherSubstanceDataModel>
   ): List<OtherSubstanceDataModel> {
      return observations.filterNot { it.key.endsWith(Constants.MANUFACTURER_NAME_STR) ||
              it.key.endsWith(Constants.BARCODE_STR) ||
              it.key.endsWith(Constants.DATE_STR) }.map { (key, value) ->
         val inputType = otherDataList.find { it.conceptName == key }?.inputType ?: "text"
         val options = otherDataList.find { it.conceptName == key }?.options ?: emptyList()
         val label = otherDataList.find { it.conceptName == key }?.label ?: key

         OtherSubstanceDataModel(
            conceptName = key,
            label = label,
            category = "notNeeded",
            inputType = inputType,
            visitType = "notNeeded",
            options = options,
            value = value.value
         )
      }
   }

   private fun setupActionBarTitle() {
      (activity as? AppCompatActivity)?.supportActionBar?.title =
         arguments?.getString(ARG_VISIT_TYPE_NAME)
   }

   private fun setupRecyclerViews() {
      substanceAdapter = SubstanceItemAdapter(
         mutableListOf(),
         viewModel,
         requireActivity().supportFragmentManager,
         requireContext()
      )
      binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerView.adapter = substanceAdapter

      otherSubstanceAdapter = OtherSubstanceItemAdapter(
         mutableListOf(),
         this,
         participant = flowViewModel.participant.value,
         registerParticipant = flowViewModel.registerParticipant.value
      )
      binding.recyclerViewOtherSubstances.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerViewOtherSubstances.adapter = otherSubstanceAdapter
   }

   override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
      viewModel.substancesData.observe(lifecycleOwner) { substanceItems ->
         substanceAdapter.updateList(substanceItems)
      }
      viewModel.otherSubstancesData.observe(lifecycleOwner) { otherSubstanceItems ->
         otherSubstanceAdapter.updateItemsList(otherSubstanceItems)
      }
      viewModel.otherSubstancesAndValues.observe(lifecycleOwner) { value ->
         otherSubstanceAdapter.otherSubstanceValues = value
      }
   }

   private fun setupClickListeners() {
      binding.btnSubmit.setOnClickListener {
         when {
            viewModel.isLocalEdit.value == true -> handleEditVisit()
            allDataViewModel.isEdit.value == true -> handleNewVisitDuringUpdate()
            else -> submitHistoricalData()
         }
      }
   }

   private fun handleEditVisit() {
      lifecycleScope.launch {
         if (isValidData()) {
            updateHistoricalVisit()
            allDataViewModel.loadOnEdit()
         }
      }
   }

   private fun handleNewVisitDuringUpdate() {
      lifecycleScope.launch {
         if (isValidData()) {
            createNewVisitDuringUpdate()
            allDataViewModel.loadOnEdit()
         }
      }
   }

   private fun submitHistoricalData() {
      if (isValidData()) {
         allDataViewModel.addVisitTypeData(
            viewModel.visitTypeName.value!!,
            viewModel.substancesAndDates.value,
            viewModel.otherSubstancesAndValues.value!!
         )
         flowViewModel.navigateBack()
      }
   }

   private fun isValidData(): Boolean {
      val errorList = otherSubstanceAdapter.checkIfAnyItemsEmpty(
         viewModel.otherSubstancesAndValues.value,
         binding.recyclerViewOtherSubstances
      )
      return if (errorList.isEmpty()) true else {
         displayValidationErrorDialog(errorList)
         false
      }
   }

   private fun updateHistoricalVisit() {
      val visit = getVisitForUpdate() ?: return
      val otherSubstances = viewModel.otherSubstancesAndValues.value ?: mutableMapOf()
      val substances = getParsedSubstances()

      lifecycleScope.launch {
         try {
            visitManager.updateVisitObservations(
               visit,
               participantUuid = flowViewModel.participantUuid.value!!,
               visitObservations = otherSubstances + substances,
               appendObservations = false
            )
            showSuccessDialog()
         } catch (e: Exception) {
            Log.e("ReferralFragment", "Referral failed", e)
            showErrorMessage(getString(R.string.referral_page_failed_referral_text))
         }
      }
   }

   private suspend fun createNewVisitDuringUpdate() {
      viewModel.loading.value = true
      val date = viewModel.visitDate.value!!.toDate()
      val visit = createVisitForNewVisit(date)

      lifecycleScope.launch {
         try {
            visitManager.registerDosingVisit(
               encounterDatetime = Date(),
               visitUuid = visit.uuid,
               participantUuid = flowViewModel.participant.value!!.participantUuid,
               dosingNumber = visit.dosingNumber ?: 0,
               substanceObservations = getObservationsForVisitEncounter(),
               otherSubstanceObservations = viewModel.otherSubstancesAndValues.value ?: mutableMapOf(),
               visitTypeVxnaid = visitTypeName
            )
            showSuccessDialog()
         } catch (ex: OperatorUuidNotAvailableException) {
            sessionExpiryObserver.notifySessionExpired()
         } catch (throwable: Throwable) {
            throwable.rethrowIfFatal()
            logError("Failed to register dosing visit: ", throwable)
         } finally {
            viewModel.loading.value = false
         }
      }
   }

   private fun showSuccessDialog() {
      UpdateParticipantSuccessfulDialog().show(
         childFragmentManager,
         RegisterParticipantParticipantDetailsFragment.TAG_UPDATE_SUCCESS_DIALOG
      )
   }

   private fun showErrorMessage(message: String) {
      com.jnj.vaccinetracker.common.dialogs.AlertDialog(requireContext()).showAlertDialog(message)
   }

   private fun getVisitForUpdate(): VisitDetail? {
      return allDataViewModel.groupedVisitsByType.value
         ?.get(visitTypeName)
         ?.firstOrNull { it.uuid == visitUuid }
   }

   fun getObservationsForVisitEncounter(): Map<String, Map<String, String>> {
      val substances: MutableMap<String, String> = viewModel.substancesAndDates.value ?: mutableMapOf()
      return substances.mapValues {
         mapOf(
            Constants.DATE_STR to it.value,
            Constants.MANUFACTURER_NAME_STR to "",
            Constants.BARCODE_STR to ""
         )
      }.toMutableMap()
   }

   private fun getParsedSubstances(): Map<String, String> {
      val substances: MutableMap<String, String> = viewModel.substancesAndDates.value ?: mutableMapOf()
      return substances.flatMap { (key, value) ->
         listOf(
            "$key ${Constants.DATE_STR}" to value,
            "$key ${Constants.MANUFACTURER_NAME_STR}" to "",
            "$key ${Constants.BARCODE_STR}" to ""
         )
      }.toMap().toMutableMap()
   }

   private fun DraftVisit.toVisitDetail(): VisitDetail {
      return VisitDetail(
         uuid = visitUuid,
         visitType = visitType,
         visitDate = startDatetime,
         attributes = attributes,
         observations = mapOf()
      )
   }

   private suspend fun createVisitForNewVisit(date: Date): VisitDetail {
      val visit = createVisitUseCase.createVisit(buildNextVisitObject(flowViewModel.participant.value!!, date)).toVisitDetail()
      return visit
   }

   private suspend fun buildNextVisitObject(participant: ParticipantSummaryUiModel, visitDate: Date): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      val visitType = findVisitType(participant, visitDate)
      return CreateVisit(
         participantUuid = participant.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = visitDate,
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_OCCURRED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
            Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitType,
         )
      )
   }

   @RequiresApi(Build.VERSION_CODES.O)
   private suspend fun findVisitType(participant: ParticipantSummaryUiModel, visitTime: Date): String {
      val participantVisits = visitManager.getVisitsForParticipant(participant.participantUuid)
      return SubstancesDataUtil.getVisitTypeForVisitWithGivenDate(
         participant.birthDateText,
         DateUtil.convertDateToString(visitTime, DateFormat.FORMAT_DATE.toString()),
         participantVisits,
         configurationManager
      )
   }


   private fun doesSubstancesHaveAnyDates(): Boolean {
      val substancesAndDates = viewModel.substancesAndDates.value
      if (substancesAndDates.isNullOrEmpty()) {
         return false
      }
      return substancesAndDates.values.any { it.isNotEmpty() }
   }

   override fun addOtherSubstance(substanceName: String, value: String) {
      viewModel.addObsToOtherSubstancesObsMap(substanceName, value)
   }

   override fun onDatePicked(date: DateTime) {
      viewModel.visitDate.value = date
      substanceAdapter.reload()
   }
}
