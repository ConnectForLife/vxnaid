package com.idi.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.ui.BaseFragment
import com.idi.vaccinetracker.databinding.FragmentVisitCaptureDataBinding
import com.idi.vaccinetracker.splash.SplashActivity
import com.idi.vaccinetracker.visit.VisitViewModel
import com.idi.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.idi.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog

/**
 * @author maartenvangiel
 * @version 1
 */
@RequiresApi(Build.VERSION_CODES.O)
class VisitCaptureDataFragment :
    BaseFragment(),
    VisitRegisteredSuccessDialog.VisitRegisteredSuccessDialogListener,
    OtherSubstanceItemAdapter.AddSubstanceValueListener
{
    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentVisitCaptureDataBinding
    private lateinit var otherSubstancesAdapter: OtherSubstanceItemAdapter

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        viewModel.otherSubstancesData.observe(lifecycleOwner) { otherSubstances ->
            otherSubstancesAdapter.updateItemsList(otherSubstances)
        }
        viewModel.checkOtherSubstances.observe(lifecycleOwner) {
            if (viewModel.checkOtherSubstances.value == true) {
                viewModel.isAnyOtherSubstancesEmpty.value = checkIfAnyOtherDataEmpty()
                viewModel.checkOtherSubstances.value = false
            }
        }

        viewModel.selectedOtherSubstances.observe(lifecycleOwner) {value ->
            otherSubstancesAdapter.otherSubstanceValues = value
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_visit_capture_data, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setOtherSubstancesRecyclerView()
        return binding.root
    }

    private fun setOtherSubstancesRecyclerView() {
        otherSubstancesAdapter = OtherSubstanceItemAdapter(mutableListOf(), this, participant = viewModel.participant.value!!)
        binding.recyclerViewOtherSubstances.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewOtherSubstances.adapter = otherSubstancesAdapter
    }

    override fun addOtherSubstance(substanceName: String, value: String) {
        viewModel.addObsToOtherSubstancesObsMap(substanceName, value)
    }

    override fun onVisitRegisteredSuccessDialogClosed() {
        requireActivity().apply {
            startActivity(SplashActivity.create(this)) // Restart the participant flow
            finishAffinity()
        }
    }

    private fun checkIfAnyOtherDataEmpty(): Boolean {
        val errorList = otherSubstancesAdapter.checkIfAnyItemsEmpty(viewModel.selectedOtherSubstances.value, binding.recyclerViewOtherSubstances)
        return if (errorList.isEmpty()) {
            false
        } else {
            displayValidationErrorDialog(errorList)
            true
        }
    }

}