package com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.screens

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.databinding.FragmentVaccinesOverviewBinding
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.adapters.VaccineDropdownItemAdapter
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.adapters.VaccinesOverviewAdapter
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.dto.VaccineObservationDTO
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.dto.VaccinesOverviewDTO
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.model.VaccinesOverviewViewModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import com.soywiz.klock.jvm.toDate
import kotlinx.coroutines.launch
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.util.CellRangeAddress
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.Q)
class VaccinesOverviewFragment : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePicker"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePicker"
    }

    private lateinit var binding: FragmentVaccinesOverviewBinding
    private lateinit var vaccinesOverviewAdapter: VaccinesOverviewAdapter
    private val selectedVaccineConceptNames = mutableSetOf<String>()
    private val vaccinesOverviewViewModel: VaccinesOverviewViewModel by viewModels { viewModelFactory }
    private var selectedStartDate: DateTime? = null
    private var selectedEndDate: DateTime? = null
    private var selectedLocation: String? = Constants.ALL_STRING
    private var selectedAgeGroup: String? = Constants.ALL_STRING

    @Inject
    lateinit var configurationManager: ConfigurationManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_vaccines_overview, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        selectedLocation = Constants.ALL_STRING
        selectedAgeGroup = Constants.ALL_STRING

        setupRecyclerView()
        initializeDefaultDates()
        setupFilterFields()
        loadVaccinesData()
        setupObservers()
        setupDownloadButtons()

        vaccinesOverviewViewModel.vaccineDTOs.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                applyFilters(data)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.vaccines_administered_title)
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
        vaccinesOverviewAdapter = VaccinesOverviewAdapter()

        binding.vaccinesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.vaccinesRecyclerView.adapter = vaccinesOverviewAdapter
    }

    private fun setupFilterFields() {
        setupDateButtons()
        setupVaccinesDropdown()
        setupLocationsDropdown()
        setupAgeGroupDropdown()
    }

    private fun setupDateButtons() {
        binding.btnStartDate.setOnClickListener {
            showDatePickerDialog(true)
        }

        binding.btnEndDate.setOnClickListener {
            showDatePickerDialog(false)
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
        } ?: getString(R.string.visits_overview_not_set_label)
    }

    private fun initializeDefaultDates() {
        val today = DateTime.now()
        val firstDayOfMonth = today.startOfMonth

        selectedStartDate = firstDayOfMonth
        selectedEndDate = today

        binding.labelStartDate.text = formatDate(firstDayOfMonth)
        binding.labelEndDate.text = formatDate(today)

        applyFilters()
    }

    @SuppressLint("SetTextI18n")
    private fun setupVaccinesDropdown() {
        lifecycleScope.launch {
            vaccinesOverviewViewModel.substancesConfig.value =
                vaccinesOverviewViewModel.getSubstancesConfig()

            val vaccineLabels =
                listOf(Constants.ALL_STRING) + vaccinesOverviewViewModel.substancesConfig.value!!
                    .filter { it.category == Constants.VACCINES_CATEGORY_NAME }
                    .map { it.label }
                    .sorted()

            val vaccineConceptNames =
                listOf(Constants.EMPTY_STRING_VALUE) + vaccinesOverviewViewModel.substancesConfig.value!!
                    .filter { it.category == Constants.VACCINES_CATEGORY_NAME }
                    .map { it.conceptName }
                    .sorted()

            val vaccinesAdapter = VaccineDropdownItemAdapter(
                requireContext(),
                vaccineLabels,
                vaccineConceptNames,
                selectedVaccineConceptNames
            )

            if (selectedVaccineConceptNames.isEmpty()) {
                selectedVaccineConceptNames.addAll(vaccineConceptNames)
                vaccinesAdapter.isSelectAllChecked = true
            }

            binding.autoCompleteVaccines.setAdapter(vaccinesAdapter)
            binding.autoCompleteVaccines.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

            binding.autoCompleteVaccines.setOnItemClickListener { parent, view, position, id ->
                if (position == 0) {
                    if (selectedVaccineConceptNames.size == vaccineConceptNames.size) {
                        selectedVaccineConceptNames.clear()
                    } else {
                        selectedVaccineConceptNames.addAll(vaccineConceptNames)
                    }
                    vaccinesAdapter.toggleSelectAll()
                } else {
                    val selectedConceptName = vaccineConceptNames[position]
                    if (selectedVaccineConceptNames.contains(selectedConceptName)) {
                        selectedVaccineConceptNames.remove(selectedConceptName)
                    } else {
                        selectedVaccineConceptNames.add(selectedConceptName)
                    }
                }

                vaccinesAdapter.isSelectAllChecked =
                    selectedVaccineConceptNames.size == vaccineConceptNames.size

                val selectedText = if (vaccinesAdapter.isSelectAllChecked) {
                    Constants.ALL_STRING
                } else {
                    selectedVaccineConceptNames.filter { it.isNotEmpty() }
                        .joinToString(", ") { findVaccineLabel(it) }
                }
                binding.labelSelectedVaccines.text = "${getString(R.string.selected_vaccines_label)} $selectedText"

                binding.autoCompleteVaccines.setText(selectedText, false)
                vaccinesAdapter.notifyDataSetChanged()
                applyFilters()
                binding.autoCompleteVaccines.post { binding.autoCompleteVaccines.showDropDown() }
            }

            binding.autoCompleteVaccines.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    binding.autoCompleteVaccines.ellipsize = TextUtils.TruncateAt.END
                }
            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupLocationsDropdown() {
        val locations = listOf(
            Constants.ALL_STRING,
            Constants.VISIT_PLACE_STATIC,
            Constants.VISIT_PLACE_OUTREACH,
            Constants.VISIT_PLACE_SCHOOL
        )

        val locationsAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, locations)
        binding.autoCompleteLocations.setAdapter(locationsAdapter)
        binding.autoCompleteLocations.setText(Constants.ALL_STRING, false)

        binding.autoCompleteLocations.setOnItemClickListener { _, _, position, _ ->
            selectedLocation = locations[position]
            applyFilters()
        }
    }

    private fun setupAgeGroupDropdown() {
        val ageGroups = listOf(
            Constants.ALL_STRING,
            Constants.GROUP_AGE_FIRST,
            Constants.GROUP_AGE_SECOND,
            Constants.GROUP_AGE_THIRD
        )
        val groupsAgeAdapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, ageGroups)
        binding.autoCompleteAgeGroups.setAdapter(groupsAgeAdapter)

        binding.autoCompleteAgeGroups.setText(Constants.ALL_STRING, false)

        binding.autoCompleteAgeGroups.setOnItemClickListener { _, _, position, _ ->
            selectedAgeGroup = ageGroups[position]
            applyFilters()
        }
    }

    private fun loadVaccinesData() {
        vaccinesOverviewViewModel.getVaccinesData()
    }

    private fun setupObservers() {
        vaccinesOverviewViewModel.vaccineDTOs.observe(viewLifecycleOwner) { vaccinesDTOs ->
            vaccinesDTOs?.let {
                applyFilters(it)
            }
        }

        vaccinesOverviewViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        }
    }

    private fun setupDownloadButtons() {
        binding.btnDownloadExcel.setOnClickListener {
            exportToExcel(
                vaccinesOverviewViewModel.vaccineDTOs.value!!,
                selectedStartDate,
                selectedEndDate
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun applyFilters(
        vaccinesData: List<VaccineObservationDTO> = vaccinesOverviewViewModel.vaccineDTOs.value ?: emptyList()
    ) {
        val isStatic = { loc: String? -> loc.equals(Constants.VISIT_PLACE_STATIC, ignoreCase = true) }
        val isOutreach = { loc: String? -> loc?.equals(Constants.VISIT_PLACE_OUTREACH, ignoreCase = true) ?: false }
        val isSchool = { loc: String? -> loc.equals(Constants.VISIT_PLACE_SCHOOL, ignoreCase = true) }

        // Filter data based on selected criteria
        val filteredData = vaccinesData.filter { observation ->
            val dateMatches = if (observation.administerDate.isNotEmpty()) {
                val observationDate = DateUtil.convertStringToDate(
                    observation.administerDate,
                    DateFormat.FORMAT_DATE.toString()
                ) ?: return@filter false

                (selectedStartDate == null || observationDate >= selectedStartDate!!.toDate()) &&
                        (selectedEndDate == null || observationDate <= selectedEndDate!!.toDate())
            } else false

            val vaccineMatches = when {
                selectedVaccineConceptNames.isEmpty() -> true
                selectedVaccineConceptNames.contains(Constants.ALL_STRING) -> true
                else -> selectedVaccineConceptNames.contains(observation.vaccineName)
            }

            val locationMatches = when (selectedLocation) {
                Constants.ALL_STRING -> true
                Constants.VISIT_PLACE_STATIC -> isStatic(observation.visitLocation)
                Constants.VISIT_PLACE_OUTREACH -> isOutreach(observation.visitLocation)
                Constants.VISIT_PLACE_SCHOOL -> isSchool(observation.visitLocation)
                else -> false
            }

            val ageGroupMatches = selectedAgeGroup == Constants.ALL_STRING ||
                    observation.ageGroup == selectedAgeGroup
            dateMatches && vaccineMatches && locationMatches && ageGroupMatches
        }.distinctBy { it.vaccineName + it.administerDate + it.visitLocation } // Remove duplicates

        // Count entries for each location type
        val staticCount = filteredData.count { isStatic(it.visitLocation) }
        val outreachCount = filteredData.count { isOutreach(it.visitLocation) }
        val schoolCount = filteredData.count { isSchool(it.visitLocation) }

        // Debugging: Log details of filtered data
        filteredData.forEach { observation ->
            Log.d("FilterDebugDetails", "Location: ${observation.visitLocation}, Vaccine: ${observation.vaccineName}, Date: ${observation.administerDate}")
        }
        Log.d("FilterDebug", "Static: $staticCount, Outreach: $outreachCount, School: $schoolCount, Total: ${filteredData.size}")

        // Update UI with filtered data
        val groupedVaccinesData = groupAndCount(filteredData)
        vaccinesOverviewAdapter.submitList(groupedVaccinesData)

        val displayTotal = filteredData.size
        binding.headerVaccineTotal.text = "Total: $displayTotal"

        // Show appropriate messages based on filtered results
        when {
            selectedLocation == Constants.VISIT_PLACE_OUTREACH && outreachCount == 0 -> {
                Toast.makeText(
                    context,
                    if (vaccinesData.none { isOutreach(it.visitLocation) })
                        getString(R.string.no_outreach_vaccines_in_system)
                    else getString(R.string.no_outreach_vaccines_match_filters),
                    Toast.LENGTH_LONG
                ).show()
            }
            selectedLocation == Constants.VISIT_PLACE_SCHOOL && schoolCount == 0 -> {
                Toast.makeText(
                    context,
                    if (vaccinesData.none { isSchool(it.visitLocation) })
                        getString(R.string.no_school_vaccines_in_system)
                    else getString(R.string.no_school_vaccines_match_filters),
                    Toast.LENGTH_LONG
                ).show()
            }
            filteredData.isEmpty() -> {
                Toast.makeText(context, getString(R.string.no_vaccines_match_filters), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun groupAndCount(vaccineObservations: List<VaccineObservationDTO>): List<VaccinesOverviewDTO> {
        return vaccineObservations
            .groupBy { it.vaccineName }
            .map { (vaccineConceptName, observations) ->
                val vaccineLabel = findVaccineLabel(vaccineConceptName)
                VaccinesOverviewDTO(vaccineLabel, observations.size)
            }
            .sortedBy { it.label }
    }

    private fun findVaccineLabel(vaccineConceptName: String): String {
        return vaccinesOverviewViewModel.substancesConfig.value?.find { it.conceptName == vaccineConceptName }?.label
            ?: vaccineConceptName
    }

    private fun exportToExcel(
        vaccinesData: List<VaccineObservationDTO>,
        startDate: DateTime?,
        endDate: DateTime?
    ) {
        val fileName = "${buildFileName()}.xls"
        val mimeType = "application/vnd.ms-excel"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            val workbook = HSSFWorkbook()
            val sheet =
                workbook.createSheet(getString(R.string.vaccines_overview_title).replace(" ", "_"))

            val dateRangeRow = sheet.createRow(0)
            val dateRangeText = if (startDate != null && endDate != null) {
                getString(
                    R.string.vaccines_excel_report_date_range_label_set, startDate.toString(
                        DateFormat.FORMAT_DATE.toString()
                    ), endDate.toString(DateFormat.FORMAT_DATE.toString())
                )
            } else {
                getString(R.string.vaccines_excel_report_date_range_label_not_set)
            }
            dateRangeRow.createCell(0).setCellValue(dateRangeText)
            sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))

            sheet.createRow(1)

            val titleRow = sheet.createRow(2)
            titleRow.createCell(0).setCellValue(getString(R.string.vaccine_name_label))
            sheet.addMergedRegion(CellRangeAddress(2, 3, 0, 0))

            val ageGroups =
                listOf(Constants.GROUP_AGE_FIRST, Constants.GROUP_AGE_SECOND, Constants.GROUP_AGE_THIRD)
            val locations = listOf(
                Constants.VISIT_PLACE_STATIC,
                Constants.VISIT_PLACE_OUTREACH,
                Constants.VISIT_PLACE_SCHOOL
            )

            val ageGroupRow = sheet.getRow(2) ?: sheet.createRow(2)
            val locationsRow = sheet.createRow(3)

            var currentColumn = 1
            ageGroups.forEach { ageGroup ->
                sheet.addMergedRegion(CellRangeAddress(2, 2, currentColumn, currentColumn + 2))
                ageGroupRow.createCell(currentColumn).setCellValue(ageGroup)

                locations.forEachIndexed { index, location ->
                    locationsRow.createCell(currentColumn + index).setCellValue(location)
                }
                currentColumn += locations.size
            }

            ageGroupRow.createCell(currentColumn).setCellValue(getString(R.string.vaccine_total_label))
            sheet.addMergedRegion(CellRangeAddress(2, 3, currentColumn, currentColumn))

            val columnSums = MutableList(currentColumn + 1) { 0 }

            val filteredAndGroupedData = vaccinesData
                .filter { observation ->
                    val administerDate = DateUtil.convertStringToDate(
                        observation.administerDate,
                        DateFormat.FORMAT_DATE.toString()
                    )
                    val isAdministerDateInRange =
                        (startDate == null || administerDate!! >= startDate.toDate()) &&
                                (endDate == null || administerDate!! <= endDate.toDate())
                    val isVaccineSelected =
                        selectedVaccineConceptNames.isEmpty() || selectedVaccineConceptNames.contains(
                            observation.vaccineName
                        )
                    isAdministerDateInRange && isVaccineSelected
                }
                .groupBy { it.vaccineName }

            var currentRow = 4
            filteredAndGroupedData.forEach { (vaccineName, observations) ->
                val row = sheet.createRow(currentRow++)
                row.createCell(0).setCellValue(findVaccineLabel(vaccineName))

                val ageLocationCounts = mutableMapOf<String, Int>()

                ageGroups.forEach { ageGroup ->
                    locations.forEach { location ->
                        ageLocationCounts["$ageGroup-$location"] = 0
                    }
                }

                observations.forEach { observation ->
                    val ageGroup = when (observation.ageGroup) {
                        Constants.GROUP_AGE_FIRST -> Constants.GROUP_AGE_FIRST
                        Constants.GROUP_AGE_SECOND -> Constants.GROUP_AGE_SECOND
                        Constants.GROUP_AGE_THIRD -> Constants.GROUP_AGE_THIRD
                        else -> null
                    }
                    if (ageGroup != null && observation.visitLocation.isNotEmpty()) {
                        val key = "$ageGroup-${observation.visitLocation}"
                        ageLocationCounts[key] = ageLocationCounts.getOrDefault(key, 0) + 1
                    }
                }

                var cellIndex = 1
                var rowTotal = 0
                ageGroups.forEach { ageGroup ->
                    locations.forEach { location ->
                        val count = ageLocationCounts["$ageGroup-$location"]?.toDouble() ?: 0.0
                        row.createCell(cellIndex).setCellValue(count)

                        columnSums[cellIndex] += count.toInt()
                        rowTotal += count.toInt()
                        cellIndex++
                    }
                }

                row.createCell(cellIndex).setCellValue(rowTotal.toDouble())
                columnSums[cellIndex] += rowTotal
            }

            val totalsRow = sheet.createRow(currentRow)
            totalsRow.createCell(0).setCellValue(getString(R.string.vaccine_total_label))
            for (colIndex in 1..columnSums.lastIndex) {
                totalsRow.createCell(colIndex).setCellValue(columnSums[colIndex].toDouble())
            }

            sheet.setColumnWidth(0, 5000)
            for (i in 1..(ageGroups.size * locations.size)) {
                sheet.setColumnWidth(i, 4000)
            }
            sheet.setColumnWidth(currentColumn, 4000)

            workbook.write(outputStream)
            workbook.close()
        }
    }

    private fun buildFileName(): String {
        return "${getString(R.string.vaccines_overview_title).replace(" ", "_")}_${
            DateUtil.convertDateToString(
                dateNow(),
                DateFormat.FORMAT_DATE.toString()
            )
        }"
    }
}