package com.jnj.vaccinetracker.common.data.database.daos.draft

import androidx.room.*
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftChildHealthPlusEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftChildHealthPlusServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftChildHealthPlusDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DraftChildHealthPlusEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServices(services: List<DraftChildHealthPlusServiceEntity>)
    
    @Query("SELECT * FROM draft_child_health_plus WHERE uuid = :uuid")
    suspend fun findByUuid(uuid: String): DraftChildHealthPlusEntity?
    
    @Query("SELECT * FROM draft_child_health_plus WHERE participantUuid = :participantUuid")
    suspend fun findByParticipantUuid(participantUuid: String): DraftChildHealthPlusEntity?
    
    @Query("SELECT * FROM draft_child_health_plus WHERE participantUuid = :participantUuid")
    fun observeByParticipantUuid(participantUuid: String): Flow<DraftChildHealthPlusEntity?>
    
    @Query("SELECT * FROM draft_child_health_plus_service WHERE childHealthPlusUuid = :childHealthPlusUuid")
    suspend fun findServices(childHealthPlusUuid: String): List<DraftChildHealthPlusServiceEntity>
    
    @Query("SELECT * FROM draft_child_health_plus_service WHERE childHealthPlusUuid = :childHealthPlusUuid")
    fun observeServices(childHealthPlusUuid: String): Flow<List<DraftChildHealthPlusServiceEntity>>
    
    @Update
    suspend fun update(entity: DraftChildHealthPlusEntity)
    
    @Delete
    suspend fun delete(entity: DraftChildHealthPlusEntity)
    
    @Query("DELETE FROM draft_child_health_plus WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
    
    @Query("DELETE FROM draft_child_health_plus_service WHERE uuid = :uuid")
    suspend fun deleteServiceByUuid(uuid: String)
    
    @Query("SELECT COUNT(*) FROM draft_child_health_plus")
    suspend fun count(): Int
}

