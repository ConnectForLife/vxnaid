package com.idi.vaccinetracker.sync.domain.usecases

import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftVisitEncounterRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftVisitRepository
import com.idi.vaccinetracker.common.domain.entities.DraftParticipant
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.domain.entities.DraftVisit
import com.idi.vaccinetracker.common.domain.entities.DraftVisitEncounter
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import com.idi.vaccinetracker.sync.domain.entities.ParticipantPendingCall
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import kotlinx.coroutines.yield
import javax.inject.Inject

class FindAllRelatedDraftParticipantDataPendingUploadUseCase @Inject constructor(
    private val draftParticipantRepository: DraftParticipantRepository,
    private val draftVisitEncounterRepository: DraftVisitEncounterRepository,
    private val draftVisitRepository: DraftVisitRepository,
    private val syncLogger: SyncLogger,
) {

    private fun DraftParticipant.toPendingCall(): ParticipantPendingCall {
        return if (isUpdate) {
            ParticipantPendingCall.UpdateParticipant(this)
        } else {
            ParticipantPendingCall.RegisterParticipant(this)
        }
    }
    private fun DraftVisit.toPendingCall() = ParticipantPendingCall.CreateVisit(this)
    private fun DraftVisitEncounter.toPendingCall() = ParticipantPendingCall.UpdateVisit(this)

    /**
     * group together all draft data related to the specified [participantUuid] with draftState [DraftState.UPLOAD_PENDING]
     */
    suspend fun findAllRelatedDraftDataPendingUpload(participantUuid: String, logSyncErrors: Boolean): List<ParticipantPendingCall> {
        val syncErrorMetadata = SyncErrorMetadata.FindAllRelatedDraftDataPendingUpload(participantUuid = participantUuid)
        try {
            val draftState = DraftState.UPLOAD_PENDING
            val draftParticipant = draftParticipantRepository.findByParticipantUuidAndDraftState(participantUuid, draftState)?.toPendingCall()
            val draftVisits = draftVisitRepository.findAllByParticipantUuidAndDraftState(participantUuid, draftState).map { it.toPendingCall() }
            val draftEncounters = draftVisitEncounterRepository.findAllByParticipantUuidAndDraftState(participantUuid, draftState).map { it.toPendingCall() }
            return listOfNotNull(
                draftParticipant,
                *draftVisits.toTypedArray(),
                *draftEncounters.toTypedArray()).sorted().also {
                syncLogger.clearSyncError(syncErrorMetadata)
            }

        } catch (ex: Exception) {
            yield()
            ex.rethrowIfFatal()
            if (logSyncErrors)
                syncLogger.logSyncError(syncErrorMetadata, ex)
            throw ex
        }
    }
}