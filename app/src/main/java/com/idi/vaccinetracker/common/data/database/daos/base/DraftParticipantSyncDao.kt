package com.idi.vaccinetracker.common.data.database.daos.base

import com.idi.vaccinetracker.common.data.database.models.draft.update.RoomUpdateParticipantDraftStateModel
import com.idi.vaccinetracker.common.data.database.models.draft.update.RoomUpdateVisitDraftStateModel
import com.idi.vaccinetracker.common.domain.entities.DraftState
import com.idi.vaccinetracker.common.exceptions.UpdateDraftStateException
import com.idi.vaccinetracker.common.helpers.logDebug
import com.idi.vaccinetracker.common.helpers.rethrowIfFatal
import kotlinx.coroutines.yield

interface DraftSyncDaoBase<in T> {
    suspend fun updateDraftState(updateDraftStateModel: T): Int
    suspend fun deleteAllByDraftState(draftState: DraftState): Int
}

interface DraftParticipantSyncDao : DraftSyncDaoBase<RoomUpdateParticipantDraftStateModel>
interface DraftVisitSyncDao : DraftSyncDaoBase<RoomUpdateVisitDraftStateModel>

suspend fun <T> DraftSyncDaoBase<T>.updateDraftStateOrThrow(updateDraftStateModel: T) {
    logDebug("updateDraftStateOrThrow: $updateDraftStateModel")
    fun createError(throwable: Throwable?) = UpdateDraftStateException(
        cause = throwable,
        message = "Failed to update draft state [$updateDraftStateModel]"
    )

    val success = try {
        updateDraftState(updateDraftStateModel) > 0
    } catch (throwable: Throwable) {
        yield()
        throwable.rethrowIfFatal()
        throw createError(throwable)
    }
    if (!success) {
        throw createError(null)
    }
}