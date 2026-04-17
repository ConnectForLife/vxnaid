package com.jnj.vaccinetracker.reportsoverview.hmis105.screens

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.dialogs.ReportOverviewDatePickerDialog
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.util.DateUtil
import com.jnj.vaccinetracker.common.util.FileUtil
import com.jnj.vaccinetracker.databinding.FragmentHmis105ReportBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.adapters.Hmis105Adapter
import com.jnj.vaccinetracker.reportsoverview.hmis105.model.Hmis105ViewModel
import com.soywiz.klock.DateTime
import com.soywiz.klock.DateFormat
import com.soywiz.klock.jvm.toDate
import org.apache.poi.hssf.usermodel.HSSFWorkbook

@RequiresApi(Build.VERSION_CODES.Q)
class Hmis105ReportFragment : BaseFragment(),
    ReportOverviewDatePickerDialog.VisitsOverviewDatePickerListener {

    companion object {
        private const val TAG = "Hmis105ReportFragment"
        private const val START_DATE_PICKER_DIALOG_TAG = "startDatePickerHmis105"
        private const val END_DATE_PICKER_DIALOG_TAG = "endDatePickerHmis105"
    }

     private lateinit var binding: FragmentHmis105ReportBinding
     private lateinit var adapter: Hmis105Adapter
     private val viewModel: Hmis105ViewModel by viewModels { viewModelFactory }

     private var selectedStartDate: DateTime? = null
     private var selectedEndDate: DateTime? = null

      override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
      ): View {
          Log.d(TAG, "onCreateView called")
          setHasOptionsMenu(true)
          binding = DataBindingUtil.inflate(inflater, R.layout.fragment_hmis105_report, container, false)
          binding.lifecycleOwner = viewLifecycleOwner

          try {
              setupRecyclerView()
              initializeDefaultDates()
              setupDateButtons()
              setupDownloadButton()
              Log.d(TAG, "Fragment setup completed successfully")
          } catch (ex: Exception) {
              Log.e(TAG, "Error during fragment setup: ${ex.message}", ex)
              Toast.makeText(requireContext(), "Error initializing report: ${ex.message}", Toast.LENGTH_LONG).show()
          }

          return binding.root
      }

      override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
          super.onViewCreated(view, savedInstanceState)
          Log.d(TAG, "onViewCreated called")

          try {
              (activity as? AppCompatActivity)?.supportActionBar?.apply {
                  title = getString(R.string.hmis105_report_title)
                  setDisplayHomeAsUpEnabled(true)
                  setHomeButtonEnabled(true)
              }

              // Load data after view is created
              loadReportData()
          } catch (ex: Exception) {
              Log.e(TAG, "Error in onViewCreated: ${ex.message}", ex)
              Toast.makeText(requireContext(), "Error: ${ex.message}", Toast.LENGTH_LONG).show()
          }
      }

       override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
           Log.d(TAG, "observeViewModel called")
           try {
               viewModel.reportDTOs.observe(lifecycleOwner) { data ->
                   Log.d(TAG, "Report data updated: ${data?.size ?: 0} rows")
                   if (::adapter.isInitialized) {
                       adapter.submitList(data)
                   } else {
                       Log.w(TAG, "Adapter not initialized when data arrived")
                   }
               }

               viewModel.isLoading.observe(lifecycleOwner) { isLoading ->
                   Log.d(TAG, "Loading state: $isLoading")
                   if (::binding.isInitialized) {
                       binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
                   }
               }
           } catch (ex: Exception) {
               Log.e(TAG, "Error in observeViewModel: ${ex.message}", ex)
           }
       }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                activity?.onBackPressedDispatcher?.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = Hmis105Adapter()
        binding.recyclerViewReport.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewReport.adapter = adapter
    }

    private fun initializeDefaultDates() {
        val today = DateTime.now()
        val monthStart = DateTime(today.yearInt, today.month, 1)
        selectedStartDate = monthStart
        selectedEndDate = today
        
        binding.labelStartDate.text = formatDate(selectedStartDate)
        binding.labelEndDate.text = formatDate(selectedEndDate)
    }

    private fun setupDateButtons() {
        binding.btnStartDate.setOnClickListener {
            showDatePickerDialog(true)
        }
        binding.btnEndDate.setOnClickListener {
            showDatePickerDialog(false)
        }
    }

    private fun setupDownloadButton() {
        binding.btnExportExcel.setOnClickListener {
            exportToExcel()
        }
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

    override fun onDatePicked(date: DateTime?, tag: String?) {
        date?.let {
            if (tag == START_DATE_PICKER_DIALOG_TAG) {
                selectedStartDate = it
                binding.labelStartDate.text = formatDate(selectedStartDate)
            } else if (tag == END_DATE_PICKER_DIALOG_TAG) {
                selectedEndDate = it
                binding.labelEndDate.text = formatDate(selectedEndDate)
            }
            if (selectedStartDate != null && selectedEndDate != null) {
                loadReportData()
            }
        }
    }

     private fun loadReportData() {
         Log.d(TAG, "loadReportData called")
         viewModel.getHmisMalaria105Data(selectedStartDate, selectedEndDate)
     }


    @SuppressLint("SimpleDateFormat")
    private fun exportToExcel() {
        val data = viewModel.reportDTOs.value ?: emptyList()
        if (data.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_data_to_export), Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "HMIS105_Report_${DateUtil.convertDateToString(dateNow(), DateFormat.FORMAT_DATE.toString())}.xls"
        val mimeType = "application/vnd.ms-excel"

        FileUtil.exportToFile(requireContext(), fileName, mimeType) { outputStream ->
            val workbook = HSSFWorkbook()
            val sheet = workbook.createSheet(getString(R.string.hmis105_report_title).replace(" ", "_"))

            // Create header row
            val headerRow = sheet.createRow(0)
            val headers = listOf(
                "Doses",
                "Under 1 - Static",
                "Under 1 - Outreach/School",
                "1-4 Years - Static",
                "1-4 Years - Outreach/School",
                "5-14 Years - Static",
                "5-14 Years - Outreach/School",
                "Total"
            )

            headers.forEachIndexed { index, header ->
                headerRow.createCell(index).setCellValue(header)
            }

            // Create data rows
            data.forEachIndexed { index, report ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(report.doses)
                row.createCell(1).setCellValue(report.under1Static.toDouble())
                row.createCell(2).setCellValue(report.under1Outreach.toDouble())
                row.createCell(3).setCellValue(report.age1to4Static.toDouble())
                row.createCell(4).setCellValue(report.age1to4Outreach.toDouble())
                row.createCell(5).setCellValue(report.age5to14Static.toDouble())
                row.createCell(6).setCellValue(report.age5to14Outreach.toDouble())
                row.createCell(7).setCellValue(report.total.toDouble())
            }

            // Auto-resize columns
            repeat(headers.size) { sheet.autoSizeColumn(it) }

            workbook.write(outputStream)
            workbook.close()

            Toast.makeText(requireContext(), getString(R.string.file_exported_successfully), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(date: DateTime?): String {
        return date?.let { DateUtil.convertDateToString(it.toDate(), DateFormat.FORMAT_DATE.toString()) } ?: "N/A"
    }
}

