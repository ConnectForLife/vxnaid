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
class RegisteredParticipantsFragment(participantKey: String) : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val ARG_PARTICIPANT_KEY = "participantKey"
        fun newInstance(participantKey: String) = RegisteredParticipantsFragment(participantKey).apply {
            arguments = Bundle().apply {
                putString(ARG_PARTICIPANT_KEY, participantKey)
            }
        }
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var binding: FragmentRegisteredChildrenBinding
    private val registeredParticipantsViewModel: RegisteredParticipantsViewModel by viewModels { viewModelFactory }
    private lateinit var patientAdapter: PatientAdapter
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null
    private lateinit var participantKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        participantKey = arguments?.getString(ARG_PARTICIPANT_KEY) ?: throw IllegalArgumentException("Missing participantKey")
        Log.d("Testing", "RegisteredParticipantsFragment onCreate called..............................")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Testing", "RegisteredParticipantsFragment onCreateView called..................")
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
        Log.d("Testing", "RegisteredParticipantsFragment onViewCreated called>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")

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
        val selectedStartDateAsJavaDate: Date? = selectedStartDate?.let { Date(it.unixMillisLong) }
        val selectedEndDateAsJavaDate: Date? = selectedEndDate?.let { Date(it.unixMillisLong) }

        val filteredPatients = patients.filter { patient ->
            val dateMatches =
                (selectedStartDateAsJavaDate == null || !patient.startDatetime.before(selectedStartDateAsJavaDate)) &&
                        (selectedEndDateAsJavaDate == null || !patient.startDatetime.after(selectedEndDateAsJavaDate))

            val textSearchMatches =
                patient.participantId.lowercase(Locale.getDefault()).contains(searchText) ||
                        patient.fullName.lowercase(Locale.getDefault()).contains(searchText) ||
                        patient.motherName.lowercase(Locale.getDefault()).contains(searchText)

            dateMatches && textSearchMatches
        }

        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)

        patientAdapter.submitList(filteredPatients)

        // Show empty view if the list is empty
        if (filteredPatients.isEmpty()) {
            binding.participantRecyclerView.visibility = View.GONE
        } else {
            binding.participantRecyclerView.visibility = View.VISIBLE
        }
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

