package com.jnj.vaccinetracker.visitsoverview.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.databinding.FragmentVisitsListBinding
import com.jnj.vaccinetracker.visitsoverview.model.VisitsListViewModel
import com.jnj.vaccinetracker.visitsoverview.adapters.VisitsAdapter
import com.jnj.vaccinetracker.visitsoverview.dialog.VisitDetailsDialog
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDataDTO
import com.jnj.vaccinetracker.visitsoverview.dto.VisitDetailsDTO
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import com.soywiz.klock.days
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
class VisitsListFragment : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
        private const val ARG_VISITS_KEY = "arg_visits_key"
        private const val STATE_START_DATE_MILLIS = "state_start_date_millis"
        private const val STATE_END_DATE_MILLIS = "state_end_date_millis"
        private const val PARENT_CLINIC_FILTER = "__PARENT__"

        fun newInstance(visitsKey: String): VisitsListFragment {
            return VisitsListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VISITS_KEY, visitsKey)
                }
            }
        }
    }

    private lateinit var binding: FragmentVisitsListBinding
    private lateinit var visitsAdapter: VisitsAdapter
    private val visitsListViewModel: VisitsListViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null
    private var selectedClinic: String? = null
    private var visitsKey: String = Constants.VISITS_OVERVIEW_SCHEDULED_VISITS_KEY

    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var visitManager: VisitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_VISITS_KEY)?.let { visitsKey = it }

        savedInstanceState?.let { bundle ->
            if (bundle.containsKey(STATE_START_DATE_MILLIS)) {
                val startMillis = bundle.getLong(STATE_START_DATE_MILLIS)
                selectedStartDate = DateTime(startMillis.toDouble())
            }
            if (bundle.containsKey(STATE_END_DATE_MILLIS)) {
                val endMillis = bundle.getLong(STATE_END_DATE_MILLIS)
                selectedEndDate = DateTime(endMillis.toDouble())
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedStartDate?.let { outState.putLong(STATE_START_DATE_MILLIS, it.toDate().time) }
        selectedEndDate?.let { outState.putLong(STATE_END_DATE_MILLIS, it.toDate().time) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visits_list, container, false)
        setupRecyclerView()
        setupObservers()
        setupFilterButtons()
        setupClinicFilter()
        setupDownloadButtons()

        if (selectedStartDate != null) {
            binding.labelStartDate.text = formatDate(selectedStartDate)
        }
        if (selectedEndDate != null) {
            binding.labelEndDate.text = formatDate(selectedEndDate)
        }

        if (savedInstanceState == null) {
            loadVisitsData()
        } else {
            visitsListViewModel.visitDTOs.value?.let { applyFilters(it) }
        }
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
        visitsAdapter = VisitsAdapter { visitData ->
            showVisitDetailsDialog(visitData)
        }
        binding.visitsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.visitsRecyclerView.adapter = visitsAdapter
    }

    private fun loadVisitsData() {
        when (visitsKey) {
            Constants.VISITS_OVERVIEW_SCHEDULED_VISITS_KEY -> {
                setDateLabelsAndLoadData { visitsListViewModel.getScheduledVisitsData() }
            }
            Constants.VISITS_OVERVIEW_HISTORICAL_VISITS_KEY -> {
                setDateLabelsAndLoadData { visitsListViewModel.getHistoricalVisitsData() }
            }
            Constants.VISITS_OVERVIEW_MISSED_VISITS_KEY -> {
                val yesterdayDateTime = DateTime.now() - 1.days
                val yesterday = DateTime(yesterdayDateTime.yearInt, yesterdayDateTime.month1, yesterdayDateTime.dayOfMonth)
                selectedStartDate = yesterday
                selectedEndDate = yesterday
                binding.labelStartDate.text = formatDate(selectedStartDate)
                binding.labelEndDate.text = formatDate(selectedEndDate)
                visitsListViewModel.getMissedVisitsData()
            }
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

    private fun setDateLabelsAndLoadData(loadData: () -> Unit) {
        if (selectedStartDate == null) {
            val now = DateTime.now()
            selectedStartDate = DateTime(now.yearInt, now.month1, now.dayOfMonth)
        }
        if (selectedEndDate == null) {
            val now = DateTime.now()
            selectedEndDate = DateTime(now.yearInt, now.month1, now.dayOfMonth)
        }
        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)
        loadData()
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

    private fun setupClinicFilter() {
        visitsListViewModel.loadAttachedClinics()
        visitsListViewModel.attachedClinics.observe(viewLifecycleOwner) { clinics ->
            if (clinics.isNullOrEmpty()) {
                binding.clinicFilterContainer.visibility = View.GONE
                return@observe
            }
            binding.clinicFilterContainer.visibility = View.VISIBLE
            val parentName = visitsListViewModel.parentSiteName.value
                ?: getString(R.string.filter_parent_facility)
            val options = listOf(getString(R.string.filter_all_clinics), parentName) + clinics
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, options)
            binding.dropdownClinicFilter.setAdapter(adapter)
            binding.dropdownClinicFilter.setText(getString(R.string.filter_all_clinics), false)
            binding.dropdownClinicFilter.setOnItemClickListener { _, _, position, _ ->
                selectedClinic = when (position) {
                    0 -> null
                    1 -> PARENT_CLINIC_FILTER
                    else -> clinics[position - 2]
                }
                applyFilters()
            }
        }
    }

    private fun setupDownloadButtons() {
        binding.btnDownloadExcel.setOnClickListener {
            exportToExcel(visitsAdapter.currentList)
        }

        binding.btnDownloadCsv.setOnClickListener {
            exportToCSV(visitsAdapter.currentList)
        }
    }

    private fun exportToExcel(visits: List<VisitDataDTO>) {
        val fileName = "${buildFileName()}.xls"
        val mimeType = "application/vnd.ms-excel"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet(visitsKey.replace(" ", "_"))

            val headerRow = sheet.createRow(0)
            headerRow.createCell(0).setCellValue(Constants.VISIT_DATE_FILE_COLUMN_HEADER)
            headerRow.createCell(1).setCellValue(Constants.CLIENT_ID_FILE_COLUMN_HEADER)
            headerRow.createCell(2).setCellValue(Constants.CLIENT_NAME_FILE_COLUMN_HEADER)
            headerRow.createCell(3).setCellValue(Constants.PHONE_NUMBER_FILE_COLUMN_HEADER)
            headerRow.createCell(4).setCellValue(Constants.CLIENT_MOTHER_NAME_FILE_HEADER)

            visits.forEachIndexed { index, visit ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(visit.formattedStartDateTime)
                row.createCell(1).setCellValue(visit.participant.participantId)
                row.createCell(2).setCellValue(visit.participant.fullName)
                row.createCell(3).setCellValue(visit.participant.phone)
                row.createCell(4).setCellValue(visit.participant.motherName)
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

    private fun exportToCSV(visits: List<VisitDataDTO>) {
        val fileName = "${buildFileName()}.csv"
        val mimeType = "text/csv"
        val titleRowColumns = "${Constants.VISIT_DATE_FILE_COLUMN_HEADER}, ${Constants.CLIENT_ID_FILE_COLUMN_HEADER}, ${Constants.CLIENT_NAME_FILE_COLUMN_HEADER}, ${Constants.CLIENT_MOTHER_NAME_FILE_HEADER}, ${Constants.PHONE_NUMBER_FILE_COLUMN_HEADER} \n"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                writer.write(titleRowColumns)
                visits.forEach { visit ->
                    writer.write("${visit.formattedStartDateTime}, ${visit.participant.participantId},${visit.participant.fullName},${visit.participant.motherName},${visit.participant.phone ?: ""} \n")
                }
            }
        }
    }

    private fun buildFileName(): String {
        return "${visitsKey.replace(" ", "_")}_${
            DateUtil.convertDateToString(
                dateNow(),
                DateFormat.FORMAT_DATE.toString()
            )
        }"
    }

    private fun applyFilters(
        visits: List<VisitDataDTO> = visitsListViewModel.visitDTOs.value ?: emptyList()
    ) {
        val searchText = binding.searchBox.text.toString().lowercase(Locale.getDefault())

        val filteredVisits = visits.filter { visit ->
            val visitDate = visit.startDatetime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val startDate = selectedStartDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()

            val endDate = selectedEndDate?.toDate()?.toInstant()
                ?.atZone(ZoneId.systemDefault())
                ?.toLocalDate()

            val dateMatches = (startDate == null || visitDate >= startDate) &&
                    (endDate == null || visitDate <= endDate)

            val textSearchMatches =
                visit.participant.participantId.lowercase(Locale.getDefault()).contains(searchText) ||
                        visit.participant.fullName.lowercase(Locale.getDefault()).contains(searchText) ||
                        visit.participant.motherName.lowercase(Locale.getDefault()).contains(searchText)

            val clinicMatches = when (selectedClinic) {
                null -> true
                PARENT_CLINIC_FILTER -> visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC].isNullOrBlank()
                else -> visit.attributes[Constants.ATTRIBUTE_VISIT_ATTACHED_CLINIC]
                    ?.equals(selectedClinic, ignoreCase = true) == true
            }

            dateMatches && textSearchMatches && clinicMatches
        }

        binding.totalVisitCount.text =
            getString(R.string.visits_overview_total_visit_count_label, filteredVisits.size)

        visitsAdapter.submitList(filteredVisits)
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

    private fun showVisitDetailsDialog(visitData: VisitDataDTO) {
        lifecycleScope.launch {
            val participant = visitData.participant
            val visitType = visitData.attributes[Constants.ATTRIBUTE_VISIT_TYPE_VXNAID] ?: ""

            val visitDetails = VisitDetailsDTO(
                formattedVisitDate = visitData.formattedStartDateTime,
                visitType = visitType,
                phoneNumber = participant.phone ?: "",
                clientID = participant.participantId,
                clientFullName = participant.fullName,
                clientMotherName = participant.motherName
            )

            val dialog = VisitDetailsDialog.newInstance(visitDetails, visitsKey)
            dialog.show(parentFragmentManager, "VisitDetailsDialog")
        }
    }
}
