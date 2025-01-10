package com.idi.vaccinetracker.sync.domain.usecases.store

import com.idi.vaccinetracker.common.data.database.repositories.DeletedSyncRecordRepository
import com.idi.vaccinetracker.common.data.database.repositories.DraftParticipantRepository
import com.idi.vaccinetracker.common.data.database.repositories.ParticipantRepository
import com.idi.vaccinetracker.common.data.database.transaction.ParticipantDbTransactionRunner
import com.idi.vaccinetracker.common.data.models.toDomain
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.domain.entities.Participant
import com.idi.vaccinetracker.common.domain.entities.toParticipantWithoutAssets
import com.idi.vaccinetracker.common.helpers.logDebug
import com.idi.vaccinetracker.common.helpers.logError
import com.idi.vaccinetracker.common.helpers.logInfo
import com.idi.vaccinetracker.common.helpers.logWarn
import com.idi.vaccinetracker.sync.data.models.ParticipantSyncRecord
import com.idi.vaccinetracker.sync.data.models.ParticipantSyncRecord.Delete.Companion.toDomain
import com.idi.vaccinetracker.sync.data.models.toMap
import com.idi.vaccinetracker.sync.domain.entities.ParticipantPendingCall
import com.idi.vaccinetracker.sync.domain.entities.SyncErrorMetadata
import com.idi.vaccinetracker.sync.domain.helpers.SyncLogger
import com.idi.vaccinetracker.sync.domain.usecases.store.base.StoreSyncRecordUseCaseBase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreParticipantSyncRecordUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val draftParticipantRepository: DraftParticipantRepository,
    private val transactionRunner: ParticipantDbTransactionRunner,
    private val syncLogger: SyncLogger,
    private val deletedSyncRecordRepository: DeletedSyncRecordRepository,
) : StoreSyncRecordUseCaseBase<ParticipantSyncRecord, Participant> {


    override suspend fun store(syncRecord: ParticipantSyncRecord) = transactionRunner.withTransaction {
        when (syncRecord) {
            is ParticipantSyncRecord.Delete -> delete(syncRecord)
            is ParticipantSyncRecord.Update -> update(syncRecord)
        }.let {}
    }

    private fun ParticipantSyncRecord.Update.toDomain() = Participant(
        participantUuid = participantUuid,
        dateModified = dateModified.date,
        image = null,
        biometricsTemplate = null,
        participantId = participantId,
        nin = nin,
        childNumber = childNumber,
        gender = gender,
        isBirthDateEstimated = isBirthDateEstimated,
        birthDate = birthDate.toDomain(),
        attributes = attributes.toMap(),
        address = address,
        childFirstName = childFirstName,
        childLastName = childLastName
    )

    private suspend fun deleteUploadedDraftParticipant(participant: Participant) {
        val participantUuid = participant.participantUuid
        val draftState = draftParticipantRepository.findDraftStateByParticipantUuid(participantUuid)
        suspend fun delete() {
            val isRecordDeleted = draftParticipantRepository.deleteByParticipantUuid(participantUuid)
            logInfo("delete draft participant [draftState:$draftState, isRecordDeleted:$isRecordDeleted]")
        }
        when (draftState) {
            DraftState.UPLOAD_PENDING -> {
                val draftParticipant = draftParticipantRepository.findByParticipantUuid(participantUuid)
                if (draftParticipant != null) {
                    val p = draftParticipant.toParticipantWithoutAssets()
                        .copy(dateModified = participant.dateModified)
                    if (p == participant) {
                        //this participant is already uploaded even though it's an pending upload draft. Somehow we failed to update to draft state to uploaded
                        //so delete this
                        delete()
                        val syncError = SyncErrorMetadata.UploadParticipantPendingCall(
                            ParticipantPendingCall.Type.REGISTER_PARTICIPANT,
                            participantUuid = p.participantUuid,
                            visitUuid = null,
                            locationUuid = p.locationUuid,
                            participantId = p.participantId
                        )
                        syncLogger.clearSyncError(syncError)
                    } else {
                        logWarn("nothing deleted for participant because draft doesn't match $participantUuid [$draftState]")
                    }
                } else {
                    logError("draftParticipant is null $participant")
                }
            }
            DraftState.UPLOADED -> delete()
            null -> logDebug("nothing deleted for participant $participantUuid [$draftState]")
        }.let {}
    }

    private suspend fun onInsertSuccess(participant: Participant) {
        val participantUuid = participant.participantUuid
        logDebug("onInsertSuccess: $participantUuid")
        deleteUploadedDraftParticipant(participant)
    }

    private suspend fun update(syncRecord: ParticipantSyncRecord.Update) {
        val participant = syncRecord.toDomain()
        participantRepository.insert(participant, orReplace = true)
        onInsertSuccess(participant)
    }

    private suspend fun delete(syncRecord: ParticipantSyncRecord.Delete) {
        participantRepository.deleteByParticipantUuid(syncRecord.participantUuid)
        draftParticipantRepository.deleteByParticipantUuid(syncRecord.participantUuid)
        deletedSyncRecordRepository.insert(syncRecord.toDomain(), orReplace = true)
    }
}