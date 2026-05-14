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
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusSuccessBinding
import com.jnj.vaccinetracker.databinding.ListItemChildHealthPlusServiceBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ChildHealthPlusSuccessFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusSuccessBinding
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_child_health_plus_success,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        binding.rvServices.layoutManager = LinearLayoutManager(requireContext())

        viewModel.generatedChildId.observe(viewLifecycleOwner) { childId ->
            binding.tvChildId.text = childId.orEmpty()
        }

        viewModel.selectedServices.observe(viewLifecycleOwner) { services ->
            binding.rvServices.adapter = SuccessServicesAdapter(services.orEmpty(), dateFormat)
        }

        viewModel.childFirstName.observe(viewLifecycleOwner) {
            binding.tvName.text = "${viewModel.childFirstName.value.orEmpty()} ${viewModel.childLastName.value.orEmpty()}".trim()
        }
        viewModel.childLastName.observe(viewLifecycleOwner) {
            binding.tvName.text = "${viewModel.childFirstName.value.orEmpty()} ${viewModel.childLastName.value.orEmpty()}".trim()
        }
        viewModel.dateOfBirth.observe(viewLifecycleOwner) { dob ->
            binding.tvDob.text = dob?.let { dateFormat.format(it) } ?: ""
        }
        viewModel.gender.observe(viewLifecycleOwner) { gender ->
            binding.tvSex.text = when (gender) {
                "M" -> "Male"
                "F" -> "Female"
                else -> gender ?: ""
            }
        }

        binding.btnDone.setOnClickListener {
            viewModel.onSuccessDismissed()
        }

        return binding.root
    }

    inner class SuccessServicesAdapter(
        private var services: List<SelectedService>,
        private val dateFormat: SimpleDateFormat
    ) : RecyclerView.Adapter<SuccessServicesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ListItemChildHealthPlusServiceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(service: SelectedService) {
                binding.service = service
                binding.onDelete = null
                binding.executePendingBindings()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ListItemChildHealthPlusServiceBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(services[position])
        }

        override fun getItemCount() = services.size

        fun updateServices(newServices: List<SelectedService>) {
            services = newServices
            notifyDataSetChanged()
        }
    }
}
