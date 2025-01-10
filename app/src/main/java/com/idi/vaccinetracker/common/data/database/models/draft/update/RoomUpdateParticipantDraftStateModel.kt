package com.idi.vaccinetracker.common.data.database.models.draft.update

import com.idi.vaccinetracker.common.data.database.entities.base.ParticipantUuidContainer
import com.idi.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.idi.vaccinetracker.common.domain.entities.DraftState

data class RoomUpdateParticipantDraftStateModel(override val participantUuid: String, override val draftState: DraftState) : UploadableDraft, ParticipantUuidContainer