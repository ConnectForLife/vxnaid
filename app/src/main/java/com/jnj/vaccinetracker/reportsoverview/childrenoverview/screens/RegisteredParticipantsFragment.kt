package com.jnj.vaccinetracker.reportsoverview.childrenoverview.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.visitsoverview.adapters.PatientAdapter
import com.jnj.vaccinetracker.reportsoverview.childrenoverview.model.RegisteredParticipantsViewModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.jvm.toDate
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.util.Locale
import java.time.ZoneId


@RequiresApi(Build.VERSION_CODES.Q)
class RegisteredParticipantsFragment : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var patientAdapter: PatientAdapter
    private lateinit var binding: FragmentRegisteredChildrenBinding
    private val registeredParticipantsViewModel: RegisteredParticipantsViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_registered_children, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        setupRecyclerView()
        loadPatients()
        setupObservers()
        setupFilterButtons()
        setupDownloadButtons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.registered_children)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                parentFragmentManager.popBackStack()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter()
        binding.patientsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.patientsRecyclerView.adapter = patientAdapter
    }

    private fun loadPatients() {
       registeredParticipantsViewModel.fetchAllPatients()
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
        val filteredPatients = patients.filter { patient ->
            val childRegistrationDate = patient.registrationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val startDate = selectedStartDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
            val endDate = selectedEndDate?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()

            val dateMatches =
                (startDate == null || childRegistrationDate >= startDate) &&
                (endDate == null || childRegistrationDate <= endDate)

            val textSearchMatches =
                patient.participantId.lowercase(Locale.getDefault()).contains(searchText) ||
                patient.fullName.lowercase(Locale.getDefault()).contains(searchText) ||
                patient.motherName.lowercase(Locale.getDefault()).contains(searchText)

            dateMatches && textSearchMatches
        }

        binding.totalPatientCount.text =
            getString(R.string.children_overview_total_children_count_label, filteredPatients.size)

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
                it.toDate(),
                DateFormat.FORMAT_DATE.toString()
            )
        } ?: "Not set"
    }

    private fun buildFileName(): String {
        return "${getString(R.string.reports_overview_title).replace(" ", "_")}_${
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

        binding.btnDownloadCsv.setOnClickListener {
            exportToCSV(patientAdapter.currentList)
        }
    }

    private fun exportToExcel(patients: List<ParticipantDataDTO>) {
        val fileName = "${buildFileName()}.xls"
        val mimeType = "application/vnd.ms-excel"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet(getString(R.string.reports_overview_title).replace(" ", "_"))

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue(Constants.CLIENT_ID_FILE_COLUMN_HEADER)
            headerRow.createCell(1).setCellValue(Constants.CLIENT_NAME_FILE_COLUMN_HEADER)
            headerRow.createCell(2).setCellValue(Constants.CLIENT_MOTHER_NAME_FILE_HEADER)
            headerRow.createCell(3).setCellValue(Constants.CLIENT_BIRTHDATE_FILE_HEADER)

            patients.forEachIndexed { index, patient ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(patient.participantId)
                row.createCell(1).setCellValue(patient.fullName)
                row.createCell(2).setCellValue(patient.motherName)
                row.createCell(3).setCellValue(patient.registrationDate)
            }

            sheet.setColumnWidth(0, 4000)
            sheet.setColumnWidth(1, 7000)
            sheet.setColumnWidth(2, 7000)
            sheet.setColumnWidth(3, 4000)

            workbook.write(outputStream)
            workbook.close()
        }
    }

    private fun exportToCSV(patients: List<ParticipantDataDTO>) {
        val fileName = "${buildFileName()}.csv"
        val mimeType = "text/csv"
        val titleRowColumns = "${Constants.CLIENT_ID_FILE_COLUMN_HEADER}, ${Constants.CLIENT_NAME_FILE_COLUMN_HEADER}, ${Constants.CLIENT_MOTHER_NAME_FILE_HEADER}, ${Constants.CLIENT_BIRTHDATE_FILE_HEADER} \n"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                writer.write(titleRowColumns)
                patients.forEach { patient ->
                    writer.write("${patient.participantId}, ${patient.fullName},${patient.motherName},${patient.formattedBirthDate} \n")
                }
            }
        }
    }
}
