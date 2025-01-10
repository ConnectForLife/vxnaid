package com.idi.vaccinetracker.common.data.database.models

import com.idi.vaccinetracker.common.data.database.entities.base.SyncBase
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity

data class RoomDeletedParticipantModel(
    val uuid: String,
    override val dateModified: DateEntity,
    val participantId : String,
) : SyncBase


