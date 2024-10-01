package com.jnj.vaccinetracker.visit.screens

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.helpers.hideKeyboard
import com.jnj.vaccinetracker.common.ui.BaseFragment
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.FragmentContraindicationsBinding
import com.jnj.vaccinetracker.visit.VisitActivity
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.jnj.vaccinetracker.visit.dialog.DosingOutOfWindowDialog
import com.jnj.vaccinetracker.visit.dialog.RescheduleVisitDialog

@RequiresApi(Build.VERSION_CODES.O)
class ContraindicationsFragment : BaseFragment() {

   private lateinit var binding: FragmentContraindicationsBinding
   private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }

   override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
   ): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contraindications, container, false)
      binding.lifecycleOwner = viewLifecycleOwner

      setupUI()
      (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
      (activity as AppCompatActivity).supportActionBar?.setHomeButtonEnabled(true)
      return binding.root
   }

   private fun setupUI() {
      setupClickListeners()
      setupTouchListener()
   }

   private fun setupTouchListener() {
      binding.root.setOnClickListener {
         requireActivity().currentFocus?.hideKeyboard()
      }
   }

   private fun setupClickListeners() {
      binding.btnYes.setOnClickListener {
         showRescheduleVisitDialog()
      }

      binding.btnNo.setOnClickListener {
         closeFragment()
         if (!viewModel.dosingVisitIsInsideTimeWindow.value) {
            showOutsideTimeWindowConfirmationDialog()
         }
      }
   }
   private fun showRescheduleVisitDialog() {
      RescheduleVisitDialog.create(participant = viewModel.participant.value, isAfterContraIndications = true)
         .show(parentFragmentManager, RescheduleVisitDialog.TAG_DIALOG_RESCHEDULE_VISIT)
   }


   private fun navigateToReferralFragment() {
      val dosingVisit = viewModel.dosingVisit.value
      if (dosingVisit == null || viewModel.participant.value == null) {
         return
      }

      val referralFragment = ReferralFragment().apply {
         arguments = Bundle().apply {
            putString(VisitActivity.CURRENT_VISIT_UUID, dosingVisit.uuid)
            putString(VisitActivity.PARTICIPANT_UUID, viewModel.participant.value!!.participantUuid)
            putBoolean(VisitActivity.IS_AFTER_VISIT, false)
         }
      }

      requireActivity().supportFragmentManager.beginTransaction()
         .animateNavigationDirection(NavigationDirection.FORWARD)
         .replace(R.id.fragment_container, referralFragment)
         .addToBackStack(null)
         .commit()
   }

   private fun closeFragment() {
      requireActivity().supportFragmentManager.beginTransaction()
         .animateNavigationDirection(NavigationDirection.FORWARD)
         .remove(this)
         .commit()
   }

   private fun showOutsideTimeWindowConfirmationDialog() {
      DosingOutOfWindowDialog().show(requireActivity().supportFragmentManager,
         VisitActivity.TAG_DIALOG_DOSING_OUT_OF_WINDOW
      )
   }
}

