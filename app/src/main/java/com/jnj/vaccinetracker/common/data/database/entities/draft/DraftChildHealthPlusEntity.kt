package com.jnj.vaccinetracker.common.data.database.entities.draft

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jnj.vaccinetracker.common.data.database.typealiases.DateEntity
import com.jnj.vaccinetracker.common.data.database.typealiases.dateNow
import com.jnj.vaccinetracker.common.domain.entities.DraftState
import java.util.*

/**
 * Room entity for Child Health+ client records
 */
@Entity(
    tableName = "draft_child_health_plus",
    foreignKeys = [
        ForeignKey(
            entity = DraftParticipantEntity::class,
            parentColumns = ["participantUuid"],
            childColumns = ["participantUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DraftChildHealthPlusEntity(
    @PrimaryKey val uuid: String,
    val participantUuid: String? = null,
    val clientName: String,
    val dateOfBirth: DateEntity,
    val sex: String,
    val contactInfo: String,
    val mothersName: String,
    val isPregnantWoman: Boolean = false,
    val locationUuid: String? = null,
    val operatorUuid: String? = null,
    val dateCreated: DateEntity,
    val dateModified: DateEntity,
    val draftState: String = DraftState.initialState().name
)

/**
 * Room entity for Child Health+ services associated with a client
 */
@Entity(
    tableName = "draft_child_health_plus_service",
    foreignKeys = [
        ForeignKey(
            entity = DraftChildHealthPlusEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["childHealthPlusUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DraftChildHealthPlusServiceEntity(
    @PrimaryKey val uuid: String,
    val childHealthPlusUuid: String,
    val service: String, // ChildHealthPlusService name
    val dose: String? = null, // DoseNumber name
    val administrationDate: DateEntity,
    val nextVisitDate: DateEntity? = null,
    val dateCreated: DateEntity = dateNow()
)
