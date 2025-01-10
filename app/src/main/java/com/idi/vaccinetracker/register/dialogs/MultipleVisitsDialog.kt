package com.idi.vaccinetracker.register.dialogs

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.helpers.dpToPx
import com.idi.vaccinetracker.common.helpers.findParent
import com.idi.vaccinetracker.common.ui.BaseDialogFragment
import com.idi.vaccinetracker.common.util.DateUtil
import com.idi.vaccinetracker.databinding.DialogMultipleVisitsBinding
import com.idi.vaccinetracker.register.screens.RegisterParticipantHistoricalDataViewModel
import com.soywiz.klock.DateFormat

@RequiresApi(Build.VERSION_CODES.O)
class MultipleVisitsDialog : BaseDialogFragment() {
   private lateinit var binding: DialogMultipleVisitsBinding
   private val viewModel: RegisterParticipantHistoricalDataViewModel by activityViewModels { viewModelFactory }
   private lateinit var visitType: String

   companion object {
      private const val ARG_VISIT_TYPE = "visit_type"

      fun create(visitType: String): MultipleVisitsDialog {
         val dialog = MultipleVisitsDialog()
         val bundle = Bundle().apply {
            putString(ARG_VISIT_TYPE, visitType)
         }
         dialog.arguments = bundle
         return dialog
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      arguments?.getString(ARG_VISIT_TYPE)?.let {
         visitType = it
      }
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_multiple_visits, container, false)
      setupVisitButtons()
      binding.btnClose.setOnClickListener {
         dismissAllowingStateLoss()
      }
      return binding.root
   }

   private fun setupVisitButtons() {
      binding.linearLayoutMultipleVisits.removeAllViews()

      viewModel.groupedVisitsByType.value?.get(visitType)?.forEach { visit ->
         val button = Button(requireContext()).apply {
            text = DateUtil.convertDateToString(visit.startDate, DateFormat("yyyy-MM-dd HH:mm").toString())
            layoutParams = LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
               setMargins(8.dpToPx, 8.dpToPx, 8.dpToPx, 8.dpToPx)
            }
            setOnClickListener {
               findParent<MultipleVisitsListener>()?.onVisitPicked(visitType, visit.uuid)
               dismissAllowingStateLoss()
            }
         }
         binding.linearLayoutMultipleVisits.addView(button)
      }
   }

   interface MultipleVisitsListener {
      fun onVisitPicked(visitType: String, visitUuid: String)
   }
}
