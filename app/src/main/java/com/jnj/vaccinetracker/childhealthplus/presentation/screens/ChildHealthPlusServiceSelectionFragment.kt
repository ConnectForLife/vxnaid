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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.childhealthplus.model.ChildHealthPlusViewModel
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusService
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.databinding.FragmentChildHealthPlusServiceSelectionBinding

/**
 * Fragment for selecting services in Child Health+ workflow
 */
@RequiresApi(Build.VERSION_CODES.O)
class ChildHealthPlusServiceSelectionFragment : BaseFragment() {

    private val viewModel: ChildHealthPlusViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: FragmentChildHealthPlusServiceSelectionBinding
    private var serviceAdapter: ServiceAdapter? = null

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

        // Setup services RecyclerView
        serviceAdapter = ServiceAdapter { service ->
            viewModel.addService(service)
        }

        binding.servicesContainer.removeAllViews()
        viewModel.availableServices.value?.forEach { service ->
            val button = Button(requireContext()).apply {
                text = service.displayName
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    viewModel.addService(service)
                }
            }
            binding.servicesContainer.addView(button)
        }

        // Buttons
        binding.btnBack.setOnClickListener {
            viewModel.goBack()
        }

        binding.btnConfirm.setOnClickListener {
            viewModel.proceedToConfirmation()
        }

        return binding.root
    }

    inner class ServiceAdapter(private val onServiceSelected: (ChildHealthPlusService) -> Unit) :
        RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            fun bind(service: ChildHealthPlusService) {
                (itemView as? Button)?.apply {
                    text = service.displayName
                    setOnClickListener { onServiceSelected(service) }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(Button(parent.context))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            viewModel.availableServices.value?.get(position)?.let { holder.bind(it) }
        }

        override fun getItemCount() = viewModel.availableServices.value?.size ?: 0
    }
}

