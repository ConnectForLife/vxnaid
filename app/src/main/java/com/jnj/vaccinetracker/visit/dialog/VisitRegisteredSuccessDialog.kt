package com.jnj.vaccinetracker.visit.dialog

import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.jnj.vaccinetracker.R
import com.jnj.vaccinetracker.common.data.managers.ConfigurationManager
import com.jnj.vaccinetracker.common.data.managers.VisitManager
import com.jnj.vaccinetracker.common.data.models.Constants
import com.jnj.vaccinetracker.common.data.repositories.UserRepository
import com.jnj.vaccinetracker.common.domain.entities.CreateVisit
import com.jnj.vaccinetracker.common.domain.usecases.CreateVisitUseCase
import com.jnj.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.jnj.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.jnj.vaccinetracker.common.helpers.findParent
import com.jnj.vaccinetracker.common.ui.BaseDialogFragment
import com.jnj.vaccinetracker.common.util.SubstancesDataUtil
import com.jnj.vaccinetracker.databinding.DialogVisitRegisteredSuccessBinding
import com.jnj.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.jnj.vaccinetracker.register.dialogs.ScheduleVisitDatePickerDialog
import com.jnj.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.jnj.vaccinetracker.sync.domain.entities.UpcomingVisit
import com.jnj.vaccinetracker.visit.VisitViewModel
import com.soywiz.klock.DateFormat
import com.soywiz.klock.DateTime
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * @author timonelen
 * @version 1
 */
class VisitRegisteredSuccessDialog : BaseDialogFragment(), ScheduleVisitDatePickerDialog.OnDateSelectedListener {

    companion object {
        private const val ARG_NEXT_VISIT = "next_visit"
        private const val PARTICIPANT = "participant"
        private const val CURRENT_VISIT_UUID = "currentVisitUuid"

        fun create(nextVisit: UpcomingVisit?, participant: ParticipantSummaryUiModel?, currentVisitUuid: String?): VisitRegisteredSuccessDialog {
            return VisitRegisteredSuccessDialog().apply { arguments = bundleOf(ARG_NEXT_VISIT to nextVisit, PARTICIPANT to participant, CURRENT_VISIT_UUID to currentVisitUuid) }
        }
    }

