package com.idi.vaccinetracker.common.data.database.entities.base

import com.idi.vaccinetracker.common.domain.entities.DraftState

interface UploadableDraft {
    val draftState: DraftState

    companion object {
        const val COL_DRAFT_STATE = "draftState"
    }
}