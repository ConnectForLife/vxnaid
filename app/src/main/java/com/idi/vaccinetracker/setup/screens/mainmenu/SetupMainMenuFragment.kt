package com.idi.vaccinetracker.setup.screens.mainmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.ui.BaseActivity
import com.idi.vaccinetracker.common.ui.BaseFragment
import com.idi.vaccinetracker.databinding.FragmentSetupMainMenuBinding
import com.idi.vaccinetracker.login.LoginActivity
import com.idi.vaccinetracker.setup.SetupFlowViewModel
import com.idi.vaccinetracker.setup.adapters.SetupMenuItemAdapter
import com.idi.vaccinetracker.setup.models.SetupMenuItem
import kotlinx.coroutines.flow.onEach

class SetupMainMenuFragment : BaseFragment() {

    private val viewModel: SetupMainMenuViewModel by viewModels { viewModelFactory }
    private val flowViewModel: SetupFlowViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentSetupMainMenuBinding

    private val adapter get() = binding.menuRecyclerView.adapter as SetupMenuItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_setup_main_menu, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner


        // Show back-button again
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finishButton.setOnClickListener {
            viewModel.onFinishClick()
        }
        binding.menuRecyclerView.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = SetupMenuItemAdapter(viewModel::onMenuItemClick)
        }
    }

    private fun openMenuItem(item: SetupMenuItem) {
        flowViewModel.openMenuItem(item.screen)
    }

    private fun onSetupCompleted() {
        startActivity(LoginActivity.create(requireContext()))
        (requireActivity() as BaseActivity).setForwardAnimation()
    }

    override fun observeViewModel(lifecycleOwner: LifecycleOwner) {
        super.observeViewModel(lifecycleOwner)
        viewModel.setupCompletedEvent.asFlow().onEach {
            onSetupCompleted()
        }.launchIn(lifecycleOwner)
        viewModel.menuItems.onEach { items ->
            adapter.items = items
        }.launchIn(lifecycleOwner)
        viewModel.openMenuItemEvent.asFlow().onEach {
            openMenuItem(it)
        }.launchIn(lifecycleOwner)
    }
}