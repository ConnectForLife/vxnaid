package com.jnj.vaccinetracker.visitsoverview.screens

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
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
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
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
import java.io.OutputStream
import java.util.Locale
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
class VisitsListFragment(private val visitsKey: String) : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var binding: FragmentVisitsListBinding
    private lateinit var visitsAdapter: VisitsAdapter
    private val visitsListViewModel: VisitsListViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null

    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var visitManager: VisitManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visits_list, container, false)

        setupRecyclerView()
        loadVisitsData()
        setupObservers()
        setupFilterButtons()
        setupDownloadButtons()

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
        val titleRowColumns = "${Constants.VISIT_DATE_FILE_COLUMN_HEADER}, ${Constants.CLIENT_ID_FILE_COLUMN_HEADER}, ${Constants.CLIENT_NAME_FILE_COLUMN_HEADER}, ${Constants.PHONE_NUMBER_FILE_COLUMN_HEADER} \n"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            outputStream.bufferedWriter().use { writer ->
                writer.write(titleRowColumns)
                visits.forEach { visit ->
                    writer.write("${visit.formattedStartDateTime}, ${visit.participant.participantId},${visit.participant.motherName},${visit.participant.phone ?: ""} \n")
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
            val dateMatches =
                (selectedStartDate == null || visit.startDatetime >= selectedStartDate?.toDate()) &&
                        (selectedEndDate == null || visit.startDatetime <= selectedEndDate?.toDate())

            val textSearchMatches =
                visit.participant.participantId.lowercase(Locale.getDefault()).contains(searchText) ||
                        visit.participant.fullName.lowercase(Locale.getDefault()).contains(searchText) ||
                        visit.participant.motherName.lowercase(Locale.getDefault()).contains(searchText)
            dateMatches && textSearchMatches
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
            val visitType = findVisitType(visitData)

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

    private suspend fun findVisitType(visitData: VisitDataDTO): String {
        val participant = visitData.participant
        val participantVisits = visitManager.getVisitsForParticipant(participant.participantUuid)
        return SubstancesDataUtil.getVisitTypeForVisitWithGivenDate(
            participant.birthDate.birthDateToString(),
            visitData.formattedStartDateTime,
            participantVisits,
            configurationManager
        )
    }
}