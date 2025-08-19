package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.daos.base.DaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftVisitDaoBase
import com.jnj.vaccinetracker.common.data.database.daos.base.DraftVisitSyncDao
import com.jnj.vaccinetracker.common.data.database.daos.base.ObservableDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitAttributeEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftVisitEntity
import com.jnj.vaccinetracker.common.data.database.models.delete.RoomDeleteVisitModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftParticipantDataToUploadModel
import com.jnj.vaccinetracker.common.data.database.models.draft.RoomDraftVisitModel
import com.jnj.vaccinetracker.common.data.database.models.draft.update.RoomUpdateVisitDraftStateModel
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftVisitDao : DraftVisitDaoBase<DraftVisitEntity, RoomDraftVisitModel>, ObservableDao, DraftVisitSyncDao {


    @Query("DELETE FROM draft_visit where visitUuid=:visitUuid and draftState=:draftState")
    override suspend fun deleteByVisitUuid(visitUuid: String, draftState: DraftState): Int

    @Query("select * from draft_visit where participantUuid=:participantUuid AND draftState=:draftState")
    @Transaction
    override suspend fun findAllByParticipantUuidAndDraftState(participantUuid: String, draftState: DraftState): List<RoomDraftVisitModel>

    @Query("select * from draft_visit where participantUuid=:participantUuid")
    @Transaction
    override suspend fun findAllByParticipantUuid(participantUuid: String): List<RoomDraftVisitModel>

    @Query("select * from draft_visit where visitUuid=:visitUuid")
    @Transaction
    override suspend fun findByVisitUuid(visitUuid: String): RoomDraftVisitModel?

    @Query("select * from draft_visit")
    @Transaction
    override suspend fun findAllVisits(): List<RoomDraftVisitModel>

    @Query("select * from draft_visit where visitUuid in (select visitUuid from visit_attribute where type = :type and value = :value)")
    @Transaction
    override suspend fun findAllVisitsByAttributeTypeAndValue(type: String, value: String): List<RoomDraftVisitModel>

    @Query("SELECT * FROM draft_visit WHERE startDatetime >= :date ORDER BY startDatetime ASC")
    @Transaction
    override suspend fun findVisitsAfterDate(date: DateEntity): List<RoomDraftVisitModel>

    @Query("SELECT * FROM draft_visit WHERE startDatetime <= :date ORDER BY startDatetime DESC")
    @Transaction
    override suspend fun findVisitsBeforeDate(date: DateEntity): List<RoomDraftVisitModel>

    @Query("SELECT v.* " +
            "FROM draft_visit v " +
            "INNER JOIN draft_visit_attribute va ON v.visitUuid = va.visitUuid AND va.type = 'Visit Status' " +
            "WHERE v.startDatetime <= :date AND va.value = :visitStatus ORDER BY v.startDatetime DESC")
    @Transaction
    override suspend fun findVisitsBeforeDate(
        date: DateEntity,
        visitStatus: String
    ): List<RoomDraftVisitModel>

    @Query("select * from draft_visit where draftState=:draftState LIMIT 1")
    @Transaction
    suspend fun findFirstByDraftState(draftState: DraftState): RoomDraftVisitModel?

    @Update(entity = DraftVisitEntity::class)
    override suspend fun updateDraftState(updateDraftStateModel: RoomUpdateVisitDraftStateModel): Int

    @Query("select count(*) from draft_visit")
    override fun observeChanges(): Flow<Long>

    @Query("select participantUuid from draft_visit where draftState=:draftState LIMIT :offset,:limit")
    override suspend fun findAllParticipantUuidsByDraftState(draftState: DraftState, offset: Int, limit: Int): List<RoomDraftParticipantDataToUploadModel>

    @Query("delete from draft_visit where draftState=:draftState")
    override suspend fun deleteAllByDraftState(draftState: DraftState): Int

    @Query("select draftState from draft_visit where visitUuid=:visitUuid")
    override suspend fun findDraftStateByVisitUuid(visitUuid: String): DraftState?

    @Query("select count(*) from draft_visit where draftState=:draftState")
    suspend fun countByDraftState(draftState: DraftState): Long

    @Delete(entity = DraftVisitEntity::class)
    override suspend fun delete(deleteVisitModel: RoomDeleteVisitModel): Int
}

@Dao
interface DraftVisitAttributeDao : DaoBase<DraftVisitAttributeEntity>, ObservableDao {

    @Query("select count(*) from draft_visit_attribute")
    override fun observeChanges(): Flow<Long>
}