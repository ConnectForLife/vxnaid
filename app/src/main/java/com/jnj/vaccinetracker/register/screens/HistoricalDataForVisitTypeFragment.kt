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
import com.jnj.vaccinetracker.common.dialogs.AlertDialog
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.DraftVisit
import com.jnj.vaccinetracker.common.domain.entities.ObservationValue
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.SessionExpiryObserver
import com.jnj.vaccinetracker.common.helpers.logError
import com.jnj.vaccinetracker.common.helpers.rethrowIfFatal
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.HistoricalVisitDateDialog
import com.jnj.vaccinetracker.register.dialogs.UpdateParticipantSuccessfulDialog
import com.jnj.vaccinetracker.register.dialogs.VaccineDialog
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
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
   HistoricalVisitDateDialog.HistoricalVisitDateListener,
   VaccineDialog.AddVaccineListener {
   @Inject
   lateinit var visitManager: VisitManager
   @Inject
   lateinit var userRepository: UserRepository
   @Inject
   lateinit var syncSettingsRepository: SyncSettingsRepository
   @Inject
   lateinit var createVisitUseCase: CreateVisitUseCase
   @Inject
   lateinit var configurationManager: ConfigurationManager
   @Inject
   lateinit var sessionExpiryObserver: SessionExpiryObserver

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
      private const val TAG_VACCINE_PICKER = "vaccinePickerDialog"

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
         viewModel.filterSubstanceDates.value = false
         val birthDate = allDataViewModel.getParticipantBirthDate()
         HistoricalVisitDateDialog.create(
             birthDate = birthDate,
             disabledDates = emptySet(),
             visitType = visitTypeName ?: Constants.EMPTY_STRING_VALUE
         ).show(childFragmentManager, TAG_HISTORICAL_VISIT_DATE)
      }
      lifecycleScope.launch {
         getAllSubstancesFromAllVisitsForGivenVisitType(visitTypeName)
      }

      return binding.root
   }

   private fun applyReceivedLLINFilter(otherSubstance: OtherSubstanceDataModel): Boolean {
      val currentVisitType = arguments?.getString(ARG_VISIT_TYPE_NAME)
      return otherSubstance.conceptName == Constants.CONCEPT_NAME_RECEIVED_LLIN && allDataViewModel.visitTypesData.value?.any { (_, historicalData) ->
         currentVisitType != viewModel.firstVisitTypeName && historicalData.value[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]?.substanceValueMap?.get(
            Constants.CONCEPT_NAME_RECEIVED_LLIN
         ) == Constants.YES_ANSWER
      } == true
   }

   private fun initViewModels() {
      visitTypeName = arguments?.getString(ARG_VISIT_TYPE_NAME)
      visitUuid = arguments?.getString(ARG_VISIT_UUID)

      viewModel.isLocalEdit.value = allDataViewModel.isEdit.value != null && visitUuid != null
      viewModel.isGlobalEdit.value = allDataViewModel.isEdit.value

      viewModel.setArguments(
         HistoricalDataForVisitTypeViewModel.Args(
            visitTypeName = visitTypeName,
            visitUuid = visitUuid
         )
      )

      // this could be refactored to use viewModel.getOtherDataForVisitType(visitTypeName!!) (and the substance method)
      // and injecting values if they exist depending on source for edit and create mode
      // not using seperate maps for dates and values but operate on DataModels
      if (allDataViewModel.isEdit.value == true) {
         handleEditMode()
      } else {
         handleCreateMode()
      }
   }

   private fun handleCreateMode() {
      lifecycleScope.launch {
         allDataViewModel.visitTypesData.value?.get(visitTypeName)?.let { historicalData ->
            val substancesAndDatesMap = historicalData.value[Constants.SUBSTANCES_AND_DATES_STR]?.substanceValueMap?.toMutableMap()
            val otherSubstancesAndValuesMap = historicalData.value[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]?.substanceValueMap?.toMutableMap()

            viewModel.substancesAndDates.value = substancesAndDatesMap
            viewModel.otherSubstancesAndValues.value = otherSubstancesAndValuesMap

            viewModel.substancesData.value = mapToSubstanceDataList(substancesAndDatesMap)
            viewModel.otherSubstancesData.value = mapToOtherSubstanceDataList(otherSubstancesAndValuesMap)
         }
      }
   }

   private fun handleEditMode() {
      val visit = allDataViewModel.groupedVisitsByType.value
         ?.get(visitTypeName)
         ?.firstOrNull { it.uuid == visitUuid }

      visit?.observations?.let { observations ->
         lifecycleScope.launch {
            val substancesForVisitTypeList = viewModel.getSubstancesDataForVisitType(visitTypeName!!)
            viewModel.substancesData.value =
               mapEditSubstanceDataList(observations, substancesForVisitTypeList!!)
            viewModel.substancesAndDates.value = viewModel.substancesData.value
               ?.associate { it.conceptName to it.obsDate.toString() }
               ?.toMutableMap()
         }

         lifecycleScope.launch {
            val otherDataForVisitTypeList = viewModel.getOtherDataForVisitType(visitTypeName!!)
            viewModel.otherSubstancesData.value =
               mapEditOtherSubstanceDataList(observations, otherDataForVisitTypeList)
            viewModel.otherSubstancesAndValues.value = viewModel.otherSubstancesData.value
               ?.associate { it.conceptName to it.value.toString() }
               ?.toMutableMap()
         }
      }
   }

   private suspend fun getAllSubstancesFromAllVisitsForGivenVisitType(visitTypeName: String?) {
      if (visitTypeName == null) return
      val substancesForVisitTypeList = viewModel.getSubstancesDataForVisitType(visitTypeName)
      if (substancesForVisitTypeList.isEmpty()) {
         Log.w("SubstanceError", "No substance data available for this visit type.")
         return
      }
      val visits = allDataViewModel.groupedVisitsByType.value?.get(visitTypeName)
      if (visits.isNullOrEmpty()) {
         Log.w("VisitError", "No visits found for the given visit type: $visitTypeName")
         return
      }
      val allObservations: MutableMap<String, ObservationValue> = mutableMapOf()
      for (visit in visits) {
         visit.observations.let { observationMap ->
            allObservations.putAll(observationMap)  // Merge observations into the map
         }
      }
      viewModel.substancesFromAllVisitsFromVisitType.value = mapEditSubstanceDataList(allObservations, substancesForVisitTypeList)
   }

   private fun mapToSubstanceDataList(data: Map<String, String>?): List<SubstanceDataModel> {
      return data?.map { (conceptName, date) ->
         SubstanceDataModel(
            conceptName = conceptName,
            label = Constants.NOT_NEEDED_STRING_VALUE,
            category = Constants.NOT_NEEDED_STRING_VALUE,
            routeOfAdministration = Constants.NOT_NEEDED_STRING_VALUE,
            group = Constants.NOT_NEEDED_STRING_VALUE,
            maximumAgeInWeeks = null,
            minimumWeeksNumberAfterPreviousDose = null,
            visitType = Constants.NOT_NEEDED_STRING_VALUE,
            obsDate = date
         )
      } ?: emptyList()
   }

   private fun mapToOtherSubstanceDataList(data: Map<String, String>?): List<OtherSubstanceDataModel> {
      return data?.map { (conceptName, value) ->
         OtherSubstanceDataModel(
            conceptName = conceptName,
            label = conceptName,
            category = Constants.NOT_NEEDED_STRING_VALUE,
            inputType = Constants.NOT_NEEDED_STRING_VALUE,
            visitType = Constants.NOT_NEEDED_STRING_VALUE,
            options = emptyList(),
            value = value
         )
      } ?: emptyList()
   }

   private fun mapEditSubstanceDataList(
      observations: Map<String, ObservationValue>,
      substancesList: List<SubstanceDataModel>
   ): List<SubstanceDataModel> {
      return observations.filter { it.key.endsWith(Constants.VXNAID_DATE_CONCEPT_NAME_SUFFIX) }.map { (key, value) ->
         val conceptName = key.removeSuffix("${Constants.SPACE_STR}${Constants.DATE_STR}")
         val label = substancesList.find { it.conceptName == conceptName }?.label ?: conceptName

         SubstanceDataModel(
            conceptName = conceptName,
            label = label,
            category = Constants.NOT_NEEDED_STRING_VALUE,
            routeOfAdministration = Constants.NOT_NEEDED_STRING_VALUE,
            group = Constants.NOT_NEEDED_STRING_VALUE,
            maximumAgeInWeeks = null,
            minimumWeeksNumberAfterPreviousDose = null,
            visitType = Constants.NOT_NEEDED_STRING_VALUE,
            obsDate = value.value
         )
      }
   }

   private suspend fun mapEditOtherSubstanceDataList(
      observations: Map<String, ObservationValue>,
      otherDataList: List<OtherSubstanceDataModel>
   ): List<OtherSubstanceDataModel> {
      val orderList = viewModel.getOtherDataForVisitType(visitTypeName!!) // Desired order.

      val filteredObservations = observations.filterNot { (key, _) ->
         key.endsWith(Constants.MANUFACTURER_NAME_STR) ||
                 key.endsWith(Constants.BARCODE_STR) ||
                 key.endsWith(Constants.DATE_STR)
      }

      val mappedData = filteredObservations.mapNotNull { (key, value) ->
         otherDataList.find { it.conceptName == key }?.let { configItem ->
            OtherSubstanceDataModel(
               conceptName = key,
               label = configItem.label ?: key,
               category = Constants.NOT_NEEDED_STRING_VALUE,
               inputType = configItem.inputType ?: "text",
               visitType = Constants.NOT_NEEDED_STRING_VALUE,
               options = configItem.options ?: emptyList(),
               value = value.value
            )
         }
      }

      val mappedDataMap = mappedData.associateBy { it.conceptName }

      return orderList.mapNotNull { configItem ->
         mappedDataMap[configItem.conceptName]
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
         val filteredOtherSubstances = otherSubstanceItems?.filterNot { otherSubstance ->
            applyReceivedLLINFilter(otherSubstance)
         }
         otherSubstanceAdapter.updateItemsList(filteredOtherSubstances)
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
      binding.btnAddVaccine.setOnClickListener {
         lifecycleScope.launch {
            val allSubstances = viewModel.substancesFromAllVisitsFromVisitType.value ?: emptyList()
            val visitSubstances = viewModel.allSubstancesDataForVisitType.value ?: emptyList()
            val currentSubstances = viewModel.substancesData.value ?: emptyList()

            val combinedSubstances =
               (currentSubstances + allSubstances).distinctBy { it.conceptName }

            val filteredSubstances = visitSubstances.filterNot { substance ->
               combinedSubstances.any { it.conceptName == substance.conceptName || it.obsDate.isNullOrEmpty() }
            }

            VaccineDialog(filteredSubstances).show(childFragmentManager, TAG_VACCINE_PICKER)
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
            binding.btnSubmit.visibility = View.INVISIBLE
         }
      }
   }

   private fun submitHistoricalData() {
      if (isValidData()) {
         allDataViewModel.addVisitTypeData(
            viewModel.visitTypeName.value!!,
            viewModel.substancesAndDates.value,
            viewModel.otherSubstancesAndValues.value!!,
            viewModel.visitDate.value
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

      try {
         visitManager.registerDosingVisit(
            encounterDatetime = Date(),
            visitUuid = visit.uuid,
            participantUuid = flowViewModel.participant.value!!.participantUuid,
            dosingNumber = visit.dosingNumber ?: 0,
            substanceObservations = getObservationsForVisitEncounter(),
            otherSubstanceObservations = viewModel.otherSubstancesAndValues.value
               ?: mutableMapOf(),
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

   private fun showSuccessDialog() {
      UpdateParticipantSuccessfulDialog().show(
         childFragmentManager,
         RegisterParticipantParticipantDetailsFragment.TAG_UPDATE_SUCCESS_DIALOG
      )
   }

   private fun showErrorMessage(message: String) {
      AlertDialog(requireContext()).showAlertDialog(message)
   }

   private fun getVisitForUpdate(): VisitDetail? {
      return allDataViewModel.groupedVisitsByType.value
         ?.get(visitTypeName)
         ?.firstOrNull { it.uuid == visitUuid }
   }

   private fun getObservationsForVisitEncounter(): Map<String, Map<String, String>> {
      val substances: MutableMap<String, String> =
         viewModel.substancesAndDates.value ?: mutableMapOf()
      return substances.mapValues {
         mapOf(
            Constants.DATE_STR to it.value,
            Constants.MANUFACTURER_NAME_STR to "",
            Constants.BARCODE_STR to ""
         )
      }.toMutableMap()
   }

   private fun getParsedSubstances(): Map<String, String> {
      val substances: MutableMap<String, String> =
         viewModel.substancesAndDates.value ?: mutableMapOf()
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
      val visit = createVisitUseCase.createVisit(
         buildNextVisitObject(
            flowViewModel.participant.value!!,
            date
         )
      ).toVisitDetail()
      return visit
   }

   private suspend fun buildNextVisitObject(
      participant: ParticipantSummaryUiModel,
      visitDate: Date
   ): CreateVisit {
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
   private suspend fun findVisitType(
      participant: ParticipantSummaryUiModel,
      visitTime: Date
   ): String {
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
      viewModel.substancesData.value?.forEach { substance ->
         viewModel.addVaccineDate(substance.conceptName, date.format(DateFormat.FORMAT_DATE))
      }
   }

   override fun addVaccine(vaccine: SubstanceDataModel) {
      viewModel.addVaccineDate(vaccine.conceptName, "")
      val currentSubstances = viewModel.substancesData.value?.toMutableList() ?: mutableListOf()
      currentSubstances.add(vaccine)
      viewModel.substancesData.value = currentSubstances
   }
}
