package com.jnj.vaccinetracker.visitsoverview.screens

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.databinding.FragmentRegisteredChildrenBinding
import com.jnj.vaccinetracker.visitsoverview.adapters.PatientAdapter
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.Q)
class RegisteredParticipantsFragment(private val participantKey: String) : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var binding: FragmentRegisteredChildrenBinding
    private val registeredParticipantsViewModel: RegisteredParticipantsViewModel by viewModels { viewModelFactory }
    private lateinit var patientAdapter: PatientAdapter
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_registered_children,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        setupFilterButtons()
        setupObservers()
        loadPatients()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.participantRecyclerView.adapter = patientAdapter// binding the adapter to recyclerview
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = participantKey
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        // Initialize the adapter without a click listener
        patientAdapter = PatientAdapter()
        binding.participantRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.participantRecyclerView.adapter = patientAdapter
    }

    private fun loadPatients() {
        when (participantKey) {
            Constants.REGISTERED_PARTICIPANT -> registeredParticipantsViewModel.fetchAllPatients()
        }
    }

    private fun setupObservers() {
        registeredParticipantsViewModel.patientDTOs.observe(viewLifecycleOwner) { patients ->
            patients?.let {
                applyFilters(it)
            }
        }

        registeredParticipantsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupFilterButtons() {
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

    private fun applyFilters(
        patients: List<ParticipantDataDTO> = registeredParticipantsViewModel.patientDTOs.value ?: emptyList()
    ) {
        val searchText = binding.searchBox.text.toString().lowercase(Locale.getDefault())
        Log.d("FilterDebug", "Search text: $searchText")

        // Convert selectedStartDate and selectedEndDate to java.util.Date (DateEntity)
        val selectedStartDateAsJavaDate: Date? = selectedStartDate?.let { Date(it.unixMillisLong) }
        val selectedEndDateAsJavaDate: Date? = selectedEndDate?.let { Date(it.unixMillisLong) }

        Log.d("FilterDebug", "Selected start date: $selectedStartDateAsJavaDate")
        Log.d("FilterDebug", "Selected end date: $selectedEndDateAsJavaDate")

        val filteredPatients = patients.filter { patient ->
            // Date comparison using java.util.Date methods
            val dateMatches =
                (selectedStartDateAsJavaDate == null || !patient.startDatetime.before(selectedStartDateAsJavaDate)) &&
                        (selectedEndDateAsJavaDate == null || !patient.startDatetime.after(selectedEndDateAsJavaDate))

            Log.d("FilterDebug", "Date matches for ${patient.participantId}: $dateMatches")

            // Text search comparison
            val textSearchMatches =
                patient.participantId.lowercase(Locale.getDefault()).contains(searchText) ||
                        patient.fullName.lowercase(Locale.getDefault()).contains(searchText) ||
                        patient.motherName.lowercase(Locale.getDefault()).contains(searchText)

            Log.d("FilterDebug", "Text search matches for ${patient.participantId}: $textSearchMatches")

            dateMatches && textSearchMatches
        }

        Log.d("FilterDebug", "Filtered patients count: ${filteredPatients.size}")

        // Format dates for display (this part is still fine)
        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)

        // Submit the filtered list to the adapter
        patientAdapter.submitList(filteredPatients)
    }



    private fun showDatePickerDialog(isStartDate: Boolean) {
        val datePickerDialog =
            ReportOverviewDatePickerDialog(selectedDate = if (isStartDate) selectedStartDate else selectedEndDate)
        datePickerDialog.show(
            childFragmentManager,
            if (isStartDate) START_DATE_PICKER_DIALOG_TAG else END_DATE_PICKER_DIALOG_TAG
        )
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
        return date?.let {
            DateUtil.convertDateToString(
                date.toDate(),
                DateFormat.FORMAT_DATE.toString()
            )
        } ?: "Not set"
    }
}

