package com.idi.vaccinetracker.common.data.database.models

import androidx.room.Relation
import com.idi.vaccinetracker.common.data.database.entities.VisitAttributeEntity
import com.idi.vaccinetracker.common.data.database.entities.VisitEntity
import com.idi.vaccinetracker.common.data.database.entities.VisitObservationEntity
import com.idi.vaccinetracker.common.data.database.entities.base.VisitEntityBase
import com.idi.vaccinetracker.common.data.database.entities.base.VisitSyncBase
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

data class RoomVisitModel(
    override val startDatetime: DateEntity,
    override val visitUuid: String,
    @Relation(parentColumn = VisitEntity.ID, entityColumn = VisitEntity.ID)
    val attributes: List<VisitAttributeEntity>,
    @Relation(parentColumn = VisitEntity.ID, entityColumn = VisitEntity.ID)
    val observations: List<VisitObservationEntity>,
    override val dateModified: DateEntity,
    override val visitType: String,
    override val participantUuid: String,
) : VisitEntityBase, VisitSyncBase