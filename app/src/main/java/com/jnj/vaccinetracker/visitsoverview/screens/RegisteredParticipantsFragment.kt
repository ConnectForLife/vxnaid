package com.jnj.vaccinetracker.visitsoverview.screens

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
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
import com.soywiz.klock.DateTime
import android.graphics.Typeface
import android.widget.TableLayout
import androidx.core.content.ContextCompat
import com.soywiz.klock.jvm.toDate
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.Q)
class RegisteredParticipantsFragment : BaseFragment() {

    companion object {
        private const val ARG_PARTICIPANT_KEY = "participantKey"
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"

        fun newInstance(participantKey: String): RegisteredParticipantsFragment {
            return RegisteredParticipantsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARTICIPANT_KEY, participantKey)
                }
            }
        }
    }

    private lateinit var binding: FragmentRegisteredChildrenBinding
    private val registeredParticipantsViewModel: RegisteredParticipantsViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null
    private lateinit var participantKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        participantKey = arguments?.getString(ARG_PARTICIPANT_KEY)
            ?: throw IllegalArgumentException("Missing participantKey")
        Log.d("Testing", "RegisteredParticipantsFragment onCreate called")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Testing", "RegisteredParticipantsFragment onCreateView called")
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_registered_children,
            container,
            false
        )
        binding.lifecycleOwner = viewLifecycleOwner

        setupFilterButtons()
        setupObservers()
        loadPatients()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Testing", "RegisteredParticipantsFragment onViewCreated called")

        // Set up the action bar
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = participantKey
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun loadPatients() {
        when (participantKey) {
            Constants.REGISTERED_PARTICIPANT -> registeredParticipantsViewModel.fetchAllPatients()
        }
    }

    private fun setupObservers() {
        registeredParticipantsViewModel.patientDTOs.observe(viewLifecycleOwner) { patients ->
            patients?.let { applyFilters(it) }
        }
        registeredParticipantsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupFilterButtons() {
        binding.btnStartDate.setOnClickListener { showDatePickerDialog(true) }
        binding.btnEndDate.setOnClickListener { showDatePickerDialog(false) }
        binding.searchBox.addTextChangedListener { applyFilters() }
    }

    private fun applyFilters(
        patients: List<ParticipantDataDTO> = registeredParticipantsViewModel.patientDTOs.value ?: emptyList()
    ) {
        val searchText = binding.searchBox.text.toString().lowercase(Locale.getDefault())
        val selectedStartDateAsJavaDate = selectedStartDate?.toDate()
        val selectedEndDateAsJavaDate = selectedEndDate?.toDate()

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

        // Update the start/end date labels
        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)

        // Update the table with filtered patients
        updateRegisteredPatientsTable(filteredPatients)
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val datePickerDialog = ReportOverviewDatePickerDialog(
            selectedDate = if (isStartDate) selectedStartDate else selectedEndDate
        )
        datePickerDialog.show(
            childFragmentManager,
            if (isStartDate) START_DATE_PICKER_DIALOG_TAG else END_DATE_PICKER_DIALOG_TAG
        )
    }

    // Called from the date picker dialog callback
    fun onDatePicked(date: DateTime?, tag: String?) {
        date?.let {
            if (tag == START_DATE_PICKER_DIALOG_TAG) {
                selectedStartDate = it
                binding.labelStartDate.text = formatDate(it)
            } else if (tag == END_DATE_PICKER_DIALOG_TAG) {
                selectedEndDate = it
                binding.labelEndDate.text = formatDate(it)
            }
            applyFilters() // Refresh the list after picking a date
        }
    }

    private fun formatDate(date: DateTime?): String {
        return date?.let {
            DateUtil.convertDateToString(it.toDate(), "dd/MM/yyyy")
        } ?: "Not set"
    }

    @SuppressLint("SetTextI18n")
    private fun updateRegisteredPatientsTable(filteredPatients: List<ParticipantDataDTO>) {
        val tableLayout = binding.tableRegisteredChildren
        tableLayout.removeAllViews()

        if (filteredPatients.isEmpty()) {
            Log.d("TableUpdate", "No patients to display")
            return
        }

        val headerRow = TableRow(requireContext()).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        val headers = listOf("Participant ID", "Full Name")
        headers.forEach { headerText ->
            val headerTextView = TextView(requireContext()).apply {
                text = headerText
                setTypeface(null, Typeface.BOLD)
                textSize = 16f
                setTextColor(Color.WHITE)
                setPadding(16, 16, 16, 16)
                gravity = Gravity.CENTER
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            headerRow.addView(headerTextView)
        }
        tableLayout.addView(headerRow)

        filteredPatients.forEach { patient ->
            val row = TableRow(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }

            val idTextView = TextView(requireContext()).apply {
                text = patient.participantId
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(16, 8, 16, 8)
                gravity = Gravity.START
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(idTextView)

            val nameTextView = TextView(requireContext()).apply {
                text = patient.fullName
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(16, 8, 16, 8)
                gravity = Gravity.START
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(nameTextView)

            tableLayout.addView(row)
        }
    }
}