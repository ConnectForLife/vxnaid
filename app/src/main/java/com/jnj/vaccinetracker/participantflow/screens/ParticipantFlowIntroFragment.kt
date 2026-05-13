package com.jnj.vaccinetracker.participantflow.screens

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseActivity
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentParticipantFlowIntroBinding
import com.jnj.vaccinetracker.participantflow.ParticipantFlowViewModel


/**
 * @author maartenvangiel
 * @author tbuehler
 * @author druelens
 * @version 2
 */
class ParticipantFlowIntroFragment : BaseFragment() {

    private val viewModel: ParticipantFlowViewModel by activityViewModels { viewModelFactory }

    private lateinit var binding: FragmentParticipantFlowIntroBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_participant_flow_intro, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.btnMotherName.setOnClickListener {
            viewModel.currentScreen.value = ParticipantFlowViewModel.Screen.MOTHER_NAME
        }
        binding.btnMobilePhoneNumber.setOnClickListener {
            viewModel.currentScreen.value = ParticipantFlowViewModel.Screen.PHONE
        }
        binding.btnChildIdNumber.setOnClickListener {
            viewModel.currentScreen.value = ParticipantFlowViewModel.Screen.PARTICIPANT_ID
        }

        setHasOptionsMenu(true)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            viewModel.currentScreen.value = ParticipantFlowViewModel.Screen.ADD_OR_SEARCH_PARTICIPANT
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun Int.toPx() = this * resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
}
