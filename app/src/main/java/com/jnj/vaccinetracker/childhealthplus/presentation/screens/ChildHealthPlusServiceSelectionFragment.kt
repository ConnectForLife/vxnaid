package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusServiceSelectionBinding
import com.jnj.vaccinetracker.databinding.ListItemChildHealthPlusServiceBinding

class ChildHealthPlusServiceSelectionFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusServiceSelectionBinding
    private val selectedServicesAdapter = SelectedServicesAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_service_selection,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupServiceButtons()
        setupSelectedServicesList()

        binding.btnBack.setOnClickListener { viewModel.goBack() }
        binding.btnConfirm.setOnClickListener { viewModel.proceedToConfirmation() }
        // "Add another service" button is redundant — the service buttons above already do this
        binding.btnAddService.visibility = View.GONE

        return binding.root
    }

    private fun setupServiceButtons() {
        binding.servicesContainer.removeAllViews()
        viewModel.availableServices.value?.forEach { service ->
            val button = Button(requireContext()).apply {
                text = service.displayName
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener { onServiceSelected(service) }
            }
            binding.servicesContainer.addView(button)
        }
    }

    private fun onServiceSelected(service: ChildHealthPlusService) {
        if (service == ChildHealthPlusService.TETANUS && viewModel.gender.value == "F") {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.child_health_plus_pregnant_woman_dialog_message)
                .setPositiveButton(R.string.general_label_yes) { _, _ ->
                    viewModel.isPregnantWoman.set(true)
                    viewModel.addService(service)
                }
                .setNegativeButton(R.string.general_label_no) { _, _ ->
                    viewModel.isPregnantWoman.set(false)
                    viewModel.addService(service)
                }
                .show()
        } else {
            viewModel.addService(service)
        }
    }

    private fun setupSelectedServicesList() {
        binding.selectedServicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedServicesAdapter
        }

        viewModel.selectedServices.observe(viewLifecycleOwner) { services ->
            selectedServicesAdapter.submitList(services.orEmpty())
            binding.selectedServicesTitle.visibility =
                if (services.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    inner class SelectedServicesAdapter :
        RecyclerView.Adapter<SelectedServicesAdapter.ViewHolder>() {

        private var items: List<SelectedService> = emptyList()

        fun submitList(list: List<SelectedService>) {
            items = list
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: ListItemChildHealthPlusServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(service: SelectedService) {
                binding.service = service
                binding.btnDelete.setOnClickListener { viewModel.removeService(service.uuid) }
                binding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(
                ListItemChildHealthPlusServiceBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            holder.bind(items[position])

        override fun getItemCount() = items.size
    }
}
