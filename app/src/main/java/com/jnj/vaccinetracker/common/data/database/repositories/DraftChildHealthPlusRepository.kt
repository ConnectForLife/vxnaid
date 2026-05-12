package com.jnj.vaccinetracker.common.data.database.repositories

import com.jnj.vaccinetracker.common.data.database.daos.draft.DraftChildHealthPlusDao
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftChildHealthPlusEntity
import com.jnj.vaccinetracker.common.data.database.entities.draft.DraftChildHealthPlusServiceEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.data.models.ChildHealthPlusData
import com.jnj.vaccinetracker.common.data.models.DoseNumber
import com.jnj.vaccinetracker.common.data.models.SelectedService
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import com.jnj.vaccinetracker.common.helpers.logInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

class DraftChildHealthPlusRepository @Inject constructor(
    private val dao: DraftChildHealthPlusDao
) {
    
    suspend fun save(data: ChildHealthPlusData): String {
        val entity = DraftChildHealthPlusEntity(
            uuid = data.uuid,
            participantUuid = data.participantUuid,
            clientName = data.clientName,
            dateOfBirth = data.dateOfBirth,
            sex = data.sex,
            contactInfo = data.contactInfo,
            mothersName = data.mothersName,
            isPregnantWoman = data.isPregnantWoman,
            locationUuid = data.locationUuid,
            operatorUuid = data.operatorUuid,
            dateCreated = data.dateCreated,
            dateModified = data.dateModified,
            draftState = DraftState.initialState().name
        )
        logInfo("Saving Child Health+ data: ${data.uuid}")
        dao.insert(entity)
        
        // Save services
        if (data.services.isNotEmpty()) {
            saveServices(data.uuid, data.services)
        }
        
        return data.uuid
    }
    
    suspend fun saveServices(childHealthPlusUuid: String, services: List<SelectedService>) {
        val serviceEntities = services.map { service ->
            DraftChildHealthPlusServiceEntity(
                uuid = service.uuid,
                childHealthPlusUuid = childHealthPlusUuid,
                service = service.service.name,
                dose = service.dose?.name,
                administrationDate = service.administrationDate,
                nextVisitDate = service.nextVisitDate
            )
        }
        dao.insertServices(serviceEntities)
    }
    
    suspend fun findByUuid(uuid: String): ChildHealthPlusData? {
        val entity = dao.findByUuid(uuid) ?: return null
        val services = dao.findServices(uuid).map { it.toDomain() }
        return entity.toDomain(services)
    }
    
    suspend fun findByParticipantUuid(participantUuid: String): ChildHealthPlusData? {
        val entity = dao.findByParticipantUuid(participantUuid) ?: return null
        val services = dao.findServices(entity.uuid).map { it.toDomain() }
        return entity.toDomain(services)
    }
    
    fun observeByParticipantUuid(participantUuid: String): Flow<ChildHealthPlusData?> {
        return dao.observeByParticipantUuid(participantUuid).map { entity ->
            if (entity == null) null
            else {
                val services = dao.findServices(entity.uuid).map { it.toDomain() }
                entity.toDomain(services)
            }
        }
    }
    
    suspend fun update(data: ChildHealthPlusData) {
        val entity = DraftChildHealthPlusEntity(
            uuid = data.uuid,
            participantUuid = data.participantUuid,
            clientName = data.clientName,
            dateOfBirth = data.dateOfBirth,
            sex = data.sex,
            contactInfo = data.contactInfo,
            mothersName = data.mothersName,
            isPregnantWoman = data.isPregnantWoman,
            locationUuid = data.locationUuid,
            operatorUuid = data.operatorUuid,
            dateCreated = data.dateCreated,
            dateModified = dateNow(),
            draftState = DraftState.initialState().name
        )
        logInfo("Updating Child Health+ data: ${data.uuid}")
        dao.update(entity)
        
        // Update services
        if (data.services.isNotEmpty()) {
            saveServices(data.uuid, data.services)
        }
    }
    
    suspend fun delete(uuid: String) {
        logInfo("Deleting Child Health+ data: $uuid")
        dao.deleteByUuid(uuid)
    }
    
    suspend fun deleteService(uuid: String) {
        dao.deleteServiceByUuid(uuid)
    }
    
    suspend fun count(): Int {
        return dao.count()
    }
    
    private fun DraftChildHealthPlusEntity.toDomain(services: List<SelectedService> = emptyList()): ChildHealthPlusData {
        return ChildHealthPlusData(
            uuid = uuid,
            participantUuid = participantUuid,
            clientName = clientName,
            dateOfBirth = dateOfBirth,
            sex = sex,
            contactInfo = contactInfo,
            mothersName = mothersName,
            services = services,
            isPregnantWoman = isPregnantWoman,
            locationUuid = locationUuid,
            operatorUuid = operatorUuid,
            dateCreated = dateCreated,
            dateModified = dateModified
        )
    }
    
    private fun DraftChildHealthPlusServiceEntity.toDomain(): SelectedService {
        return SelectedService(
            service = enumValueOf(service),
            dose = dose?.let { enumValueOf<DoseNumber>(it) },
            administrationDate = administrationDate,
            nextVisitDate = nextVisitDate,
            uuid = uuid
        )
    }
}

