package com.jnj.vaccinetracker.visitsoverview.screens

import android.annotation.SuppressLint
import android.content.Context
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
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.databinding.FragmentRegisteredChildrenBinding
import com.jnj.vaccinetracker.visitsoverview.dto.ParticipantDataDTO
import com.soywiz.klock.DateTime
import android.graphics.Typeface
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.visitsoverview.adapters.PatientAdapter
import com.jnj.vaccinetracker.visitsoverview.dialog.PatientDetailsDialog
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import com.soywiz.klock.DateFormat
import com.soywiz.klock.jvm.toDate
import org.apache.poi.hssf.usermodel.HSSFWorkbook
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

    private lateinit var patientAdapter: PatientAdapter
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
        setupDownloadButtons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Testing", "RegisteredParticipantsFragment onViewCreated called")

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

        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)

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

    fun onDatePicked(date: DateTime?, tag: String?) {
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

        val headers = listOf("Participant ID", "Full Name", "View")
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

        // Add the header row to the table
        tableLayout.addView(headerRow)

        // Add rows for each patient
        filteredPatients.forEach { patient ->
            val row = TableRow(requireContext()).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
            }

            // Add Participant ID
            val idTextView = TextView(requireContext()).apply {
                text = patient.participantId
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(16, 8, 16, 8)
                gravity = Gravity.START
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(idTextView)

            // Add Full Name
            val nameTextView = TextView(requireContext()).apply {
                text = patient.fullName
                textSize = 14f
                setTextColor(Color.BLACK)
                setPadding(16, 8, 16, 8)
                gravity = Gravity.START
                layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
            }
            row.addView(nameTextView)

            val eyeIcon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.eye_icon)
                setPadding(16, 8, 16, 8)
                layoutParams = TableRow.LayoutParams(
                    0,
                    TableRow.LayoutParams.WRAP_CONTENT,
                    1f
                ).apply {
                    gravity = Gravity.CENTER
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxWidth = 48.dpToPx(requireContext())
                maxHeight = 48.dpToPx(requireContext())
            }

            eyeIcon.setOnClickListener {
                val dialog = PatientDetailsDialog.newInstance(patient, patient.participantId)
                dialog.showNow(requireActivity().supportFragmentManager, "PatientDetailsDialog")
            }
            row.addView(eyeIcon)
            tableLayout.addView(row)
        }
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun buildFileName(): String {
        return "${participantKey.replace(" ", "_")}_${
            DateUtil.convertDateToString(
                dateNow(),
                DateFormat.FORMAT_DATE.toString()
            )
        }"
    }

    private fun setupDownloadButtons() {
        binding.btnDownloadExcel.setOnClickListener {
            exportToExcel(patientAdapter.currentList)
        }
    }

    private fun exportToExcel(patients: List<ParticipantDataDTO>) {
        val fileName = "${buildFileName()}.xls"
        val mimeType = "application/vnd.ms-excel"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet(participantKey.replace(" ", "_"))

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue(Constants.VISIT_DATE_FILE_COLUMN_HEADER)
            headerRow.createCell(1).setCellValue(Constants.CLIENT_ID_FILE_COLUMN_HEADER)
            headerRow.createCell(2).setCellValue(Constants.CLIENT_NAME_FILE_COLUMN_HEADER)
            headerRow.createCell(3).setCellValue(Constants.PHONE_NUMBER_FILE_COLUMN_HEADER)
            headerRow.createCell(4).setCellValue(Constants.CLIENT_MOTHER_NAME_FILE_HEADER)

            patients.forEachIndexed { index, patients ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(patients.formattedStartDateTime)
                row.createCell(1).setCellValue(patients.participantId)
                row.createCell(2).setCellValue(patients.fullName)
                row.createCell(4).setCellValue(patients.motherName)
            }

            sheet.setColumnWidth(0, 4000)
            sheet.setColumnWidth(1, 4000)
            sheet.setColumnWidth(2, 7000)
            sheet.setColumnWidth(3, 4000)
            sheet.setColumnWidth(4, 7000)
            workbook.write(outputStream)
            workbook.close()
        }
    }

}
