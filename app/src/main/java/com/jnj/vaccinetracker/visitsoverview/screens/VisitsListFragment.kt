package com.jnj.vaccinetracker.visitsoverview.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.databinding.FragmentVisitsListBinding
import com.jnj.vaccinetracker.visitsoverview.VisitsListViewModel
import com.jnj.vaccinetracker.visitsoverview.adapters.VisitsAdapter
import com.jnj.vaccinetracker.visitsoverview.dialog.VisitDetailsDialog
import com.jnj.vaccinetracker.visitsoverview.dialog.VisitsOverviewDatePickerDialog
import com.jnj.vaccinetracker.visitsoverview.model.VisitDataDTO
import com.jnj.vaccinetracker.visitsoverview.model.VisitDetailsDTO
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import java.util.Locale

class VisitsListFragment(private val visitsKey: String) : BaseFragment(), VisitsOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var binding: FragmentVisitsListBinding
    private lateinit var visitsAdapter: VisitsAdapter
    private val visitsListViewModel: VisitsListViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visits_list, container, false)

        setupRecyclerView()
        loadVisitsData()
        setupObservers()
        setupFiltersButton()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = visitsKey
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        visitsAdapter = VisitsAdapter { visitData ->
            showVisitDetailsDialog(visitData)
        }
        binding.visitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.visitsRecyclerView.adapter = visitsAdapter
    }

    private fun loadVisitsData() {
        when (visitsKey) {
            Constants.VISITS_OVERVIEW_SCHEDULED_VISITS_KEY -> visitsListViewModel.getScheduledVisitsData()
            Constants.VISITS_OVERVIEW_HISTORICAL_VISITS_KEY -> visitsListViewModel.getHistoricalVisitsData()
            Constants.VISITS_OVERVIEW_MISSED_VISITS_KEY -> visitsListViewModel.getMissedVisitsData()
        }
    }

    private fun setupObservers() {
        visitsListViewModel.visitDTOs.observe(viewLifecycleOwner) { visits ->
            visits?.let {
                applyFilters(it)
            }
        }

        visitsListViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupFiltersButton() {
        binding.btnStartDate.setOnClickListener {
            showDatePickerDialog(true)
        }

        binding.btnEndDate.setOnClickListener {
            showDatePickerDialog(false)
        }

        binding.searchBox.addTextChangedListener {
            applyFilters()
        }
    }

    private fun applyFilters(visits: List<VisitDataDTO> = visitsListViewModel.visitDTOs.value ?: emptyList()) {
        val searchText = binding.searchBox.text.toString().lowercase(Locale.getDefault())
        val filteredVisits = visits.filter { visit ->
            val dateMatches = (selectedStartDate == null || visit.startDatetime >= selectedStartDate?.toDate()) &&
                    (selectedEndDate == null || visit.startDatetime <= selectedEndDate?.toDate())
            val textSearchMatches = visit.participant.participantId.lowercase(Locale.getDefault()).contains(searchText)
                    || visit.participant.fullName.lowercase(Locale.getDefault()).contains(searchText)

            dateMatches && textSearchMatches
        }
        visitsAdapter.submitList(filteredVisits)
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val datePickerDialog = VisitsOverviewDatePickerDialog(selectedDate = if (isStartDate) selectedStartDate else selectedEndDate)
        datePickerDialog.show(childFragmentManager, if (isStartDate) START_DATE_PICKER_DIALOG_TAG else END_DATE_PICKER_DIALOG_TAG)
    }

    override fun onDatePicked(date: DateTime?, tag: String?) {
        date?.let {
            if (tag == START_DATE_PICKER_DIALOG_TAG) {
                selectedStartDate = it
                binding.labelStartDate.text = formatDate(it)

            } else if (tag == END_DATE_PICKER_DIALOG_TAG) {
                selectedEndDate = it
                binding.labelEndDate.text = formatDate(it)
            }

            if (selectedStartDate != null && selectedEndDate != null) {
                applyFilters()
            }
        }
    }

    private fun formatDate(date: DateTime?): String {
        return date?.let { DateUtil.convertDateToString(date.toDate(), DateFormat.FORMAT_DATE.toString()) } ?: "Not set"
    }

    private fun showVisitDetailsDialog(visitData: VisitDataDTO) {
        val visitDetails = VisitDetailsDTO(
            formattedVisitDate = visitData.formattedStartDateTime,
            vaccines = "test value",
            phoneNumber = visitData.participant.phone ?: "",
            clientID = visitData.participant.participantId,
            clientFullName = visitData.participant.fullName
        )

        val dialog = VisitDetailsDialog.newInstance(visitDetails, visitsKey)
        dialog.show(parentFragmentManager, "VisitDetailsDialog")
    }
}