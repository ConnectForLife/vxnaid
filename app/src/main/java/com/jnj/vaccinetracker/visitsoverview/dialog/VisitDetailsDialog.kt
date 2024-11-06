package com.jnj.vaccinetracker.visitsoverview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.databinding.DialogVisitDetailsBinding
import com.jnj.vaccinetracker.visitsoverview.model.VisitDetailsDTO

class VisitDetailsDialog : BaseDialogFragment() {

    private lateinit var binding: DialogVisitDetailsBinding
    private lateinit var visitDetails: VisitDetailsDTO
    private lateinit var visitKey: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_visit_details, container, false)

        arguments?.let {
            visitDetails = it.getParcelable(ARG_VISIT_DETAILS)!!
            visitKey = it.getString(ARG_VISIT_KEY)!!
        }

        setupLabels()
        binding.visitDetails = visitDetails
        binding.closeButton.setOnClickListener { dismissAllowingStateLoss() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tableRowViews = listOf(
            binding.tableRowVisitDate,
            binding.tableRowVaccines,
            binding.tableRowPhoneNumber,
            binding.tableRowClientId,
            binding.tableRowClientName
        )

        tableRowViews.forEachIndexed { index, tableRow ->
            val backgroundColor = if (index % 2 == 0) {
                ContextCompat.getColor(requireContext(), R.color.row_odd_background)
            } else {
                ContextCompat.getColor(requireContext(), R.color.row_even_background)
            }
            tableRow.setBackgroundColor(backgroundColor)
        }
    }

    private fun setupLabels() {
        binding.labelVisitDate.text = when (visitKey) {
            Constants.VISITS_OVERVIEW_SCHEDULED_VISITS_KEY -> getString(R.string.visits_overview_details_scheduled_visit_date_label)
            Constants.VISITS_OVERVIEW_HISTORICAL_VISITS_KEY -> getString(R.string.visits_overview_details_historical_visit_date_label)
            Constants.VISITS_OVERVIEW_MISSED_VISITS_KEY -> getString(R.string.visits_overview_details_missed_visit_date_label)
            else -> getString(R.string.visits_overview_details_visit_date_label)
        }
    }

    companion object {
        private const val ARG_VISIT_DETAILS = "visit_details"
        private const val ARG_VISIT_KEY = "visit_key"

        fun newInstance(visit: VisitDetailsDTO, visitKey: String): VisitDetailsDialog {
            val dialog = VisitDetailsDialog()
            val args = Bundle().apply {
                putParcelable(ARG_VISIT_DETAILS, visit)
                putString(ARG_VISIT_KEY, visitKey)
            }
            dialog.arguments = args
            return dialog
        }
    }
}