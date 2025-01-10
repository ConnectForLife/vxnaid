package com.idi.vaccinetracker.common.data.database.models.draft

import androidx.room.Relation
import com.idi.vaccinetracker.common.data.database.entities.base.DraftVisitEncounterEntityBase
import com.idi.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterAttributeEntity
import com.idi.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterEntity
import com.idi.vaccinetracker.common.data.database.entities.draft.DraftVisitEncounterObservationEntity
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.DraftState

data class RoomDraftVisitEncounterModel(
    override val startDatetime: DateEntity,
    override val locationUuid: String,
    override val visitUuid: String,
    @Relation(parentColumn = DraftVisitEncounterEntity.ID, entityColumn = DraftVisitEncounterEntity.ID)
    val attributes: List<DraftVisitEncounterAttributeEntity>,
    @Relation(parentColumn = DraftVisitEncounterEntity.ID, entityColumn = DraftVisitEncounterEntity.ID)
    val observations: List<DraftVisitEncounterObservationEntity>,
    override val draftState: DraftState = DraftState.initialState(),
    override val participantUuid: String,
) : DraftVisitEncounterEntityBase