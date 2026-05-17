package com.jnj.vaccinetracker.childhealthplus.presentation.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusConfirmationBinding
import com.jnj.vaccinetracker.databinding.ListItemChildHealthPlusServiceBinding

/**
 * Fragment for reviewing and confirming Child Health+ submission
 */
class ChildHealthPlusConfirmationFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusConfirmationBinding
    private var servicesAdapter: ServicesAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_confirmation,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup services adapter
        servicesAdapter = ServicesAdapter(viewModel.selectedServices.value.orEmpty()) { serviceUuid ->
            viewModel.removeService(serviceUuid)
        }

        binding.servicesList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = servicesAdapter
        }

        // Observe service changes
        viewModel.selectedServices.observe(viewLifecycleOwner) { services ->
            servicesAdapter?.updateServices(services.orEmpty())
        }

        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        binding.btnSubmit.setOnClickListener {
            viewModel.submitChildHealthPlus()
        }

        return binding.root
    }

    inner class ServicesAdapter(
        private var services: List<SelectedService>,
        private val onDelete: (String) -> Unit
    ) : RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ListItemChildHealthPlusServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(service: SelectedService) {
                binding.service = service
                binding.onDelete = View.OnClickListener { onDelete(service.uuid) }
                binding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ListItemChildHealthPlusServiceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(services[position])
        }

        override fun getItemCount() = services.size

        fun updateServices(newServices: List<SelectedService>) {
            this.services = newServices
            notifyDataSetChanged()
        }
    }
}
