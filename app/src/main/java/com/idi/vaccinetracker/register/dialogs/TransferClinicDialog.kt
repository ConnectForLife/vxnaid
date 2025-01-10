package com.idi.vaccinetracker.register.dialogs

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.idi.vaccinetracker.R
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.data.database.typealiases.dateNow
import com.idi.vaccinetracker.common.data.managers.ParticipantManager
import com.idi.vaccinetracker.common.data.models.Constants
import com.idi.vaccinetracker.common.data.repositories.UserRepository
import com.idi.vaccinetracker.common.domain.entities.ScheduleFirstVisit
import com.idi.vaccinetracker.common.domain.entities.UpdateParticipant
import com.idi.vaccinetracker.common.domain.usecases.FindParticipantByParticipantUuidUseCase
import com.idi.vaccinetracker.common.exceptions.NoSiteUuidAvailableException
import com.idi.vaccinetracker.common.exceptions.OperatorUuidNotAvailableException
import com.idi.vaccinetracker.common.ui.BaseActivity
import com.idi.vaccinetracker.common.ui.BaseDialogFragment
import com.idi.vaccinetracker.databinding.DialogTransferClinicBinding
import com.idi.vaccinetracker.participantflow.model.ParticipantSummaryUiModel
import com.idi.vaccinetracker.participantflow.model.ParticipantUiModel
import com.idi.vaccinetracker.register.ParticipantUpdateViewModel
import com.idi.vaccinetracker.sync.data.network.VaccineTrackerSyncApiDataSource
import com.idi.vaccinetracker.sync.data.repositories.SyncSettingsRepository
import com.idi.vaccinetracker.visit.VisitActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class TransferClinicDialog(
    private var participant: ParticipantUiModel,
    private var currentLocationUuid: String
) : BaseDialogFragment() {

    private lateinit var binding: DialogTransferClinicBinding
    @Inject lateinit var vaccineTrackerSyncApiDataSource: VaccineTrackerSyncApiDataSource
    @Inject lateinit var syncSettingsRepository: SyncSettingsRepository
    @Inject lateinit var participantRepository: ParticipantRepository
    @Inject lateinit var findParticipantByParticipantUuidUseCase: FindParticipantByParticipantUuidUseCase
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var participantManager: ParticipantManager

    private lateinit var participantViewModel: ParticipantUpdateViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_transfer_clinic, container, false)

        participantViewModel = ParticipantUpdateViewModel(participantManager)

        val participantSummaryUiModel = ParticipantSummaryUiModel(
            participant.participantUUID!!,
            participant.participantId!!,
            participant.gender!!,
            participant.birthDateText!!,
            participant.isBirthDateEstimated!!,
            participant.vaccine,
            null
        )

        lifecycleScope.launch {
            try {
                val fetchedParticipant = findParticipantByParticipantUuidUseCase.findByParticipantUuid(participant.participantUUID!!)
                val updatedAttributes = fetchedParticipant?.attributes?.toMutableMap() ?: mutableMapOf()
                updatedAttributes[Constants.ATTRIBUTE_LOCATION] = currentLocationUuid

                val participantToUpdate = UpdateParticipant(
                    fetchedParticipant!!.participantUuid,
                    fetchedParticipant.participantId,
                    fetchedParticipant.nin,
                    fetchedParticipant.childNumber,
                    fetchedParticipant.gender,
                    fetchedParticipant.isBirthDateEstimated ?: false,
                    fetchedParticipant.birthDate,
                    fetchedParticipant.address!!,
                    updatedAttributes,
                    null,
                    createScheduleFirstVisit(),
                    fetchedParticipant.childFirstName,
                    fetchedParticipant.childLastName
                )

                binding.btnTransferAndContinueVisit.setOnClickListener {
                    try {
                        startParticipantVisitContraindications(participantSummaryUiModel)
                        participantViewModel.updateParticipantInBackground(participantToUpdate)
                    } catch (e: Exception) {
                        Log.e("TransferClinicDialog", "Something went wrong during updating participant location", e)
                    }

                    dismissAllowingStateLoss()
                }

                binding.btnJustVisit.setOnClickListener {
                    startParticipantVisitContraindications(participantSummaryUiModel)
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                Log.e("TransferClinicDialog", "Error fetching participant", e)
            }
        }

        return binding.root
    }

    private fun startParticipantVisitContraindications(participant: ParticipantSummaryUiModel) {
        startActivity(VisitActivity.create(requireContext(), participant, newRegisteredParticipant = false))
        (requireActivity() as BaseActivity).setForwardAnimation()
    }

    private fun createScheduleFirstVisit(): ScheduleFirstVisit {
        val locationUuid = syncSettingsRepository.getSiteUuid() ?: throw NoSiteUuidAvailableException("Trying to register scheduled visit without a selected site")
        val operatorUUid = userRepository.getUser()?.uuid ?: throw OperatorUuidNotAvailableException("trying to register scheduled visit without stored operator uuid")
        return ScheduleFirstVisit(
            visitType = Constants.VISIT_TYPE_DOSING,
            startDatetime = dateNow(),
            locationUuid = locationUuid,
            attributes = mapOf(
                Constants.ATTRIBUTE_VISIT_STATUS to Constants.VISIT_STATUS_SCHEDULED,
                Constants.ATTRIBUTE_OPERATOR to operatorUUid,
                Constants.ATTRIBUTE_VISIT_DOSE_NUMBER to "1"
            )
        )
    }
}
