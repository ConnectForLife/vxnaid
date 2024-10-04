package com.jnj.vaccinetracker.register.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentHistoricalDataForVisitTypeBinding
import com.jnj.vaccinetracker.register.adapters.SubstanceItemAdapter
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.register.RegisterParticipantFlowViewModel
import com.jnj.vaccinetracker.register.dialogs.HistoricalVisitDateDialog
import com.jnj.vaccinetracker.visit.model.OtherSubstanceDataModel
import com.jnj.vaccinetracker.visit.model.SubstanceDataModel
import com.soywiz.klock.DateTime

@RequiresApi(Build.VERSION_CODES.O)
class HistoricalDataForVisitTypeFragment :
   BaseFragment(),
   OtherSubstanceItemAdapter.AddSubstanceValueListener,
   HistoricalVisitDateDialog.HistoricalVisitDateListener
{

   private val viewModel: HistoricalDataForVisitTypeViewModel by viewModels { viewModelFactory }
   private val allDataViewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }
   private val flowViewModel: RegisterParticipantFlowViewModel by activityViewModels { viewModelFactory }
   private lateinit var binding: FragmentHistoricalDataForVisitTypeBinding
   private lateinit var substanceAdapter: SubstanceItemAdapter
   private lateinit var otherSubstanceAdapter: OtherSubstanceItemAdapter

   companion object {
      private const val ARG_VISIT_TYPE_NAME = "visitTypeName"
      private const val TAG_HISTORICAL_VISIT_DATE = "historicalVisitDateDialog"

      fun create(visitTypeName: String?): HistoricalDataForVisitTypeFragment {
         return HistoricalDataForVisitTypeFragment().apply {
            arguments = Bundle().apply {
               putString(ARG_VISIT_TYPE_NAME, visitTypeName)
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

      viewModel.substancesData.observe(viewLifecycleOwner) { substances ->
         substanceAdapter.updateList(substances)
      }

      viewModel.otherSubstancesData.observe(viewLifecycleOwner) { otherSubstances ->
         otherSubstanceAdapter.updateItemsList(otherSubstances)
      }

      if (!doesSubstancesHaveAnyDates()) {
         HistoricalVisitDateDialog().show(childFragmentManager, TAG_HISTORICAL_VISIT_DATE)
      }

      return binding.root
   }

   private fun initViewModels() {
      val visitTypeName = arguments?.getString(ARG_VISIT_TYPE_NAME)
      viewModel.setArguments(HistoricalDataForVisitTypeViewModel.Args(visitTypeName = visitTypeName))

      allDataViewModel.visitTypesData.value?.get(visitTypeName)?.let { visitTypeData ->
         viewModel.substancesAndDates.value = visitTypeData[Constants.SUBSTANCES_AND_DATES_STR]
         viewModel.otherSubstancesAndValues.value = visitTypeData[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]

         val substanceDataList = visitTypeData[Constants.SUBSTANCES_AND_DATES_STR]?.map { (conceptName, date) ->
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
         viewModel.substancesData.value = substanceDataList

         val otherSubstancesDataList = visitTypeData[Constants.OTHER_SUBSTANCES_AND_VALUES_STR]?.map { (conceptName, value) ->
            OtherSubstanceDataModel(
               conceptName = conceptName,
               label = conceptName,
               category = "notNeeded",
               inputType = "notNeeded",
               options = emptyList(),
               value = value
            )
         } ?: emptyList()
         viewModel.otherSubstancesData.value = otherSubstancesDataList
      }
   }

   private fun setupActionBarTitle() {
      (activity as? AppCompatActivity)?.supportActionBar?.title = arguments?.getString(ARG_VISIT_TYPE_NAME)
   }

   private fun setupRecyclerViews() {
      substanceAdapter = SubstanceItemAdapter(mutableListOf(), viewModel, requireActivity().supportFragmentManager, requireContext())
      binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerView.adapter = substanceAdapter

      otherSubstanceAdapter = OtherSubstanceItemAdapter(mutableListOf(), this, registerParticipant = flowViewModel.registerParticipant.value!!)
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
      viewModel.otherSubstancesAndValues.observe(lifecycleOwner){value ->
         otherSubstanceAdapter.otherSubstanceValues = value
      }
   }

   private fun setupClickListeners() {
      binding.btnSubmit.setOnClickListener {
         submitHistoricalData()
      }
   }

   private fun submitHistoricalData() {
      if (otherSubstanceAdapter.checkIfAnyItemsEmpty(viewModel.otherSubstancesAndValues.value, binding.recyclerViewOtherSubstances).not()) {
         allDataViewModel.addVisitTypeData(
            viewModel.visitTypeName.value!!,
            viewModel.substancesAndDates.value,
            viewModel.otherSubstancesAndValues.value!!
         )
         flowViewModel.navigateBack()
      }
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
