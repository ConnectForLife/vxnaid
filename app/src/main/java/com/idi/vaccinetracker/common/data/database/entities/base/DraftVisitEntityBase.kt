package com.idi.vaccinetracker.common.data.database.entities.base

interface DraftVisitEntityBase : VisitEntityBase, UploadableDraft {
    val locationUuid: String
}