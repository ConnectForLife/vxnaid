package com.idi.vaccinetracker.common.data.database.models.draft.update

import com.idi.vaccinetracker.common.data.database.entities.base.UploadableDraft
import com.idi.vaccinetracker.common.data.database.entities.base.VisitBase
import com.idi.vaccinetracker.common.domain.entities.DraftState

data class RoomUpdateVisitDraftStateModel(override val visitUuid: String, override val draftState: DraftState) : UploadableDraft, VisitBase