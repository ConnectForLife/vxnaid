package com.jnj.vaccinetracker.common.data.database.daos.base

import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteVisitModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantDataToUploadModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState

interface VisitDaoCommon<E, M> : DaoBase<E> {
    suspend fun findByVisitUuid(visitUuid: String): M?
    suspend fun findAllByParticipantUuid(participantUuid: String): List<@JvmSuppressWildcards M>
    suspend fun findAllVisits(): List<@JvmSuppressWildcards M>
    suspend fun findAllVisitsByAttributeTypeAndValue(type: String, value: String): List<@JvmSuppressWildcards M>
    suspend fun findVisitsAfterDate(date: DateEntity): List<@JvmSuppressWildcards M>
    suspend fun findVisitsBeforeDate(date: DateEntity): List<@JvmSuppressWildcards M>
    suspend fun findVisitsBeforeDate(date: DateEntity, visitStatus: String): List<@JvmSuppressWildcards M>
    suspend fun delete(deleteVisitModel: RoomDeleteVisitModel): Int
}

suspend fun VisitDaoCommon<*, *>.deleteByVisitUuid(visitUuid: String): Int = delete(RoomDeleteVisitModel(visitUuid))

interface VisitDaoBase<E, M> : VisitDaoCommon<E, M> {

}

interface DraftVisitDaoBase<E, M> : VisitDaoCommon<E, M> {
    suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Int
    suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<@JvmSuppressWildcards M>
    suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<RoomDraftParticipantDataToUploadModel>
    suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState?
}