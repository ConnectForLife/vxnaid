package com.jnj.vaccinetracker.visit.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.models.NavigationDirection
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.entities.VisitDetail
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.findDosingVisit
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.common.ui.animateNavigationDirection
import com.jnj.vaccinetracker.databinding.DialogRescheduleVisitBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.visit.screens.ReferralFragment
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class RescheduleVisitDialog @Inject constructor() : BaseDialogFragment(), ScheduleVisitDatePickerDialog.OnDateSelectedListener {
   private lateinit var binding: DialogRescheduleVisitBinding
   private lateinit var rescheduleReasonEditText: EditText
   private lateinit var visitDateTextView: TextView
   private var visitDate: DateTime? = null
   private val participant: ParticipantSummaryUiModel? by lazy { requireArguments().getParcelable(PARTICIPANT) }
   private val isAfterContraindications: Boolean by lazy { requireArguments().getBoolean(IS_AFTER_CONTRAINDICATIONS) }
   @Inject lateinit var createVisitUseCase: CreateVisitUseCase
   @Inject lateinit var userRepository: UserRepository
   @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
   @Inject lateinit var configurationManager: ConfigurationManager
   @Inject lateinit var visitManager: VisitManager
   @Inject lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource
   private var currentVisit: VisitDetail? = null

   companion object {
      private const val PARTICIPANT = "participant"
      private const val CURRENT_VISIT_UUID = "currentVisitUuid"
      private const val IS_AFTER_CONTRAINDICATIONS = "isAfterContraIndications"
      const val TAG_DIALOG_RESCHEDULE_VISIT = "rescheduleVisitDialog"

      fun create(participant: ParticipantSummaryUiModel?, isAfterContraIndications: Boolean = false): RescheduleVisitDialog {
         return RescheduleVisitDialog().apply { arguments = bundleOf(PARTICIPANT to participant, IS_AFTER_CONTRAINDICATIONS to isAfterContraIndications) }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setStyle(STYLE_NO_TITLE, 0)
      isCancelable = false
      lifecycleScope.launch {
         val allVisits = visitManager.getVisitsForParticipant(participant!!.participantUuid)
         currentVisit = allVisits.findDosingVisit()
      }
   }

   @RequiresApi(Build.VERSION_CODES.O)
   override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
      binding = DataBindingUtil.inflate(inflater, R.layout.dialog_reschedule_visit, container, false)
      binding.executePendingBindings()

      visitDateTextView = binding.nextVisitDateValue
      rescheduleReasonEditText = binding.editTextRescheduleReason

      binding.nextVisitDatePickerButton.setOnClickListener {
         ScheduleVisitDatePickerDialog(visitDate, this).show(childFragmentManager, "scheduleVisitDatePickerDialog")
      }

      binding.btnSaveVisit.setOnClickListener {
         lifecycleScope.launch {
            try {
               validateDate()
               if (visitDate != null) {
                  if (!isAfterContraindications){
                     createVisitUseCase.createVisit(
                        buildNextVisitObject(
                           participant,
                           Date(visitDate!!.unixMillisLong)
                        )
                     )

                     if (rescheduleReasonEditText.text.toString().isNotEmpty()) {
                        val attributesToAdd =
                           mutableMapOf(Constants.RESCHEDULE_VISIT_REASON_ATTRIBUTE_TYPE_NAME to rescheduleReasonEditText.text.toString())
                        visitManager.updateVisitAttributes(
                           currentVisit,
                           participant!!.participantUuid,
                           attributesToAdd
                        )
                     }
                  }

                  dismissAllowingStateLoss()

                  findParent<RescheduleVisitListener>()?.onRescheduleVisitListener(visitDate!!, rescheduleReasonEditText.text.toString())
               }
            } catch (ex: Exception) {
               Log.e("RescheduleVisitDialog", "Something went wrong during rescheduling a visit", ex)
               com.jnj.vaccinetracker.common.dialogs.AlertDialog(requireContext()).showAlertDialog(getString(R.string.reschedule_visit_failed))
            }
         }
      }

      binding.btnFinish.setOnClickListener {
         dismissAllowingStateLoss()
      }
      return binding.root
   }

   override fun onDismiss(dialog: DialogInterface) {
      super.onDismiss(dialog)
      findParent<VisitRegisteredSuccessDialogListener>()?.onVisitRegisteredSuccessDialogClosed()
   }

   override fun onDateSelected(dateTime: DateTime) {
      visitDate = dateTime
      visitDateTextView.text = dateTime.format(DateFormat.FORMAT_DATE)
      binding.nextVisitDateValue.error = null
   }

   @RequiresApi(Build.VERSION_CODES.O)
   fun buildNextVisitObject(
      participant: ParticipantSummaryUiModel?,
      visitDate: Date
   ): CreateVisit {
      val operatorUuid = userRepository.getUser()?.uuid
         ?: throw OperatorUuidNotAvailableException("Operator uuid not available")
      val locationUuid = syncSettingsRepository.getSiteUuid()
         ?: throw NoSiteUuidAvailableException("Location not available")
      return CreateVisit(
         participantUuid = participant!!.participantUuid,
         visitType = Constants.VISIT_TYPE_DOSING,
         startDatetime = visitDate,
         locationUuid = locationUuid,
         attributes = mapOf(
            Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
            Constants.ATTRIBUTE_OPERATOR to operatorUuid,
         )
      )
   }

   private fun validateDate() {
      if (visitDate == null) {
         val dateValidationText = getString(R.string.dialog_missing_substances_empty_date_validation_message)
         binding.nextVisitDateValue.error = dateValidationText
         val hintTextColor = ContextCompat.getColor(requireContext(), R.color.errorLight)
         binding.nextVisitDateValue.setHintTextColor(hintTextColor)
      }
   }

   interface VisitRegisteredSuccessDialogListener {
      fun onVisitRegisteredSuccessDialogClosed()
   }

   interface RescheduleVisitListener {
      fun onRescheduleVisitListener(newVisitDate: DateTime, rescheduleReasonText: String)
   }
}