package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowIntroBinding
import com.jnj.vaccinetracker.databinding.ItemParticipantFlowItemBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel

class ParticipantFlowIntroFragment : BaseFragment() {

    private val viewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowIntroBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_intro, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.btnMotherName.setOnClickListener {
            navigateToFragment(ParticipantFlowMotherNameFragment())
        }

        binding.btnMobilePhoneNumber.setOnClickListener {
            navigateToFragment(ParticipantFlowPhoneNumberFragment())
        }

        binding.btnChildIdNumber.setOnClickListener {
            navigateToFragment(ParticipantFlowParticipantIdFragment())
        }

        // Add dynamic content
//        populateWorkflowSteps(inflater)

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    private fun populateWorkflowSteps(inflater: LayoutInflater) {
        var index = 1

        viewModel.workflowItems.forEach { step ->
            val view = DataBindingUtil.inflate<ItemParticipantFlowItemBinding>(
                inflater, R.layout.item_participant_flow_item, binding.authStepsContainer, true
            )

            when (step) {
                ParticipantFlowViewModel.WorkflowItem.ID_CARD -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_id_card)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_id_card)
                }
                ParticipantFlowViewModel.WorkflowItem.PHONE -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_phone)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_baseline_phone)
                }
                ParticipantFlowViewModel.WorkflowItem.MOTHER_NAME -> {
                    view.label = this.getString(R.string.participant_flow_mother_name_label)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_account)
                }
                ParticipantFlowViewModel.WorkflowItem.IRIS_SCAN -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_iris_scan)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.drawable.ic_eyeglasses_right)
                    view.imgId.updateLayoutParams {
                        width = 153.toPx()
                        height = 153.toPx()
                    }
                }
                ParticipantFlowViewModel.WorkflowItem.MATCHING -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_identify)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.updateLayoutParams {
                        width = 120.toPx()
                        height = 120.toPx()
                    }
                }
                ParticipantFlowViewModel.WorkflowItem.VISIT -> {
                    view.label = this.getString(R.string.match_or_register_patient_step_visit)
                    view.stepIndex = this.getString(R.string.match_or_register_patient_step_index, index.toString())
                    view.imgId.setImageResource(R.mipmap.ic_launcher_foreground)
                    view.imgId.updateLayoutParams {
                        width = 140.toPx()
                        height = 140.toPx()
                    }
                }
            }
            index++

            if (index <= viewModel.workflowItems.size) {
                inflater.inflate(R.layout.item_participant_flow_divider, binding.authStepsContainer, true)
            }
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun Int.toPx() = this * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
}
