package com.jnj.vaccinetracker.reportsoverview.hmis105.screens

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.domain.entities.Gender
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.databinding.FragmentHmis105ChildHealthReportBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.adapters.Hmis105ChildHealthAdapter
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ChildHealthObservationDTO
import com.jnj.vaccinetracker.reportsoverview.hmis105.dto.Hmis105ChildHealthReportDTO
import com.jnj.vaccinetracker.reportsoverview.hmis105.model.Hmis105ChildHealthViewModel
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateFormat
import com.soywiz.klock.jvm.toDate
import org.apache.poi.hssf.usermodel.HSSFWorkbook

@RequiresApi(Build.VERSION_CODES.Q)
class Hmis105ChildHealthFragment : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val TAG = "Hmis105ChildHealthFrag"
        private const val START_DATE_TAG = "startDateCHP"
        private const val END_DATE_TAG   = "endDateCHP"
        private const val PARENT_CLINIC_FILTER = "__PARENT__"

        private val ALL_DOSES = listOf(
            "CH01 Vitamin A (Dose 1)",
            "CH02 Vitamin A (Dose 2)",
            "CH03 Dewormed (Dose 1)",
            "CH04 Dewormed (Dose 2)"
        )
        private val ALL_LOCATIONS = listOf(
            Constants.VISIT_PLACE_STATIC,
            Constants.VISIT_PLACE_OUTREACH,
            Constants.VISIT_PLACE_SCHOOL
        )
    }

    private lateinit var binding: FragmentHmis105ChildHealthReportBinding
    private lateinit var adapter: Hmis105ChildHealthAdapter
    private val viewModel: Hmis105ChildHealthViewModel by viewModels { viewModelFactory }
    private var selectedClinic: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_hmis105_child_health_report, container, false
        )
        binding.lifecycleOwner = viewLifecycleOwner

        try {
            setupRecyclerView()
            if (savedInstanceState != null) viewModel.restoreInstanceState(savedInstanceState)
            if (viewModel.selectedStartDate.value == null && viewModel.selectedEndDate.value == null) {
                initializeDefaultDates()
            } else {
                updateDateLabels()
            }
            setupDateButtons()
            setupDownloadButton()
            setupClinicFilter()
        } catch (ex: Exception) {
            Log.e(TAG, "Setup error", ex)
            Toast.makeText(requireContext(), getString(R.string.hmis105_child_health_initialization_error), Toast.LENGTH_LONG).show()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as? AppCompatActivity)?.supportActionBar?.apply {
                title = getString(R.string.hmis105_child_health_report_title)
                setDisplayHomeAsUpEnabled(true)
                setHomeButtonEnabled(true)
            }
            viewModel.loadAttachedClinics()
            viewModel.getChildHealthData()
        } catch (ex: Exception) {
            Log.e(TAG, "onViewCreated error", ex)
        }
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.observationDTOs.observe(lifecycleOwner) {
            applyFilters()
        }
        viewModel.isLoading.observe(lifecycleOwner) { loading ->
            if (::binding.isInitialized) {
                binding.progressBar.visibility = if (loading == true) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { activity?.onBackPressedDispatcher?.onBackPressed(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.saveInstanceState(outState)
    }

    private fun setupRecyclerView() {
        adapter = Hmis105ChildHealthAdapter()
        binding.recyclerViewReport.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewReport.adapter = adapter
    }

    private fun initializeDefaultDates() {
        val today = DateTime.now()
        viewModel.selectedStartDate.value = DateTime(today.yearInt, today.month, 1)
        viewModel.selectedEndDate.value   = DateTime(today.yearInt, today.month1, today.dayOfMonth)
        updateDateLabels()
    }

    private fun updateDateLabels() {
        binding.labelStartDate.text = formatDate(viewModel.selectedStartDate.value)
        binding.labelEndDate.text   = formatDate(viewModel.selectedEndDate.value)
    }

    private fun setupDateButtons() {
        binding.btnStartDate.setOnClickListener { showDatePicker(true) }
        binding.btnEndDate.setOnClickListener   { showDatePicker(false) }
    }

    private fun setupDownloadButton() {
        binding.btnExportExcel.setOnClickListener { exportToExcel() }
    }

    private fun showDatePicker(isStart: Boolean) {
        ReportOverviewDatePickerDialog(
            selectedDate = if (isStart) viewModel.selectedStartDate.value else viewModel.selectedEndDate.value
        ).show(childFragmentManager, if (isStart) START_DATE_TAG else END_DATE_TAG)
    }

    override fun onDatePicked(date: DateTime?, tag: String?) {
        date?.let {
            if (tag == START_DATE_TAG) {
                viewModel.selectedStartDate.value = it
                binding.labelStartDate.text = formatDate(it)
            } else {
                viewModel.selectedEndDate.value = it
                binding.labelEndDate.text = formatDate(it)
            }
            applyFilters()
        }
    }

    private fun setupClinicFilter() {
        viewModel.attachedClinics.observe(viewLifecycleOwner) { clinics ->
            if (clinics.isNullOrEmpty()) {
                binding.clinicFilterContainer.visibility = View.GONE
                return@observe
            }
            binding.clinicFilterContainer.visibility = View.VISIBLE
            val parentName = viewModel.parentSiteName.value
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

    // ── Core logic ────────────────────────────────────────────────────────────

    private fun applyFilters(
        dtos: List<Hmis105ChildHealthObservationDTO> = viewModel.observationDTOs.value ?: emptyList()
    ) {
        val startDate = viewModel.selectedStartDate.value?.toDate()
        val endDate   = viewModel.selectedEndDate.value?.toDate()

        val filtered = dtos.filter { dto ->
            if (dto.administerDate.isEmpty()) return@filter false
            val adminDate = DateUtil.convertStringToDate(
                dto.administerDate, DateFormat.FORMAT_DATE.toString()
            ) ?: return@filter false
            val dateMatches = (startDate == null || adminDate >= startDate) &&
                              (endDate   == null || adminDate <= endDate)
            val clinicMatches = when (selectedClinic) {
                null -> true
                PARENT_CLINIC_FILTER -> dto.attachedClinic.isNullOrBlank()
                else -> dto.attachedClinic?.equals(selectedClinic, ignoreCase = true) == true
            }
            dateMatches && clinicMatches
        }

        adapter.submitList(aggregateToRows(filtered))
    }

    private fun aggregateToRows(
        dtos: List<Hmis105ChildHealthObservationDTO>
    ): List<Hmis105ChildHealthReportDTO> {
        val rowsMap = mutableMapOf<Pair<String, String>, Hmis105ChildHealthReportDTO>()
        for (dose in ALL_DOSES) {
            for (loc in ALL_LOCATIONS) {
                rowsMap[dose to loc] = Hmis105ChildHealthReportDTO(dose = dose, visitLocation = loc)
            }
        }

        for (dto in dtos) {
            val rowKey = dto.dose to dto.visitLocation
            val row = rowsMap[rowKey] ?: continue
            rowsMap[rowKey] = when (dto.gender) {
                Gender.MALE -> when (dto.ageGroup) {
                    Constants.GROUP_AGE_FIRST  -> row.copy(months6to11Male   = row.months6to11Male   + 1)
                    Constants.GROUP_AGE_SECOND -> row.copy(months12to59Male  = row.months12to59Male  + 1)
                    Constants.GROUP_AGE_THIRD  -> row.copy(years5to14Male    = row.years5to14Male    + 1)
                    else -> row
                }
                Gender.FEMALE -> when (dto.ageGroup) {
                    Constants.GROUP_AGE_FIRST  -> row.copy(months6to11Female  = row.months6to11Female  + 1)
                    Constants.GROUP_AGE_SECOND -> row.copy(months12to59Female = row.months12to59Female + 1)
                    Constants.GROUP_AGE_THIRD  -> row.copy(years5to14Female   = row.years5to14Female   + 1)
                    else -> row
                }
                else -> row
            }.let { updated ->
                updated.copy(
                    total = updated.months6to11Male + updated.months6to11Female +
                            updated.months12to59Male + updated.months12to59Female +
                            updated.years5to14Male   + updated.years5to14Female
                )
            }
        }

        val rows = mutableListOf<Hmis105ChildHealthReportDTO>()
        for (dose in ALL_DOSES) {
            for (loc in ALL_LOCATIONS) {
                rows.add(rowsMap[dose to loc]!!)
            }
        }

        rows.add(Hmis105ChildHealthReportDTO(
            dose              = "TOTAL",
            visitLocation     = "ALL",
            months6to11Male   = rows.sumOf { it.months6to11Male },
            months6to11Female = rows.sumOf { it.months6to11Female },
            months12to59Male  = rows.sumOf { it.months12to59Male },
            months12to59Female= rows.sumOf { it.months12to59Female },
            years5to14Male    = rows.sumOf { it.years5to14Male },
            years5to14Female  = rows.sumOf { it.years5to14Female },
            total             = rows.sumOf { it.total }
        ))

        return rows
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @SuppressLint("SimpleDateFormat")
    private fun exportToExcel() {
        val rows = aggregateToRows(
            (viewModel.observationDTOs.value ?: emptyList()).filter { dto ->
                if (dto.administerDate.isEmpty()) return@filter false
                val startDate = viewModel.selectedStartDate.value?.toDate()
                val endDate   = viewModel.selectedEndDate.value?.toDate()
                val adminDate = DateUtil.convertStringToDate(
                    dto.administerDate, DateFormat.FORMAT_DATE.toString()
                ) ?: return@filter false
                (startDate == null || adminDate >= startDate) &&
                (endDate   == null || adminDate <= endDate)
            }
        )

        if (rows.isEmpty() || rows.all { it.total == 0 && !it.isTotalRow }) {
            Toast.makeText(requireContext(), getString(R.string.no_data_to_export), Toast.LENGTH_SHORT).show()
            return
        }

        val title = getString(R.string.hmis105_child_health_report_title)
        FileUtil.exportToFile(requireContext(), "${title.replace(" ", "_")}_${DateUtil.convertDateToString(dateNow(), DateFormat.FORMAT_DATE.toString())}.xls", "application/vnd.ms-excel") { outputStream ->
            HSSFWorkbook().use { workbook ->
                val sheet = workbook.createSheet(title.replace(" ", "_"))

                val header = sheet.createRow(0)
                listOf("Dose", "Location", "6-11M Male", "6-11M Female", "12-59M Male", "12-59M Female", "5-14Y Male", "5-14Y Female", "Total")
                    .forEachIndexed { i, label -> header.createCell(i).setCellValue(label) }

                rows.forEachIndexed { index, row ->
                    val r = sheet.createRow(index + 1)
                    r.createCell(0).setCellValue(row.dose)
                    r.createCell(1).setCellValue(row.visitLocation)
                    r.createCell(2).setCellValue(row.months6to11Male.toDouble())
                    r.createCell(3).setCellValue(row.months6to11Female.toDouble())
                    r.createCell(4).setCellValue(row.months12to59Male.toDouble())
                    r.createCell(5).setCellValue(row.months12to59Female.toDouble())
                    r.createCell(6).setCellValue(row.years5to14Male.toDouble())
                    r.createCell(7).setCellValue(row.years5to14Female.toDouble())
                    r.createCell(8).setCellValue(row.total.toDouble())
                }

                for (i in 0..8) sheet.setColumnWidth(i, 4000)
                workbook.write(outputStream)
            }
        }
    }

    private fun formatDate(date: DateTime?): String =
        date?.let { DateUtil.convertDateToString(it.toDate(), DateFormat.FORMAT_DATE.toString()) } ?: "N/A"
}
