package com.idi.vaccinetracker.common.data.database.daos.base

import com.idi.vaccinetracker.common.data.database.models.delete.RoomDeleteParticipantModel
import com.idi.vaccinetracker.common.domain.entities.DraftState

interface DraftParticipantDataFileDaoBase<E> {
    suspend fun findAllByDraftState(draftState: DraftState, offset: Int, limit: Int): List<@JvmSuppressWildcards E>
    suspend fun delete(deleteParticipantModel: RoomDeleteParticipantModel): Int
}