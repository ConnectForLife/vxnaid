package com.jnj.vaccinetracker.common.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.common.adapters.StringItemAdapter
import com.jnj.vaccinetracker.databinding.DialogValidationErrorBinding

class ValidationErrorDialog: BaseDialogFragment() {
   private lateinit var binding: DialogValidationErrorBinding
   private val validationErrorList: List<String> by lazy {
      requireArguments().getStringArrayList(VALIDATION_ERROR_LIST)?.toList() ?: emptyList()
   }

   companion object {
      private const val VALIDATION_ERROR_LIST = "validationErrorList"

      fun create(validationErrorList: List<String>): ValidationErrorDialog {
         return ValidationErrorDialog().apply {
            arguments = bundleOf(VALIDATION_ERROR_LIST to ArrayList(validationErrorList))
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
   }

   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_validation_error, container, false)
      binding.executePendingBindings()

      setupRecyclerView()
      setupClickListeners()

      return binding.root
   }

   private fun setupClickListeners() {
      binding.btnOk.setOnClickListener {
         dismissAllowingStateLoss()
      }
   }

   private fun setupRecyclerView() {
      val adapter = StringItemAdapter(validationErrorList)
      binding.recyclerViewErrorList.layoutManager = LinearLayoutManager(requireContext())
      binding.recyclerViewErrorList.adapter = adapter
   }
}