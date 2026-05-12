package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusDoseSelectionBinding

/**
 * Fragment for selecting dose in Child Health+ workflow
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusDoseSelectionFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusDoseSelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_dose_selection,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Populate dose buttons
        viewModel.availableDoses.observe(viewLifecycleOwner) { doses ->
            binding.dosesList.removeAllViews()
            doses?.forEach { dose ->
                val button = Button(requireContext()).apply {
                    text = dose.label
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        viewModel.selectDose(dose)
                    }
                }
                binding.dosesList.addView(button)
            }
        }

        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        return binding.root
    }
}

