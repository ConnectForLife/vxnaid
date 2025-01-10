package com.idi.vaccinetracker.common.data.database.models.draft

import com.idi.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer

data class RoomDraftParticipantDataToUploadModel(override val participantUuid: String) : ParticipantUuidContainer