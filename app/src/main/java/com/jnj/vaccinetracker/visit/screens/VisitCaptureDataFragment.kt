package com.jnj.vaccinetracker.visit.screens

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
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentVisitCaptureDataBinding
import com.jnj.vaccinetracker.splash.SplashActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.adapters.OtherSubstanceItemAdapter
import com.jnj.vaccinetracker.visit.dialog.VisitRegisteredSuccessDialog

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.containerDosingVisit.requestFocus()
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