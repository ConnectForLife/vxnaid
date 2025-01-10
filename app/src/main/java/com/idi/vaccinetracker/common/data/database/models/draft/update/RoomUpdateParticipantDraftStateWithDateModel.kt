package com.idi.vaccinetracker.common.data.database.models.draft.update

import com.idi.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.idi.vaccinetracker.common.data.database.entities.base.UploadableDraftWithDate
import com.idi.vaccinetracker.common.data.database.typealiases.DateEntity
import com.idi.vaccinetracker.common.domain.entities.DraftState

data class RoomUpdateParticipantDraftStateWithDateModel(
    override val participantUuid: String,
    override val draftState: DraftState,
    override val dateLastUploadAttempt: DateEntity?,
) : UploadableDraftWithDate, ParticipantUuidContainer