    private val viewModel: VisitViewModel by activityViewModels { viewModelFactory }
    private lateinit var binding: DialogVisitRegisteredSuccessBinding
    private lateinit var visitDateTextView: TextView
    private lateinit var visitScheduleResultTextView: TextView
    private lateinit var nextVisitDateContainerLinearLayout: LinearLayout
    private lateinit var proposedDateContainerLinearLayout: LinearLayout
    private lateinit var saveVisitButton: Button
    private lateinit var closeButton: Button
    private lateinit var proposedDateTextVisit: TextView
    private val nextVisit: UpcomingVisit? by lazy { requireArguments().getParcelable(ARG_NEXT_VISIT) }
    private var visitDate: DateTime? = null
    private var canFinish: Boolean = false
    private val participant: ParticipantSummaryUiModel? by lazy { requireArguments().getParcelable(PARTICIPANT) }
    private val currentVisitUuid: String? by lazy { requireArguments().getString(CURRENT_VISIT_UUID) }
    @Inject lateinit var createVisitUseCase: CreateVisitUseCase
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
    @Inject lateinit var configurationManager: ConfigurationManager
    @Inject lateinit var visitManager: VisitManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
        isCancelable = false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_visit_registered_success, container, false)
        binding.nextVisit = nextVisit
        binding.executePendingBindings()
        visitDateTextView = binding.root.findViewById(R.id.next_visit_date_value)
        visitScheduleResultTextView = binding.root.findViewById(R.id.visit_schedule_result_label)
        nextVisitDateContainerLinearLayout = binding.root.findViewById(R.id.next_visit_date_container)
        proposedDateContainerLinearLayout = binding.root.findViewById(R.id.proposed_next_visit_date_container)
        saveVisitButton = binding.root.findViewById(R.id.btn_save_visit)
        closeButton = binding.root.findViewById(R.id.btn_finish)
        proposedDateTextVisit = binding.root.findViewById(R.id.proposed_next_visit_date_value)

        lifecycleScope.launch {
            val nextVisitProposedDateAsLocalDate = findProposedNextVisitDateAsLocalDate()
            proposedDateTextVisit.text = nextVisitProposedDateAsLocalDate?.toString() ?: ""

            val nextVisitProposedDateAsDate = Date.from(nextVisitProposedDateAsLocalDate
                ?.atStartOfDay(ZoneId.of(Constants.UTC_TIME_ZONE_NAME))?.toInstant())
            val proposedDateAsDate = nextVisitProposedDateAsDate?.let { DateTime.fromUnix(it.time) }
            binding.nextVisitDatePickerButton.setOnClickListener {
                ScheduleVisitDatePickerDialog(proposedDateAsDate, this@VisitRegisteredSuccessDialog).show(childFragmentManager, "scheduleVisitDatePickerDialog")
            }

            if (proposedDateAsDate != null) {
                onDateSelected(proposedDateAsDate)
            }
        }

        binding.btnSaveVisit.setOnClickListener {
           lifecycleScope.launch {
               try {
                   validateDate()
                   if (visitDate != null) {
                       createVisitUseCase.createVisit(buildNextVisitObject(participant!!, Date(visitDate!!.unixMillisLong)))
                       val visitDateAsText = visitDate!!.format(DateFormat.FORMAT_DATE)
                       visitScheduleResultTextView.text = "${getString(R.string.visit_schedule_visit_saved_successfully_label)} $visitDateAsText"
                       visitScheduleResultTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.successDark))
                       nextVisitDateContainerLinearLayout.visibility = View.GONE
                       proposedDateContainerLinearLayout.visibility = View.GONE
                       saveVisitButton.visibility = View.GONE

                       val layoutParams = closeButton.layoutParams as ConstraintLayout.LayoutParams
                       layoutParams.horizontalBias = 0.5f
                       closeButton.layoutParams = layoutParams
                       canFinish = true
                   }
               } catch (ex: Exception) {
                   visitScheduleResultTextView.text = getString(R.string.visit_schedule_visit_failed)
                   visitScheduleResultTextView.setTextColor(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_error))
               }
           }
        }

        binding.btnFinish.setOnClickListener {
            if (canFinish) {
                dismissAllowingStateLoss()
            } else {
                dismiss()
            }
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
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun buildNextVisitObject(participant: ParticipantSummaryUiModel, visitDate: Date): CreateVisit {
        val operatorUuid = userRepository.getUser()?.uuid
            ?: throw OperatorUuidNotAvailableException("Operator UUID not available")
        val locationUuid = syncSettingsRepository.getSiteUuid()
            ?: throw NoSiteUuidAvailableException("Location not available")
        val visitType = findVisitTypeOfNextVisit()
        return CreateVisit(
            participantUuid = participant.participantUuid,
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = visitDate,
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUuid,
                Constants.ATTRIBUTE_VISIT_TYPE_VXNAID to visitType,
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun findVisitTypeOfNextVisit(): String {
        val participantBirthDate = participant!!.birthDateText
        val participantVisits = visitManager.getVisitsForParticipant(participant!!.participantUuid)
        val currentVisitType = SubstancesDataUtil.getVisitTypeForCurrentVisit(participantBirthDate, participantVisits, configurationManager)
        val substancesConfig = configurationManager.getSubstancesConfig()
        val currentVaccine = substancesConfig.find { it.visitType == currentVisitType }
        if (currentVaccine == null) {
            return ""
        }
        val nextVaccine = substancesConfig.filter { it.weeksAfterBirth > currentVaccine.weeksAfterBirth }
            .minByOrNull { it.weeksAfterBirth }
        if (nextVaccine == null) {
            return ""
        }

        return nextVaccine.visitType
    }

    private fun validateDate() {
        if (visitDate == null) {
            val dateValidationText = getString(R.string.dialog_missing_substances_empty_date_validation_message)
            binding.nextVisitDateValue.error = dateValidationText
            val hintTextColor = ContextCompat.getColor(requireContext(), R.color.errorLight)
            binding.nextVisitDateValue.setHintTextColor(hintTextColor)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun findProposedNextVisitDateAsLocalDate(): LocalDate? {
        val currentVisitType = viewModel.selectedVisitType.value
        val substancesConfig = configurationManager.getSubstancesConfig()

        val currentVaccine = substancesConfig.find { it.visitType == currentVisitType }
        if (currentVaccine == null) {
            return null
        }

        val nextVaccine = substancesConfig
            .filter { it.weeksAfterBirth > currentVaccine.weeksAfterBirth }
            .minByOrNull { it.weeksAfterBirth }
        if (nextVaccine == null) {
            return null
        }

        val participantBirthDate = participant!!.birthDateText
        return if (currentVisitType == Constants.AT_BIRTH_VISIT_TYPE) {
            val birthDateLocalDate = LocalDate.parse(participantBirthDate)
            birthDateLocalDate.plusWeeks(nextVaccine.weeksAfterBirth.toLong())
        } else {
            val weeksDifference = nextVaccine.weeksAfterBirth - currentVaccine.weeksAfterBirth
            LocalDate.now().plusWeeks(weeksDifference.toLong())
        }
    }

    interface VisitRegisteredSuccessDialogListener {
        fun onVisitRegisteredSuccessDialogClosed()
    }
}