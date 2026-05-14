package com.jnj.vaccinetracker.reportsoverview.childrenoverview.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentReportsOverviewBinding
import com.jnj.vaccinetracker.reportsoverview.hmis105.activity.Hmis105ChildHealthFlowActivity
import com.jnj.vaccinetracker.reportsoverview.hmis105.activity.Hmis105FlowActivity
import com.jnj.vaccinetracker.reportsoverview.vaccinesoverview.screens.VaccinesOverviewFragment

class ReportsOverviewFragment : BaseFragment() {

    private lateinit var binding: FragmentReportsOverviewBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        setHasOptionsMenu(true)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reports_overview, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.btnVaccinesOverviewReport.setOnClickListener {
            navigateToVaccinesReport()
        }

        binding.btnRegisteredChildrenReport.setOnClickListener {
            navigateToRegisteredChildrenReport()
        }

        binding.btnHmis105Report.setOnClickListener {
            navigateToHmis105Report()
        }

        binding.btnHmis105ChildHealthReport.setOnClickListener {
            navigateToHmis105ChildHealthReport()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.reports_overview_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToVaccinesReport() {
        val fragment = VaccinesOverviewFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToRegisteredChildrenReport() {
        val fragment = RegisteredParticipantsFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToHmis105Report() {
        startActivity(Hmis105FlowActivity.create(requireContext()))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToHmis105ChildHealthReport() {
        startActivity(Hmis105ChildHealthFlowActivity.create(requireContext()))
    }
}