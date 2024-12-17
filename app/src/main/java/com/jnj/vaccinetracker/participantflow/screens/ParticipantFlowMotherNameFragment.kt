package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowMotherNameBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel

class ParticipantFlowMotherNameFragment : BaseFragment() {

    private val viewModel: ParticipantFlowMotherNameViewModel by viewModels { viewModelFactory }
    private val flowViewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowMotherNameBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_mother_name, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.editTextMotherName.doAfterTextChanged {
            viewModel.validateInput(it.toString())
        }

        binding.btnSkip.setOnClickListener {
            flowViewModel.confirmMotherName(null)
        }

        binding.btnSubmit.setOnClickListener {
            val motherName = if (viewModel.canSubmit.get()) binding.editTextMotherName.text.toString() else null
            flowViewModel.confirmMotherName(motherName)
        }

        return binding.root
    }
}