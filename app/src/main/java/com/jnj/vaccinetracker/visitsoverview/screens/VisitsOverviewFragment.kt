package com.jnj.vaccinetracker.visitsoverview.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentVisitsOverviewBinding

class VisitsOverviewFragment : BaseFragment() {

    private lateinit var binding: FragmentVisitsOverviewBinding

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visits_overview, container, false)
        binding.lifecycleOwner = viewLifecycleOwner

        binding.btnScheduledVisits.setOnClickListener {
            navigateToVisitsList(Constants.VISITS_OVERVIEW_SCHEDULED_VISITS_KEY)
        }

        binding.btnHistoricalVisits.setOnClickListener {
            navigateToVisitsList(Constants.VISITS_OVERVIEW_HISTORICAL_VISITS_KEY_OFFLINE)
        }

        binding.btnMissedVisits.setOnClickListener {
            navigateToVisitsList(Constants.VISITS_OVERVIEW_MISSED_VISITS_KEY)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).supportActionBar?.apply {
            title = getString(R.string.visits_overview_title)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun navigateToVisitsList(visitsKey: String) {
        val fragment = VisitsListFragment(visitsKey)
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}